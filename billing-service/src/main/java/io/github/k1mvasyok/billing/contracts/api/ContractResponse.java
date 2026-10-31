package io.github.k1mvasyok.billing.contracts.api;

import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;

import java.time.LocalDate;
import java.util.UUID;

/** Договор без позиций: для списков, чтобы не тянуть позиции каждого договора. */
public record ContractResponse(
        UUID id,
        UUID customerId,
        String number,
        LocalDate signedOn,
        LocalDate validFrom,
        LocalDate validTo,
        ContractStatus status,
        int paymentTermDays,
        Long version
) {
}
