package io.github.k1mvasyok.billing.invoicing.domain;

public enum InvoiceKind {
    /** Обычный счёт за период по договору. На договор и период — один, пока не отменён (BR-04). */
    REGULAR,
    /** Исправление в открытом периоде. Сумма может быть отрицательной (BR-05). */
    CORRECTION
}
