package io.github.k1mvasyok.billing.contracts.api;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;

import java.math.BigDecimal;

/** Цена плана за месяц на период. */
public record PlanPriceView(ValidityPeriod period, BigDecimal monthlyPrice) {
}
