package io.github.k1mvasyok.billing.invoicing.domain;

/**
 * Статусы счёта (BR-06). Переходы в PARTIALLY_PAID и PAID появятся с разноской платежей в M3.
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}
