package io.github.k1mvasyok.billing.contracts.domain;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanPriceCloseTest {

    private static final LocalDate START = LocalDate.of(2025, 1, 1);

    @Test
    @DisplayName("открытая цена закрывается на указанный день, после этого у неё есть дата окончания")
    void openPriceClosesAtGivenDay() {
        PlanPrice price = openPrice();

        price.closeAt(LocalDate.of(2027, 1, 1));

        assertThat(price.getValidTo()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("цену нельзя закрыть днём не позже её начала: период станет пустым")
    void closingNotAfterStartIsRejected() {
        PlanPrice price = openPrice();

        assertThatThrownBy(() -> price.closeAt(START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> price.closeAt(LocalDate.of(2024, 12, 31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("закрытую цену нельзя закрыть повторно")
    void closedPriceCannotBeClosedAgain() {
        PlanPrice price = openPrice();
        price.closeAt(LocalDate.of(2027, 1, 1));

        assertThatThrownBy(() -> price.closeAt(LocalDate.of(2027, 6, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PlanPrice openPrice() {
        return new PlanPrice(new Plan("PLAN", "Тариф"), new ValidityPeriod(START, null), new BigDecimal("1500.00"));
    }
}
