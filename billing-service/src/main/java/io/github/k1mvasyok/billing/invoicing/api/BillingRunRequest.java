package io.github.k1mvasyok.billing.invoicing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Запуск выставления счетов за месяц, формат yyyy-MM. */
public record BillingRunRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period) {
}
