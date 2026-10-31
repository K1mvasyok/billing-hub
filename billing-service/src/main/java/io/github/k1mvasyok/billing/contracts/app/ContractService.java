package io.github.k1mvasyok.billing.contracts.app;

import io.github.k1mvasyok.billing.contracts.api.ContractDetailResponse;
import io.github.k1mvasyok.billing.contracts.api.ContractRequest;
import io.github.k1mvasyok.billing.contracts.api.ContractResponse;
import io.github.k1mvasyok.billing.contracts.api.ItemRequest;
import io.github.k1mvasyok.billing.contracts.api.ItemResponse;
import io.github.k1mvasyok.billing.contracts.api.ItemUpdateRequest;
import io.github.k1mvasyok.billing.contracts.domain.Contract;
import io.github.k1mvasyok.billing.contracts.domain.ContractItem;
import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import io.github.k1mvasyok.billing.contracts.domain.Plan;
import io.github.k1mvasyok.billing.contracts.domain.ValidityPeriod;
import io.github.k1mvasyok.billing.contracts.infra.ContractRepository;
import io.github.k1mvasyok.billing.contracts.infra.PlanRepository;
import io.github.k1mvasyok.billing.customers.api.CustomerDirectory;
import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contracts;
    private final PlanRepository plans;
    private final CustomerDirectory customers;
    private final ContractMapper mapper;

    public ContractService(ContractRepository contracts, PlanRepository plans,
                           CustomerDirectory customers, ContractMapper mapper) {
        this.contracts = contracts;
        this.plans = plans;
        this.customers = customers;
        this.mapper = mapper;
    }

    public ContractResponse create(ContractRequest request) {
        customers.requireExists(request.customerId());
        if (contracts.existsByNumber(request.number())) {
            throw new BusinessRuleException("contract-number-exists",
                    "Договор с номером " + request.number() + " уже есть");
        }
        Contract contract = new Contract(request.customerId(), request.number(), request.signedOn(),
                new ValidityPeriod(request.validFrom(), request.validTo()), request.paymentTermDays());
        return mapper.toContractResponse(contracts.saveAndFlush(contract));
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> search(UUID customerId, ContractStatus status, Pageable pageable) {
        return contracts.search(customerId, status, pageable).map(mapper::toContractResponse);
    }

    @Transactional(readOnly = true)
    public ContractDetailResponse get(UUID id) {
        return mapper.toDetailResponse(load(id));
    }

    public ContractResponse activate(UUID id) {
        Contract contract = load(id);
        contract.activate();
        return mapper.toContractResponse(contracts.saveAndFlush(contract));
    }

    public ContractResponse terminate(UUID id) {
        Contract contract = load(id);
        contract.terminate();
        return mapper.toContractResponse(contracts.saveAndFlush(contract));
    }

    public ItemResponse addItem(UUID contractId, ItemRequest request) {
        Contract contract = load(contractId);
        Plan plan = plans.findById(request.planId())
                .orElseThrow(() -> new NotFoundException("План", request.planId()));
        ContractItem item = contract.addItem(plan, request.quantity(),
                new ValidityPeriod(request.validFrom(), request.validTo()));
        contracts.saveAndFlush(contract);
        return mapper.toItemResponse(item);
    }

    public ItemResponse updateItem(UUID contractId, UUID itemId, ItemUpdateRequest request) {
        Contract contract = load(contractId);
        ContractItem item = contract.updateItem(itemId, request.quantity(),
                new ValidityPeriod(request.validFrom(), request.validTo()));
        contracts.saveAndFlush(contract);
        return mapper.toItemResponse(item);
    }

    private Contract load(UUID id) {
        return contracts.findById(id).orElseThrow(() -> new NotFoundException("Договор", id));
    }
}
