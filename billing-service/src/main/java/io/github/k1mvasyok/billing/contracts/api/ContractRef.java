package io.github.k1mvasyok.billing.contracts.api;

import java.util.UUID;

/** Минимум данных о договоре, нужный модулю invoicing. */
public record ContractRef(UUID contractId, UUID customerId, int paymentTermDays) {
}
