package io.github.k1mvasyok.billing.invoicing.api;

import io.github.k1mvasyok.billing.invoicing.domain.BillingPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillingPeriodResponse(UUID id, LocalDate period, BillingPeriodStatus status, Instant closedAt,
                                    String closedBy) {
}
