package io.github.k1mvasyok.billing.invoicing.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

/** Корректировка договора в открытом периоде. amount может быть отрицательной. */
public record CorrectionRequest(
        @NotNull UUID contractId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) {
}
