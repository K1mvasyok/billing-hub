package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Расчётный месяц. Строка создаётся при закрытии: месяц без строки считается открытым.
 * period — первое число месяца.
 */
@Entity
@Table(name = "billing_period")
public class BillingPeriod extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private LocalDate period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BillingPeriodStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    protected BillingPeriod() {
    }

    public BillingPeriod(LocalDate period) {
        this.period = period;
        this.status = BillingPeriodStatus.OPEN;
    }

    public void close(Instant at, String by) {
        this.status = BillingPeriodStatus.CLOSED;
        this.closedAt = at;
        this.closedBy = by;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public BillingPeriodStatus getStatus() {
        return status;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }
}
