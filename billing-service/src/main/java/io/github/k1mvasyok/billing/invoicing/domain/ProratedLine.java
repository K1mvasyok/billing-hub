package io.github.k1mvasyok.billing.invoicing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Строка счёта за отрезок месяца, на котором цена и количество постоянны (BR-03).
 * monthlyPrice — цена за месяц на этом отрезке, amount уже округлена до копеек (BR-01).
 */
public record ProratedLine(LocalDate from, LocalDate to, int days, int daysInMonth,
                           BigDecimal monthlyPrice, BigDecimal amount) {
}
