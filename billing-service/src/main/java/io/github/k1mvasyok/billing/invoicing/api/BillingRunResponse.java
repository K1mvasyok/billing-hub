package io.github.k1mvasyok.billing.invoicing.api;

import io.github.k1mvasyok.billing.invoicing.domain.BillingRunStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillingRunResponse(UUID id, LocalDate period, BillingRunStatus status, Instant startedAt,
                                 Instant finishedAt, int contractsTotal, int invoicesCreated, int skipped,
                                 int errors) {
}
