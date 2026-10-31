package io.github.k1mvasyok.billing.customers.infra;

import io.github.k1mvasyok.billing.customers.domain.Customer;
import io.github.k1mvasyok.billing.customers.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByTaxIdAndStatus(String taxId, CustomerStatus status);

    /** Поиск по названию (без учёта регистра) или по ИНН, необязательный фильтр по статусу. */
    @Query("""
            select c from Customer c
            where (:status is null or c.status = :status)
              and (:query is null
                   or lower(c.name) like lower(concat('%', :query, '%'))
                   or c.taxId like concat('%', :query, '%'))
            """)
    Page<Customer> search(@Param("query") String query,
                          @Param("status") CustomerStatus status,
                          Pageable pageable);
}
