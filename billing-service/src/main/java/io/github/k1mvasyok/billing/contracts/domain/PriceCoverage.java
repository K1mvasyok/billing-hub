package io.github.k1mvasyok.billing.contracts.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Проверка, что цены плана покрывают период без разрывов.
 * Цены одного плана не пересекаются (это держит БД), поэтому достаточно идти от начала периода вперёд.
 */
public final class PriceCoverage {

    private PriceCoverage() {
    }

    /**
     * @param prices  периоды цен плана, в любом порядке
     * @param target  период позиции, который нужно покрыть
     * @return true, если каждый день target попадает в какой-то период цены
     */
    public static boolean covers(List<ValidityPeriod> prices, ValidityPeriod target) {
        List<ValidityPeriod> sorted = prices.stream()
                .sorted(Comparator.comparing(ValidityPeriod::from))
                .toList();
        LocalDate cursor = target.from();
        for (ValidityPeriod price : sorted) {
            if (price.from().isAfter(cursor)) {
                // Между cursor и началом этой цены дыра.
                return false;
            }
            if (price.to() == null) {
                // Бессрочная цена покрывает всё, что дальше cursor.
                return true;
            }
            if (price.to().isAfter(cursor)) {
                cursor = price.to();
            }
            if (target.to() != null && !cursor.isBefore(target.to())) {
                return true;
            }
        }
        return false;
    }
}
