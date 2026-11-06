package io.github.k1mvasyok.billing.contracts.infra;

import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import io.github.k1mvasyok.billing.contracts.domain.Plan;
import io.github.k1mvasyok.billing.contracts.domain.PlanPrice;
import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяет правило «периоды цен одного плана не пересекаются» на уровне БД.
 * Тест идёт на настоящем PostgreSQL: H2 не умеет exclusion constraint.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PlanPriceConstraintTest {

    @Autowired
    private PlanRepository plans;

    @Autowired
    private PlanPriceRepository prices;

    @Test
    void rejectsOverlappingPeriodsForTheSamePlan() {
        Plan plan = plans.saveAndFlush(new Plan("BASIC", "Базовый"));
        prices.saveAndFlush(price(plan, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "100.00"));

        assertThatThrownBy(() -> prices.saveAndFlush(
                price(plan, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1), "120.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsAdjacentPeriodsBecauseEndIsExclusive() {
        Plan plan = plans.saveAndFlush(new Plan("ADJACENT", "Стык"));
        prices.saveAndFlush(price(plan, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), "100.00"));

        assertThatCode(() -> prices.saveAndFlush(
                price(plan, LocalDate.of(2026, 7, 1), null, "110.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsSamePeriodForDifferentPlans() {
        Plan first = plans.saveAndFlush(new Plan("FIRST", "Первый"));
        Plan second = plans.saveAndFlush(new Plan("SECOND", "Второй"));
        prices.saveAndFlush(price(first, LocalDate.of(2026, 1, 1), null, "100.00"));

        assertThatCode(() -> prices.saveAndFlush(price(second, LocalDate.of(2026, 1, 1), null, "100.00")))
                .doesNotThrowAnyException();
    }

    private static PlanPrice price(Plan plan, LocalDate from, LocalDate to, String amount) {
        return new PlanPrice(plan, new ValidityPeriod(from, to), new BigDecimal(amount));
    }
}
