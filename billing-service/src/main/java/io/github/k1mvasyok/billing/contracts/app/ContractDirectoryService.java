package io.github.k1mvasyok.billing.contracts.app;

import io.github.k1mvasyok.billing.contracts.api.BillableContract;
import io.github.k1mvasyok.billing.contracts.api.BillableItem;
import io.github.k1mvasyok.billing.contracts.api.ContractDirectory;
import io.github.k1mvasyok.billing.contracts.api.ContractRef;
import io.github.k1mvasyok.billing.contracts.api.PlanPriceView;
import io.github.k1mvasyok.billing.contracts.domain.Contract;
import io.github.k1mvasyok.billing.contracts.domain.ContractItem;
import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import io.github.k1mvasyok.billing.contracts.domain.Plan;
import io.github.k1mvasyok.billing.contracts.domain.PlanPrice;
import io.github.k1mvasyok.billing.contracts.infra.ContractRepository;
import io.github.k1mvasyok.billing.contracts.infra.PlanPriceRepository;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ContractDirectoryService implements ContractDirectory {

    private final ContractRepository contracts;
    private final PlanPriceRepository planPrices;

    public ContractDirectoryService(ContractRepository contracts, PlanPriceRepository planPrices) {
        this.contracts = contracts;
        this.planPrices = planPrices;
    }

    @Override
    public List<BillableContract> findBillable(YearMonth month, UUID afterId, int limit) {
        List<UUID> ids = contracts.findBillableIds(
                List.of(ContractStatus.ACTIVE, ContractStatus.TERMINATED),
                month.atDay(1), month.plusMonths(1).atDay(1), afterId, PageRequest.of(0, limit));
        if (ids.isEmpty()) {
            return List.of();
        }
        // Порядок страницы задаёт БД (ORDER BY id); keyset-пагинация опирается на него, а не на Java-сравнение UUID.
        Map<UUID, Contract> loaded = new HashMap<>();
        for (Contract contract : contracts.findWithItemsByIdIn(ids)) {
            loaded.put(contract.getId(), contract);
        }
        return ids.stream().map(loaded::get).map(this::toBillable).toList();
    }

    @Override
    public ContractRef requireContract(UUID contractId) {
        Contract contract = contracts.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Договор", contractId));
        return new ContractRef(contract.getId(), contract.getCustomerId(), contract.getPaymentTermDays());
    }

    private BillableContract toBillable(Contract contract) {
        List<BillableItem> items = contract.getItems().stream()
                .map(this::toBillableItem)
                .toList();
        return new BillableContract(contract.getId(), contract.getCustomerId(), contract.getPaymentTermDays(), items);
    }

    private BillableItem toBillableItem(ContractItem item) {
        Plan plan = item.getPlan();
        List<PlanPriceView> prices = planPrices.findByPlanId(plan.getId()).stream()
                .map(ContractDirectoryService::toPriceView)
                .toList();
        return new BillableItem(plan.getId(), plan.getName(), item.getQuantity(), item.period(), prices);
    }

    private static PlanPriceView toPriceView(PlanPrice price) {
        return new PlanPriceView(price.period(), price.getMonthlyPrice());
    }
}
