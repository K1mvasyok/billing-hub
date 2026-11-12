package io.github.k1mvasyok.billing.contracts.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID planId,
        int quantity,
        LocalDate validFrom,
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo
) {
}
