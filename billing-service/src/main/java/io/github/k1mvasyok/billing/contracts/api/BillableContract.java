package io.github.k1mvasyok.billing.contracts.api;

import java.util.List;
import java.util.UUID;

/** Договор с позициями, готовый для расчёта счёта за месяц. */
public record BillableContract(UUID contractId, UUID customerId, int paymentTermDays, List<BillableItem> items) {
}
