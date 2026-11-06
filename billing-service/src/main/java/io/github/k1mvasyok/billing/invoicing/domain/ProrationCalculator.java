package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.period.PriceCoverage;
import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Пропорциональный расчёт позиции за месяц (BR-02, BR-03).
 * Отрезки получаются пересечением позиции, окна месяца и каждого ценового периода.
 * Сумма строки считается одним делением, округление HALF_UP до копеек на строке (BR-01).
 */
public final class ProrationCalculator {

    private ProrationCalculator() {
    }

    /**
     * @param month      расчётный месяц
     * @param itemPeriod период позиции договора
     * @param quantity   количество позиции, должно быть больше нуля
     * @param prices     цены плана; периоды не пересекаются
     * @return строки счёта по отрезкам с постоянной ценой, по возрастанию дат; пусто, если позиция вне месяца
     * @throws IllegalStateException если часть периода позиции внутри месяца не покрыта ценой
     */
    public static List<ProratedLine> prorate(YearMonth month, ValidityPeriod itemPeriod,
                                             int quantity, List<PriceSegment> prices) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше нуля");
        }
        ValidityPeriod window = new ValidityPeriod(month.atDay(1), month.plusMonths(1).atDay(1));
        ValidityPeriod clipped = intersect(itemPeriod, window);
        if (clipped == null) {
            return List.of();
        }

        List<ValidityPeriod> pricePeriods = prices.stream().map(PriceSegment::period).toList();
        if (!PriceCoverage.covers(pricePeriods, clipped)) {
            throw new IllegalStateException("Нет цены плана на часть периода позиции в месяце " + month);
        }

        int daysInMonth = month.lengthOfMonth();
        List<ProratedLine> lines = new ArrayList<>();
        for (PriceSegment price : prices) {
            ValidityPeriod part = intersect(clipped, price.period());
            if (part == null) {
                continue;
            }
            int days = (int) ChronoUnit.DAYS.between(part.from(), part.to());
            BigDecimal amount = price.monthlyPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .multiply(BigDecimal.valueOf(days))
                    .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
            lines.add(new ProratedLine(part.from(), part.to(), days, daysInMonth, price.monthlyPrice(), amount));
        }
        lines.sort(Comparator.comparing(ProratedLine::from));
        return lines;
    }

    /** Пересечение двух периодов; null — если пересечения нет. null в to означает «бессрочно». */
    private static ValidityPeriod intersect(ValidityPeriod a, ValidityPeriod b) {
        LocalDate from = a.from().isAfter(b.from()) ? a.from() : b.from();
        LocalDate to = earlierEnd(a.to(), b.to());
        if (to != null && !from.isBefore(to)) {
            return null;
        }
        return new ValidityPeriod(from, to);
    }

    private static LocalDate earlierEnd(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }
}
