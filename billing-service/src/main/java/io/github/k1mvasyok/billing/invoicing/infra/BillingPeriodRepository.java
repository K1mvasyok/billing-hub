package io.github.k1mvasyok.billing.invoicing.infra;

import io.github.k1mvasyok.billing.invoicing.domain.BillingPeriod;
import io.github.k1mvasyok.billing.invoicing.domain.BillingPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, UUID> {

    Optional<BillingPeriod> findByPeriod(LocalDate period);

    /** Самый поздний закрытый период: всё до него включительно заблокировано (BR-05). */
    Optional<BillingPeriod> findFirstByStatusOrderByPeriodDesc(BillingPeriodStatus status);

    List<BillingPeriod> findAllByOrderByPeriodAsc();
}
