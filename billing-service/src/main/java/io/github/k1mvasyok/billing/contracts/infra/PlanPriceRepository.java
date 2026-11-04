package io.github.k1mvasyok.billing.contracts.infra;

import io.github.k1mvasyok.billing.contracts.domain.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, UUID> {

    List<PlanPrice> findByPlanId(UUID planId);
}
