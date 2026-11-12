package io.github.k1mvasyok.billing.contracts.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ContractRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 32) String number,
        @NotNull LocalDate signedOn,
        @NotNull LocalDate validFrom,
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo,
        @NotNull @PositiveOrZero Integer paymentTermDays
) {
}
