package io.github.k1mvasyok.billing.contracts.api;

import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo,
        ContractStatus status,
        int paymentTermDays,
        Long version,
        List<ItemResponse> items
) {
}
