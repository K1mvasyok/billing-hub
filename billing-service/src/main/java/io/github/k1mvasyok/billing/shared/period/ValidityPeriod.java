package io.github.k1mvasyok.billing.shared.period;

import java.time.LocalDate;

/**
 * Период действия [from, to). Конец не входит в период, как и в диапазоне PostgreSQL '[)'.
 * {@code to == null} означает «без даты окончания».
 */
public record ValidityPeriod(LocalDate from, LocalDate to) {

    public ValidityPeriod {
        if (from == null) {
            throw new IllegalArgumentException("Дата начала периода обязательна");
        }
        if (to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("Дата начала должна быть раньше даты окончания");
        }
    }

    public boolean covers(LocalDate day) {
        return !day.isBefore(from) && (to == null || day.isBefore(to));
    }

    /** Период целиком лежит внутри outer. Открытый конец внутри закрытого outer не помещается. */
    public boolean isWithin(ValidityPeriod outer) {
        if (from.isBefore(outer.from)) {
            return false;
        }
        if (outer.to == null) {
            return true;
        }
        return to != null && !to.isAfter(outer.to);
    }
}
