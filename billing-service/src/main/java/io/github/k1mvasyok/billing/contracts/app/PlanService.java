package io.github.k1mvasyok.billing.contracts.app;

import io.github.k1mvasyok.billing.contracts.api.PlanRequest;
import io.github.k1mvasyok.billing.contracts.api.PlanResponse;
import io.github.k1mvasyok.billing.contracts.api.PriceRequest;
import io.github.k1mvasyok.billing.contracts.api.PriceResponse;
import io.github.k1mvasyok.billing.contracts.domain.Plan;
import io.github.k1mvasyok.billing.contracts.domain.PlanPrice;
import io.github.k1mvasyok.billing.contracts.domain.ValidityPeriod;
import io.github.k1mvasyok.billing.contracts.infra.PlanPriceRepository;
import io.github.k1mvasyok.billing.contracts.infra.PlanRepository;
import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PlanService {

    private final PlanRepository plans;
    private final PlanPriceRepository prices;
    private final PlanMapper mapper;

    public PlanService(PlanRepository plans, PlanPriceRepository prices, PlanMapper mapper) {
        this.plans = plans;
        this.prices = prices;
        this.mapper = mapper;
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
     * Пересечение периодов проверяет БД (exclusion constraint). Если цена пересекается,
     * saveAndFlush бросит нарушение ограничения, а обработчик ошибок вернёт 409 с кодом plan-price-overlap.
     */
    public PriceResponse addPrice(UUID planId, PriceRequest request) {
        Plan plan = plans.findById(planId).orElseThrow(() -> new NotFoundException("План", planId));
        ValidityPeriod period = new ValidityPeriod(request.validFrom(), request.validTo());
        PlanPrice price = new PlanPrice(plan, period, request.monthlyPrice());
        return mapper.toPriceResponse(prices.saveAndFlush(price));
    }
}
