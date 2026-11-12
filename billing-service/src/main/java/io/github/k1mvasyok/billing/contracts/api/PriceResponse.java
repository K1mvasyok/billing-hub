package io.github.k1mvasyok.billing.contracts.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PriceResponse(
        UUID id,
        UUID planId,
        LocalDate validFrom,
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal monthlyPrice
) {
}
