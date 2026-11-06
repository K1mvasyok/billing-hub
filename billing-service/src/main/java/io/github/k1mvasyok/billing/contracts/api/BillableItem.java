package io.github.k1mvasyok.billing.contracts.api;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;

import java.util.List;
import java.util.UUID;

/** Позиция договора с ценами её плана. Отрезки месяца режет расчёт, а не контракты. */
public record BillableItem(UUID planId, String planName, int quantity, ValidityPeriod period,
                           List<PlanPriceView> prices) {
}
