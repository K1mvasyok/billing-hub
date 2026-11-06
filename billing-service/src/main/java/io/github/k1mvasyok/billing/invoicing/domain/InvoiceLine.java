package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Строка счёта: план за отрезок месяца с постоянной ценой. */
@Entity
@Table(name = "invoice_line")
public class InvoiceLine extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** План из модуля contracts. Храним id, без связи JPA. */
    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(nullable = false)
    private int days;

    @Column(name = "days_in_month", nullable = false)
    private int daysInMonth;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected InvoiceLine() {
    }

    InvoiceLine(Invoice invoice, UUID planId, String description, int quantity, ProratedLine line) {
        this.invoice = invoice;
        this.planId = planId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = line.monthlyPrice();
        this.fromDate = line.from();
        this.toDate = line.to();
        this.days = line.days();
        this.daysInMonth = line.daysInMonth();
        this.amount = line.amount();
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public int getDays() {
        return days;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
