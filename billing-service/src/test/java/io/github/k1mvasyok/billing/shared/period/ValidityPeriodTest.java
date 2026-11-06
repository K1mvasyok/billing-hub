package io.github.k1mvasyok.billing.shared.period;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidityPeriodTest {

    @Test
    void rejectsEmptyOrReversedPeriod() {
        LocalDate day = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> new ValidityPeriod(day, day))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValidityPeriod(day, day.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openEndedPeriodHasNoEnd() {
        ValidityPeriod period = new ValidityPeriod(LocalDate.of(2026, 1, 1), null);

        assertThat(period.covers(LocalDate.of(2099, 12, 31))).isTrue();
    }

    @Test
    void endDateIsExclusive() {
        ValidityPeriod period = new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        assertThat(period.covers(LocalDate.of(2026, 1, 31))).isTrue();
        assertThat(period.covers(LocalDate.of(2026, 2, 1))).isFalse();
    }

    @Test
    void innerPeriodIsWithinOuter() {
        ValidityPeriod outer = new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        assertThat(new ValidityPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)).isWithin(outer)).isTrue();
        assertThat(new ValidityPeriod(LocalDate.of(2026, 3, 1), null).isWithin(outer)).isFalse();
    }
}
