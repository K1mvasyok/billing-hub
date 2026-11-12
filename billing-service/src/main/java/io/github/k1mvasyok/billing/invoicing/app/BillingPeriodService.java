package io.github.k1mvasyok.billing.invoicing.app;

import io.github.k1mvasyok.billing.invoicing.api.BillingPeriodResponse;
import io.github.k1mvasyok.billing.invoicing.domain.BillingPeriod;
import io.github.k1mvasyok.billing.invoicing.domain.BillingPeriodStatus;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRunStatus;
import io.github.k1mvasyok.billing.invoicing.infra.BillingPeriodRepository;
import io.github.k1mvasyok.billing.invoicing.infra.BillingRunRepository;
import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
public class BillingPeriodService {

    private final BillingPeriodRepository periods;
    private final BillingRunRepository runs;
    private final BillingLock billingLock;
    private final InvoicingMapper mapper;
    private final Clock clock;

    public BillingPeriodService(BillingPeriodRepository periods, BillingRunRepository runs,
                                BillingLock billingLock, InvoicingMapper mapper, Clock clock) {
        this.periods = periods;
        this.runs = runs;
        this.billingLock = billingLock;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Разбирает yyyy-MM в первое число месяца. Неверный формат — IllegalArgumentException (400). */
    public static LocalDate parseMonth(String text) {
        try {
            return YearMonth.parse(text).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Период должен быть в формате yyyy-MM, получено: " + text);
        }
    }

    @Transactional(readOnly = true)
    public List<BillingPeriodResponse> list() {
        return periods.findAllByOrderByPeriodAsc().stream().map(mapper::toResponse).toList();
    }

    /**
     * Закрытие месяца. Нельзя закрыть месяц, если идёт расчёт за него или за более ранний:
     * такой расчёт пишет счета в периоды, которые закрытие как раз блокирует.
     * closed_by пока пустой: привязка к пользователю появится с безопасностью (M6).
     */
    public BillingPeriodResponse close(String text) {
        LocalDate period = parseMonth(text);
        billingLock.acquire();
        requireFinished(period);
        if (runs.existsByPeriodLessThanEqualAndStatus(period, BillingRunStatus.RUNNING)) {
            throw new BusinessRuleException("billing-run-in-progress",
                    "Идёт выставление счетов за период не позже " + YearMonth.from(period)
                            + ", закрыть период нельзя");
        }
        BillingPeriod billing = periods.findByPeriod(period).orElseGet(() -> new BillingPeriod(period));
        if (billing.getStatus() == BillingPeriodStatus.CLOSED) {
            throw new BusinessRuleException("period-already-closed",
                    "Период " + YearMonth.from(period) + " уже закрыт");
        }
        billing.close(Instant.now(), null);
        return mapper.toResponse(periods.saveAndFlush(billing));
    }

    /**
     * Закрывать и выставлять счета можно только за месяц, который уже закончился.
     * Счёт датируется последним днём месяца, поэтому за текущий и будущий месяцы его выставлять нельзя.
     */
    @Transactional(readOnly = true)
    public void requireFinished(LocalDate period) {
        LocalDate currentMonth = YearMonth.now(clock).atDay(1);
        if (!period.isBefore(currentMonth)) {
            throw new BusinessRuleException("period-not-finished",
                    "Период " + YearMonth.from(period) + " ещё не закончился: закрыть и выставить счета"
                            + " можно только за прошедший месяц");
        }
    }

    /**
     * BR-05: счёта с периодом не позже закрытого нельзя ни создать, ни отменить.
     * Исправления идут корректировкой в открытом периоде.
     */
    @Transactional(readOnly = true)
    public void requireOpen(LocalDate period) {
        periods.findFirstByStatusOrderByPeriodDesc(BillingPeriodStatus.CLOSED).ifPresent(closed -> {
            if (!period.isAfter(closed.getPeriod())) {
                throw new BusinessRuleException("period-closed",
                        "Период " + YearMonth.from(period) + " закрыт: изменения до "
                                + YearMonth.from(closed.getPeriod()) + " включительно запрещены");
            }
        });
    }
}
