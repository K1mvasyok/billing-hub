package io.github.k1mvasyok.billing.contracts.app;

import io.github.k1mvasyok.billing.contracts.api.PlanRequest;
import io.github.k1mvasyok.billing.contracts.api.PlanResponse;
import io.github.k1mvasyok.billing.contracts.api.PriceRequest;
import io.github.k1mvasyok.billing.contracts.api.PriceResponse;
import io.github.k1mvasyok.billing.contracts.domain.Plan;
import io.github.k1mvasyok.billing.contracts.domain.PlanPrice;
import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;
import io.github.k1mvasyok.billing.contracts.infra.PlanPriceRepository;
import io.github.k1mvasyok.billing.contracts.infra.PlanRepository;
import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PlanService {

    private final PlanRepository plans;
    private final PlanPriceRepository prices;
    private final PlanMapper mapper;
    private final Clock clock;

    public PlanService(PlanRepository plans, PlanPriceRepository prices, PlanMapper mapper, Clock clock) {
        this.plans = plans;
        this.prices = prices;
        this.mapper = mapper;
        this.clock = clock;
    }

    public PlanResponse create(PlanRequest request) {
        if (plans.existsByCode(request.code())) {
            throw new BusinessRuleException("plan-code-exists", "План с кодом " + request.code() + " уже есть");
        }
        return mapper.toPlanResponse(plans.saveAndFlush(new Plan(request.code(), request.name())));
    }

    @Transactional(readOnly = true)
    public Page<PlanResponse> list(Pageable pageable) {
        return plans.findAll(pageable).map(mapper::toPlanResponse);
    }

    /**
     * Цена плана. Если новая цена начинается строго внутри открытой, это смена цены: открытая цена
     * закрывается на дату новой. Закрытие сохраняется до вставки, иначе БД откатит вставку
     * (Hibernate пишет вставки раньше обновлений).
     * Иначе пересечение проверяет БД (exclusion constraint) и обработчик вернёт 409 plan-price-overlap.
     */
    public PriceResponse addPrice(UUID planId, PriceRequest request) {
        Plan plan = plans.findById(planId).orElseThrow(() -> new NotFoundException("План", planId));
        ValidityPeriod period = new ValidityPeriod(request.validFrom(), request.validTo());
        PlanPrice open = prices.findByPlanId(planId).stream()
                .filter(price -> price.getValidTo() == null)
                .findFirst()
                .orElse(null);
        if (open != null && period.from().isAfter(open.getValidFrom())) {
            requireChangeAllowed(period);
            open.closeAt(period.from());
            prices.saveAndFlush(open);
        }
        PlanPrice price = new PlanPrice(plan, period, request.monthlyPrice());
        return mapper.toPriceResponse(prices.saveAndFlush(price));
    }

    /**
     * Смена цены: новая цена открыта и начинается не раньше первого числа текущего месяца.
     * Так смена не трогает счета за прошедшие месяцы, которые уже выставлены по старой цене.
     */
    private void requireChangeAllowed(ValidityPeriod period) {
        if (period.to() != null) {
            throw new BusinessRuleException("plan-price-change-must-be-open",
                    "При смене цены новая цена должна быть без даты окончания");
        }
        LocalDate currentMonth = YearMonth.now(clock).atDay(1);
        if (period.from().isBefore(currentMonth)) {
            throw new BusinessRuleException("plan-price-change-in-past",
                    "Цену нельзя менять задним числом: смена начинается не раньше первого числа текущего месяца");
        }
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> listPrices(UUID planId) {
        if (!plans.existsById(planId)) {
            throw new NotFoundException("План", planId);
        }
        return prices.findByPlanId(planId).stream()
                .sorted(Comparator.comparing(PlanPrice::getValidFrom))
                .map(mapper::toPriceResponse)
                .toList();
    }
}
