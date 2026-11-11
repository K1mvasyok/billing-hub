package io.github.k1mvasyok.billing.invoicing.app;

import io.github.k1mvasyok.billing.contracts.api.BillableContract;
import io.github.k1mvasyok.billing.contracts.api.BillableItem;
import io.github.k1mvasyok.billing.contracts.api.ContractDirectory;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRun;
import io.github.k1mvasyok.billing.invoicing.domain.Invoice;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceKind;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import io.github.k1mvasyok.billing.invoicing.domain.PriceSegment;
import io.github.k1mvasyok.billing.invoicing.domain.ProrationCalculator;
import io.github.k1mvasyok.billing.invoicing.domain.ProratedLine;
import io.github.k1mvasyok.billing.invoicing.infra.BillingRunRepository;
import io.github.k1mvasyok.billing.invoicing.infra.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Фоновая часть выставления счетов. Договоры идут пачками, каждая пачка — своя транзакция:
 * сбой одной пачки не откатывает уже выставленные.
 */
@Component
public class BillingRunWorker {

    static final int BATCH_SIZE = 500;

    private static final Logger log = LoggerFactory.getLogger(BillingRunWorker.class);

    private final ContractDirectory contracts;
    private final InvoiceRepository invoices;
    private final BillingRunRepository runs;
    private final TransactionTemplate transactions;

    public BillingRunWorker(ContractDirectory contracts, InvoiceRepository invoices,
                            BillingRunRepository runs, TransactionTemplate transactions) {
        this.contracts = contracts;
        this.invoices = invoices;
        this.runs = runs;
        this.transactions = transactions;
    }

    public void run(UUID runId, LocalDate period) {
        try {
            UUID afterId = null;
            while (true) {
                List<BillableContract> batch = contracts.findBillable(YearMonth.from(period), afterId, BATCH_SIZE);
                if (batch.isEmpty()) {
                    break;
                }
                transactions.executeWithoutResult(status -> processBatch(runId, period, batch));
                afterId = batch.getLast().contractId();
            }
            transactions.executeWithoutResult(status -> finish(runId, true));
        } catch (RuntimeException ex) {
            log.error("Запуск биллинга {} завершился ошибкой", runId, ex);
            transactions.executeWithoutResult(status -> finish(runId, false));
        }
    }

    private void processBatch(UUID runId, LocalDate period, List<BillableContract> batch) {
        int created = 0;
        int skipped = 0;
        int errors = 0;
        for (BillableContract contract : batch) {
            // BR-04: повторный запуск пропускает договоры, по которым счёт уже есть.
            if (invoices.existsByContractIdAndPeriodAndKindAndStatusNot(
                    contract.contractId(), period, InvoiceKind.REGULAR, InvoiceStatus.CANCELLED)) {
                skipped++;
                continue;
            }
            try {
                Optional<Invoice> invoice = buildRegularInvoice(period, contract);
                if (invoice.isEmpty()) {
                    skipped++;
                    continue;
                }
                invoices.save(invoice.get());
                created++;
            } catch (IllegalStateException ex) {
                // Нет цены на часть периода: счёт по договору не выставляем, остальные идут дальше.
                errors++;
                log.warn("По договору {} счёт не выставлен: {}", contract.contractId(), ex.getMessage());
            }
        }
        invoices.flush();
        runs.findById(runId).orElseThrow().addBatch(batch.size(), created, skipped, errors);
    }

    /** Строки считаем до выдачи номера: договор без начислений не расходует номер. */
    private Optional<Invoice> buildRegularInvoice(LocalDate period, BillableContract contract) {
        List<PendingLine> pending = new ArrayList<>();
        YearMonth month = YearMonth.from(period);
        for (BillableItem item : contract.items()) {
            List<PriceSegment> segments = item.prices().stream()
                    .map(price -> new PriceSegment(price.period(), price.monthlyPrice()))
                    .toList();
            for (ProratedLine line : ProrationCalculator.prorate(month, item.period(), item.quantity(), segments)) {
                pending.add(new PendingLine(item.planId(), item.planName(), item.quantity(), line));
            }
        }
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        LocalDate issueDate = month.atEndOfMonth();
        LocalDate dueDate = issueDate.plusDays(contract.paymentTermDays());
        Invoice invoice = Invoice.regular(
                Invoice.numberFor(period, invoices.nextNumber()),
                contract.contractId(), contract.customerId(), period, issueDate, dueDate);
        for (PendingLine line : pending) {
            invoice.addLine(line.planId(), line.planName(), line.quantity(), line.line());
        }
        return Optional.of(invoice);
    }

    private void finish(UUID runId, boolean success) {
        BillingRun run = runs.findById(runId).orElseThrow();
        if (success) {
            run.complete(Instant.now());
        } else {
            run.fail(Instant.now());
        }
    }

    private record PendingLine(UUID planId, String planName, int quantity, ProratedLine line) {
    }
}
