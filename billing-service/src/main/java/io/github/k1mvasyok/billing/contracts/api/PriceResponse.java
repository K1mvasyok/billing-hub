package io.github.k1mvasyok.billing.contracts.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PriceResponse(
        UUID id,
        UUID planId,
        LocalDate validFrom,
        LocalDate validTo,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal monthlyPrice
) {
}
