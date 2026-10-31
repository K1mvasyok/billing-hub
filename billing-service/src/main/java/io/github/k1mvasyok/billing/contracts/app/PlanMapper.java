package io.github.k1mvasyok.billing.contracts.app;

import io.github.k1mvasyok.billing.contracts.api.PlanResponse;
import io.github.k1mvasyok.billing.contracts.api.PriceResponse;
import io.github.k1mvasyok.billing.contracts.domain.Plan;
import io.github.k1mvasyok.billing.contracts.domain.PlanPrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanMapper {

    PlanResponse toPlanResponse(Plan plan);

    @Mapping(target = "planId", source = "plan.id")
    PriceResponse toPriceResponse(PlanPrice price);
}
