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
    private final InvoicingMapper mapper;

    public BillingPeriodService(BillingPeriodRepository periods, BillingRunRepository runs,
                                InvoicingMapper mapper) {
        this.periods = periods;
        this.runs = runs;
        this.mapper = mapper;
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
     * Закрытие месяца. Пока выполняется расчёт за этот месяц, закрыть нельзя.
     * closed_by пока пустой: привязка к пользователю появится с безопасностью (M6).
     */
    public BillingPeriodResponse close(String text) {
        LocalDate period = parseMonth(text);
        if (runs.existsByPeriodAndStatus(period, BillingRunStatus.RUNNING)) {
            throw new BusinessRuleException("billing-run-in-progress",
                    "За " + YearMonth.from(period) + " идёт выставление счетов, закрыть период нельзя");
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
