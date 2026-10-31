package io.github.k1mvasyok.billing.contracts.api;

import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Договор с позициями: для карточки одного договора. */
public record ContractDetailResponse(
        UUID id,
        UUID customerId,
        String number,
        LocalDate signedOn,
        LocalDate validFrom,
        LocalDate validTo,
        ContractStatus status,
        int paymentTermDays,
        Long version,
        List<ItemResponse> items
) {
}
