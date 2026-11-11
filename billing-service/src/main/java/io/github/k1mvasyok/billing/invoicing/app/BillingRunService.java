package io.github.k1mvasyok.billing.invoicing.app;

import io.github.k1mvasyok.billing.invoicing.api.BillingRunResponse;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRun;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRunStatus;
import io.github.k1mvasyok.billing.invoicing.infra.BillingRunRepository;
import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Запуск выставления счетов. Сам расчёт идёт в фоне: сервис возвращает 202 сразу, статус видно в billing_run.
 */
@Service
public class BillingRunService {

    private final BillingRunRepository runs;
    private final BillingPeriodService periods;
    private final BillingRunWorker worker;
    private final BillingLock billingLock;
    private final TransactionTemplate transactions;
    private final InvoicingMapper mapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public BillingRunService(BillingRunRepository runs, BillingPeriodService periods, BillingRunWorker worker,
                             BillingLock billingLock, TransactionTemplate transactions, InvoicingMapper mapper) {
        this.runs = runs;
        this.periods = periods;
        this.worker = worker;
        this.billingLock = billingLock;
        this.transactions = transactions;
        this.mapper = mapper;
    }

    public BillingRunResponse start(String periodText) {
        LocalDate period = BillingPeriodService.parseMonth(periodText);
        // Проверка периода и вставка запуска идут под общей блокировкой: второй старт ждёт коммита первого.
        // Запись фиксируем до старта фонового потока, иначе он может не увидеть её.
        UUID runId = transactions.execute(status -> {
            billingLock.acquire();
            periods.requireOpen(period);
            if (runs.existsByPeriodAndStatus(period, BillingRunStatus.RUNNING)) {
                throw new BusinessRuleException("billing-run-in-progress",
                        "Выставление за этот период уже идёт");
            }
            return runs.saveAndFlush(new BillingRun(period, Instant.now())).getId();
        });
        executor.submit(() -> worker.run(runId, period));
        return get(runId);
    }

    @Transactional(readOnly = true)
    public BillingRunResponse get(UUID id) {
        return runs.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Запуск выставления", id));
    }

    /** Запуски, оставшиеся RUNNING после падения приложения, больше не продолжаются: помечаем как FAILED. */
    @EventListener(ApplicationReadyEvent.class)
    void failStaleRuns() {
        transactions.executeWithoutResult(status -> runs.findByStatus(BillingRunStatus.RUNNING)
                .forEach(run -> run.fail(Instant.now())));
    }
}
