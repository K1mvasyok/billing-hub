package io.github.k1mvasyok.billing.invoicing.infra;

import io.github.k1mvasyok.billing.invoicing.domain.BillingRun;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BillingRunRepository extends JpaRepository<BillingRun, UUID> {

    boolean existsByPeriodAndStatus(LocalDate period, BillingRunStatus status);

    boolean existsByPeriodLessThanEqualAndStatus(LocalDate period, BillingRunStatus status);

    List<BillingRun> findByStatus(BillingRunStatus status);
}
