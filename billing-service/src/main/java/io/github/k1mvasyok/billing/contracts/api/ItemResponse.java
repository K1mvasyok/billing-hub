package io.github.k1mvasyok.billing.contracts.api;

import java.time.LocalDate;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID planId,
        int quantity,
        LocalDate validFrom,
        LocalDate validTo
) {
}
