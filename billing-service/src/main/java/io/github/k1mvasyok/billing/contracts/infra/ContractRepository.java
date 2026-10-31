package io.github.k1mvasyok.billing.contracts.infra;

import io.github.k1mvasyok.billing.contracts.domain.Contract;
import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    boolean existsByNumber(String number);

    @Query("""
            select c from Contract c
            where (:customerId is null or c.customerId = :customerId)
              and (:status is null or c.status = :status)
            """)
    Page<Contract> search(@Param("customerId") UUID customerId,
                          @Param("status") ContractStatus status,
                          Pageable pageable);
}
