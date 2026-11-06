package io.github.k1mvasyok.billing.shared.period;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Цены плана должны покрывать весь период позиции без разрывов (нужно для BR-03). */
class PriceCoverageTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final LocalDate FEB = LocalDate.of(2026, 2, 1);
    private static final LocalDate MAR = LocalDate.of(2026, 3, 1);
    private static final LocalDate APR = LocalDate.of(2026, 4, 1);

    @Test
    void singlePriceCoveringWholeItemIsEnough() {
        List<ValidityPeriod> prices = List.of(new ValidityPeriod(JAN, APR));

        assertThat(PriceCoverage.covers(prices, new ValidityPeriod(JAN, MAR))).isTrue();
    }

    @Test
    void adjacentPricesWithoutGapCoverItem() {
        // [a, b) и [b, c) стыкуются: пересечения и разрыва нет.
        List<ValidityPeriod> prices = List.of(
                new ValidityPeriod(JAN, FEB),
                new ValidityPeriod(FEB, APR));

        assertThat(PriceCoverage.covers(prices, new ValidityPeriod(JAN, APR))).isTrue();
    }

    @Test
    void gapBetweenPricesIsNotCovered() {
        List<ValidityPeriod> prices = List.of(
                new ValidityPeriod(JAN, FEB),
                new ValidityPeriod(MAR, APR));

        assertThat(PriceCoverage.covers(prices, new ValidityPeriod(JAN, APR))).isFalse();
    }

    @Test
    void itemEndingInsideLastPriceIsCovered() {
        List<ValidityPeriod> prices = List.of(new ValidityPeriod(JAN, APR));

        assertThat(PriceCoverage.covers(prices, new ValidityPeriod(FEB, MAR))).isTrue();
    }

    @Test
    void itemStartingBeforeFirstPriceIsNotCovered() {
        List<ValidityPeriod> prices = List.of(new ValidityPeriod(FEB, APR));

        assertThat(PriceCoverage.covers(prices, new ValidityPeriod(JAN, MAR))).isFalse();
    }

    @Test
    void openEndedItemNeedsOpenEndedPrice() {
        List<ValidityPeriod> finite = List.of(new ValidityPeriod(JAN, APR));
        List<ValidityPeriod> open = List.of(new ValidityPeriod(JAN, null));

        assertThat(PriceCoverage.covers(finite, new ValidityPeriod(JAN, null))).isFalse();
        assertThat(PriceCoverage.covers(open, new ValidityPeriod(JAN, null))).isTrue();
    }

    @Test
    void noPricesMeansNotCovered() {
        assertThat(PriceCoverage.covers(List.of(), new ValidityPeriod(JAN, APR))).isFalse();
    }

    @Test
    void unsortedPricesAreHandled() {
        List<ValidityPeriod> prices = List.of(
                new ValidityPeriod(FEB, APR),
                new ValidityPeriod(JAN, FEB));

        assertThat(PriceCoverage.covers(prices, new ValidityPeriod(JAN, APR))).isTrue();
    }
}
