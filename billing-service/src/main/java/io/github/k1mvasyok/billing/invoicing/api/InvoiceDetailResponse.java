package io.github.k1mvasyok.billing.invoicing.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceKind;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailResponse(UUID id, String number, UUID contractId, UUID customerId, LocalDate period,
                                    InvoiceKind kind, LocalDate issueDate, LocalDate dueDate, InvoiceStatus status,
                                    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total,
                                    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal paid,
                                    String accountingRef, List<InvoiceLineResponse> lines) {
}
