package io.github.k1mvasyok.billing.invoicing.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceLineResponse(UUID planId, String description, int quantity,
                                  @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal unitPrice,
                                  LocalDate fromDate, LocalDate toDate, int days, int daysInMonth,
                                  @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) {
}
