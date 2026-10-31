package io.github.k1mvasyok.billing.contracts.app;

import io.github.k1mvasyok.billing.contracts.api.ContractDetailResponse;
import io.github.k1mvasyok.billing.contracts.api.ContractResponse;
import io.github.k1mvasyok.billing.contracts.api.ItemResponse;
import io.github.k1mvasyok.billing.contracts.domain.Contract;
import io.github.k1mvasyok.billing.contracts.domain.ContractItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    ContractResponse toContractResponse(Contract contract);

    ContractDetailResponse toDetailResponse(Contract contract);

    @Mapping(target = "planId", source = "plan.id")
    ItemResponse toItemResponse(ContractItem item);
}
