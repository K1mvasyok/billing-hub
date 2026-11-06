package io.github.k1mvasyok.billing.invoicing.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceKind;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Счёт без строк: для списков. Полная карточка — {@link InvoiceDetailResponse}. */
public record InvoiceResponse(UUID id, String number, UUID contractId, UUID customerId, LocalDate period,
                              InvoiceKind kind, LocalDate issueDate, LocalDate dueDate, InvoiceStatus status,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal paid,
                              String accountingRef) {
}
