package io.github.k1mvasyok.billing.contracts.infra;

import io.github.k1mvasyok.billing.contracts.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    boolean existsByCode(String code);
}
