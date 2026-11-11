package io.github.k1mvasyok.billing.contracts.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Расторжение с даты effectiveDate. Это первый день, за который договор уже не действует:
 * расторжение 16 марта даёт последний оплачиваемый день 15 марта.
 */
public record TerminateRequest(
        @NotNull LocalDate effectiveDate
) {
}
