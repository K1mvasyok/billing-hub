package io.github.k1mvasyok.billing.shared.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("BR-01: сумма с одним знаком и с хвостовыми нулями приводится к двум знакам")
    void amountsWithFewerDigitsGetTwoDecimals() {
        assertThat(Money.toCents(new BigDecimal("10.5"))).hasToString("10.50");
        assertThat(Money.toCents(new BigDecimal("1500"))).hasToString("1500.00");
        assertThat(Money.toCents(new BigDecimal("1500.500"))).hasToString("1500.50");
        assertThat(Money.toCents(new BigDecimal("-10.5"))).hasToString("-10.50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.005", "0.001", "-0.001"})
    @DisplayName("BR-01: дробная часть тоньше копейки отклоняется, а не округляется молча")
    void amountsFinerThanCentAreRejected(String value) {
        assertThatThrownBy(() -> Money.toCents(new BigDecimal(value)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
