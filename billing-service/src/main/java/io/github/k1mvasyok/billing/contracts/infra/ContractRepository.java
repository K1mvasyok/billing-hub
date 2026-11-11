package io.github.k1mvasyok.billing.contracts.infra;

import io.github.k1mvasyok.billing.contracts.domain.Contract;
import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {

    boolean existsByNumber(String number);

    /**
     * Договоры к расчёту: активированные (действующие или расторгнутые), чей срок пересекает месяц [monthStart, monthEnd).
     * Черновик, расторгнутый до активации, сюда не попадает: у него нет activatedOn.
     */
    @Query("""
            select c.id from Contract c
            where c.status in :statuses
              and c.activatedOn is not null
              and c.validFrom < :monthEnd
              and (c.validTo is null or c.validTo > :monthStart)
              and (:afterId is null or c.id > :afterId)
            order by c.id
            """)
    List<UUID> findBillableIds(@Param("statuses") Collection<ContractStatus> statuses,
                               @Param("monthStart") LocalDate monthStart,
                               @Param("monthEnd") LocalDate monthEnd,
                               @Param("afterId") UUID afterId,
                               Pageable page);

    @Query("select distinct c from Contract c left join fetch c.items where c.id in :ids")
    List<Contract> findWithItemsByIdIn(@Param("ids") Collection<UUID> ids);
}
