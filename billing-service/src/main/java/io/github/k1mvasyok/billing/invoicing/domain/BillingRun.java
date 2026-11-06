package io.github.k1mvasyok.billing.invoicing.domain;

import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** Запуск выставления счетов за период. Счётчики растут по пачкам, каждая пачка — своя транзакция. */
@Entity
@Table(name = "billing_run")
public class BillingRun extends AbstractEntity {

    @Column(nullable = false)
    private LocalDate period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BillingRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "contracts_total", nullable = false)
    private int contractsTotal;

    @Column(name = "invoices_created", nullable = false)
    private int invoicesCreated;

    @Column(nullable = false)
    private int skipped;

    @Column(nullable = false)
    private int errors;

    protected BillingRun() {
    }

    public BillingRun(LocalDate period, Instant startedAt) {
        this.period = period;
        this.status = BillingRunStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public void addBatch(int contracts, int created, int skippedInBatch, int errorsInBatch) {
        this.contractsTotal += contracts;
        this.invoicesCreated += created;
        this.skipped += skippedInBatch;
        this.errors += errorsInBatch;
    }

    public void complete(Instant at) {
        this.status = BillingRunStatus.COMPLETED;
        this.finishedAt = at;
    }

    public void fail(Instant at) {
        this.status = BillingRunStatus.FAILED;
        this.finishedAt = at;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public BillingRunStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getContractsTotal() {
        return contractsTotal;
    }

    public int getInvoicesCreated() {
        return invoicesCreated;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getErrors() {
        return errors;
    }
}
