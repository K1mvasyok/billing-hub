package io.github.k1mvasyok.billing.contracts.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record ItemRequest(
        @NotNull UUID planId,
        @NotNull @Positive Integer quantity,
        @NotNull LocalDate validFrom,
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo
) {
}
