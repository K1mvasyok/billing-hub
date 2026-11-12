package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.money.Money;
import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Счёт клиенту по договору. Итог — сумма строк (обычный счёт) или сумма корректировки.
 * Правило «один обычный счёт на договор и период» держит частичный индекс в БД (BR-04).
 */
@Entity
@Table(name = "invoice")
public class Invoice extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String number;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private LocalDate period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceKind kind;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paid = BigDecimal.ZERO;

    @Column(name = "accounting_ref", length = 64)
    private String accountingRef;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {
    }

    private Invoice(String number, UUID contractId, UUID customerId, LocalDate period, InvoiceKind kind,
                    LocalDate issueDate, LocalDate dueDate) {
        this.number = number;
        this.contractId = contractId;
        this.customerId = customerId;
        this.period = period;
        this.kind = kind;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = InvoiceStatus.ISSUED;
        this.total = BigDecimal.ZERO;
    }

    /** Обычный счёт: строки добавляются через {@link #addLine}, итог копится по ним. */
    public static Invoice regular(String number, UUID contractId, UUID customerId, LocalDate period,
                                  LocalDate issueDate, LocalDate dueDate) {
        return new Invoice(number, contractId, customerId, period, InvoiceKind.REGULAR, issueDate, dueDate);
    }

    /** Корректировка без строк: сумма может быть отрицательной, ноль не имеет смысла. */
    public static Invoice correction(String number, UUID contractId, UUID customerId, LocalDate period,
                                     LocalDate issueDate, LocalDate dueDate, BigDecimal amount) {
        BigDecimal cents = Money.toCents(amount);
        if (cents.signum() == 0) {
            throw new IllegalArgumentException("Сумма корректировки не может быть нулевой");
        }
        Invoice invoice = new Invoice(number, contractId, customerId, period, InvoiceKind.CORRECTION,
                issueDate, dueDate);
        invoice.total = cents;
        return invoice;
    }

    public void addLine(UUID planId, String description, int quantity, ProratedLine line) {
        lines.add(new InvoiceLine(this, planId, description, quantity, line));
        total = total.add(line.amount());
    }

    /** Отмена: только выставленный счёт. Проверку открытого периода делает сервис (BR-05). */
    public void cancel() {
        if (status != InvoiceStatus.ISSUED) {
            throw new BusinessRuleException("invoice-invalid-transition",
                    "Отменить можно только выставленный счёт, сейчас статус " + status);
        }
        status = InvoiceStatus.CANCELLED;
    }

    /** Номер счёта: INV-yyyy-MM-NNNNNN, где NNNNNN — значение invoice_number_seq. */
    public static String numberFor(LocalDate period, long sequence) {
        return "INV-" + YearMonth.from(period) + "-" + String.format("%06d", sequence);
    }

    public String getNumber() {
        return number;
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public InvoiceKind getKind() {
        return kind;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public String getAccountingRef() {
        return accountingRef;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }
}
