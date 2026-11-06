package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;

import java.math.BigDecimal;

/** Цена плана за месяц на период. Входные данные для {@link ProrationCalculator}. */
public record PriceSegment(ValidityPeriod period, BigDecimal monthlyPrice) {
}
