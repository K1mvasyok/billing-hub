package io.github.k1mvasyok.billing.contracts.api;

import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/** Договор без позиций: для списков, чтобы не тянуть позиции каждого договора. */
public record ContractResponse(
        UUID id,
        UUID customerId,
        String number,
        LocalDate signedOn,
        LocalDate validFrom,
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo,
        ContractStatus status,
        int paymentTermDays,
        Long version
) {
}
