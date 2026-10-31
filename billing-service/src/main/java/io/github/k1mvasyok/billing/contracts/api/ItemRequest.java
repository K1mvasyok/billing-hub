package io.github.k1mvasyok.billing.contracts.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record ItemRequest(
        @NotNull UUID planId,
        @NotNull @Positive Integer quantity,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {
}
