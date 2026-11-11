package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Спецификация для ProrationCalculator: BR-01, BR-02, BR-03. */
class ProrationCalculatorTest {

    private static final YearMonth NOVEMBER_2026 = YearMonth.of(2026, 11);

    @Test
    @DisplayName("BR-02: полный месяц даёт полную цену")
    void fullMonthGivesFullPrice() {
        List<ProratedLine> lines = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-12-01"), 1, prices(price("2026-01-01", null, "1500.00")));

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.amount()).isEqualByComparingTo("1500.00");
            assertThat(line.days()).isEqualTo(30);
            assertThat(line.daysInMonth()).isEqualTo(30);
        });
    }

    @ParameterizedTest(name = "{0}: знаменатель равен {1} дням месяца")
    @CsvSource({
            "2026-02, 28",
            "2028-02, 29",
            "2026-04, 30",
            "2026-01, 31"
    })
    @DisplayName("BR-02: знаменатель — число дней месяца")
    void denominatorIsDaysInMonth(String month, int daysInMonth) {
        YearMonth ym = YearMonth.parse(month);
        List<ProratedLine> lines = ProrationCalculator.prorate(ym,
                period(ym.atDay(1).toString(), ym.plusMonths(1).atDay(1).toString()),
                1, prices(price("2020-01-01", null, "3000.00")));

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.days()).isEqualTo(daysInMonth);
            assertThat(line.daysInMonth()).isEqualTo(daysInMonth);
            assertThat(line.amount()).isEqualByComparingTo("3000.00");
        });
    }

    @Test
    @DisplayName("BR-02: частичный месяц — цена × количество × дни / дни в месяце")
    void partialMonthIsProrated() {
        // 15 дней из 29 (с 15 февраля по 1 марта 2028): 2900 × 15 / 29 = 1500.00
        YearMonth february2028 = YearMonth.of(2028, 2);
        List<ProratedLine> lines = ProrationCalculator.prorate(february2028,
                period("2028-02-15", "2028-03-01"), 1, prices(price("2020-01-01", null, "2900.00")));

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.days()).isEqualTo(15);
            assertThat(line.amount()).isEqualByComparingTo("1500.00");
        });
    }

    @Test
    @DisplayName("BR-02: количество умножает сумму строки")
    void quantityMultipliesAmount() {
        // 16–30 ноября: 15 дней из 30, количество 2: 1500 × 2 × 15 / 30 = 1500.00
        List<ProratedLine> lines = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-16", "2026-12-01"), 2, prices(price("2026-01-01", null, "1500.00")));

        assertThat(lines).singleElement().satisfies(line ->
                assertThat(line.amount()).isEqualByComparingTo("1500.00"));
    }

    @Test
    @DisplayName("BR-03: смена цены 15-го числа режет месяц на два отрезка")
    void priceChangeSplitsMonth() {
        // 1–14 ноября по 3000 (14 дней): 3000 × 14 / 30 = 1400.00
        // 15–30 ноября по 6000 (16 дней): 6000 × 16 / 30 = 3200.00
        List<ProratedLine> lines = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-12-01"), 1,
                prices(
                        price("2026-01-01", "2026-11-15", "3000.00"),
                        price("2026-11-15", null, "6000.00")));

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).from()).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(lines.get(0).to()).isEqualTo(LocalDate.parse("2026-11-15"));
        assertThat(lines.get(0).amount()).isEqualByComparingTo("1400.00");
        assertThat(lines.get(1).from()).isEqualTo(LocalDate.parse("2026-11-15"));
        assertThat(lines.get(1).to()).isEqualTo(LocalDate.parse("2026-12-01"));
        assertThat(lines.get(1).amount()).isEqualByComparingTo("3200.00");
    }

    @Test
    @DisplayName("BR-03: сумма строк равна сумме отрезков без потери копеек")
    void sumOfLinesMatchesSegments() {
        List<ProratedLine> lines = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-12-01"), 1,
                prices(
                        price("2026-01-01", "2026-11-15", "3000.00"),
                        price("2026-11-15", null, "6000.00")));

        BigDecimal total = lines.stream().map(ProratedLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("4600.00");
    }

    @Test
    @DisplayName("BR-03: позиция с 31-го числа в 31-дневном месяце — один день")
    void lastDayOfLongMonth() {
        // 31 января: 1 день из 31, цена 3100: 3100 × 1 / 31 = 100.00
        List<ProratedLine> lines = ProrationCalculator.prorate(YearMonth.of(2026, 1),
                period("2026-01-31", "2026-02-01"), 1, prices(price("2020-01-01", null, "3100.00")));

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.days()).isEqualTo(1);
            assertThat(line.amount()).isEqualByComparingTo("100.00");
        });
    }

    @Test
    @DisplayName("BR-02: позиция вне месяца не даёт строк")
    void itemOutsideMonthGivesNoLines() {
        List<ProratedLine> lines = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-12-01", "2027-01-01"), 1, prices(price("2020-01-01", null, "1500.00")));

        assertThat(lines).isEmpty();
    }

    @Test
    @DisplayName("BR-01: округление HALF_UP до копеек на строке")
    void roundsLineHalfUp() {
        // 100 × 10 / 30 = 33.333… → 33.33
        // 100 × 20 / 30 = 66.666… → 66.67
        List<ProratedLine> oneThird = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-11-11"), 1, prices(price("2020-01-01", null, "100.00")));
        List<ProratedLine> twoThirds = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-11-21"), 1, prices(price("2020-01-01", null, "100.00")));

        assertThat(oneThird.get(0).amount()).isEqualByComparingTo("33.33");
        assertThat(twoThirds.get(0).amount()).isEqualByComparingTo("66.67");
    }

    @Test
    @DisplayName("BR-01: сумма с тремя знаками после запятой не роняет расчёт")
    void tinyAmountDoesNotThrow() {
        // 0.01 × 1 / 30 = 0.000333… → 0.00, исключения быть не должно
        List<ProratedLine> lines = ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-11-02"), 1, prices(price("2020-01-01", null, "0.01")));

        assertThat(lines.get(0).amount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Защита входа: количество должно быть положительным")
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-12-01"), 0, prices(price("2020-01-01", null, "1500.00"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Нет цены на часть периода внутри месяца — исключение, а не тихий пропуск")
    void gapInPricesInsideMonthIsRejected() {
        // Цена есть только с 1 по 15 ноября, с 15 по 30 ноября покрытия нет
        assertThatThrownBy(() -> ProrationCalculator.prorate(NOVEMBER_2026,
                period("2026-11-01", "2026-12-01"), 1, prices(price("2026-11-01", "2026-11-15", "3000.00"))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ValidityPeriod period(String from, String to) {
        return new ValidityPeriod(LocalDate.parse(from), to == null ? null : LocalDate.parse(to));
    }

    private static PriceSegment price(String from, String to, String amount) {
        return new PriceSegment(period(from, to), new BigDecimal(amount));
    }

    private static List<PriceSegment> prices(PriceSegment... segments) {
        return List.of(segments);
    }
}
