package io.github.k1mvasyok.billing.contracts.domain;

import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractTerminationTest {

    private static final ValidityPeriod YEAR_2031 =
            new ValidityPeriod(LocalDate.of(2031, 1, 1), LocalDate.of(2031, 12, 31));
    private static final LocalDate MARCH_16 = LocalDate.of(2031, 3, 16);

    private final Plan plan = new Plan("BASIC", "Базовый");

    @Test
    @DisplayName("расторжение 16 марта обрезает договор и позицию: последний день действия — 15 марта")
    void terminationCutsContractAndItemAtEffectiveDate() {
        Contract contract = activeContract(YEAR_2031);

        contract.terminate(MARCH_16);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(contract.getValidTo()).isEqualTo(MARCH_16);
        assertThat(contract.getItems()).hasSize(1);
        assertThat(contract.getItems().get(0).getValidTo()).isEqualTo(MARCH_16);
    }

    @Test
    @DisplayName("позиция, которая начинается в день расторжения или позже, удаляется из договора")
    void itemStartingOnOrAfterEffectiveDateIsRemoved() {
        Contract contract = new Contract(UUID.randomUUID(), "C-REMOVE", YEAR_2031.from(), YEAR_2031, 14);
        contract.addItem(plan, 1, YEAR_2031);
        contract.addItem(plan, 1, new ValidityPeriod(MARCH_16, LocalDate.of(2031, 12, 31)));
        contract.addItem(plan, 1, new ValidityPeriod(LocalDate.of(2031, 4, 1), LocalDate.of(2031, 12, 31)));
        contract.activate(YEAR_2031.from());

        contract.terminate(MARCH_16);

        assertThat(contract.getItems()).hasSize(1);
        assertThat(contract.getItems().get(0).getValidFrom()).isEqualTo(YEAR_2031.from());
    }

    @Test
    @DisplayName("договор и позиция без даты окончания получают дату расторжения")
    void openEndedContractAndItemGetEffectiveDate() {
        ValidityPeriod openEnded = new ValidityPeriod(LocalDate.of(2031, 1, 1), null);
        Contract contract = new Contract(UUID.randomUUID(), "C-OPEN", openEnded.from(), openEnded, 14);
        contract.addItem(plan, 1, openEnded);
        contract.activate(openEnded.from());

        contract.terminate(MARCH_16);

        assertThat(contract.getValidTo()).isEqualTo(MARCH_16);
        assertThat(contract.getItems().get(0).getValidTo()).isEqualTo(MARCH_16);
    }

    @Test
    @DisplayName("дата расторжения в день начала договора отклоняется")
    void effectiveDateOnContractStartIsRejected() {
        Contract contract = activeContract(YEAR_2031);

        assertThatThrownBy(() -> contract.terminate(YEAR_2031.from()))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "contract-termination-outside-term");
    }

    @Test
    @DisplayName("дата расторжения после окончания договора отклоняется, а в день окончания допустима")
    void effectiveDateAfterContractEndIsRejectedAndEndDateAccepted() {
        Contract late = activeContract(YEAR_2031);
        assertThatThrownBy(() -> late.terminate(LocalDate.of(2032, 1, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "contract-termination-outside-term");

        Contract onEnd = activeContract(YEAR_2031);
        assertThatCode(() -> onEnd.terminate(YEAR_2031.to())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("дата активации записывается при активации, у черновика её нет")
    void activationDateIsRecordedOnlyForActivatedContract() {
        Contract draft = new Contract(UUID.randomUUID(), "C-DRAFT", YEAR_2031.from(), YEAR_2031, 14);
        assertThat(draft.getActivatedOn()).isNull();

        draft.addItem(plan, 1, YEAR_2031);
        draft.activate(LocalDate.of(2031, 1, 5));

        assertThat(draft.getActivatedOn()).isEqualTo(LocalDate.of(2031, 1, 5));
    }

    @Test
    @DisplayName("черновик с позицией расторгается без активации: дата активации остаётся пустой")
    void draftWithItemCanBeTerminatedWithoutActivation() {
        Contract draft = new Contract(UUID.randomUUID(), "C-CANCEL", YEAR_2031.from(), YEAR_2031, 14);
        draft.addItem(plan, 1, YEAR_2031);

        draft.terminate(MARCH_16);

        assertThat(draft.getStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(draft.getActivatedOn()).isNull();
        assertThat(draft.getItems().get(0).getValidTo()).isEqualTo(MARCH_16);
    }

    private Contract activeContract(ValidityPeriod period) {
        Contract contract = new Contract(UUID.randomUUID(), "C-" + period.from(), period.from(), period, 14);
        contract.addItem(plan, 2, period);
        contract.activate(period.from());
        return contract;
    }
}
