package io.github.k1mvasyok.billing.invoicing.infra;

import io.github.k1mvasyok.billing.invoicing.domain.Invoice;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Фильтры счетов для списка и выгрузки. Условие добавляется только для заданного параметра:
 * null внутри JPQL PostgreSQL не типизирует, поэтому конструкция «:param is null or …» здесь не годится.
 */
public final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    public static Specification<Invoice> matching(UUID customerId, UUID contractId,
                                                  InvoiceStatus status, LocalDate period) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (contractId != null) {
                predicates.add(cb.equal(root.get("contractId"), contractId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (period != null) {
                predicates.add(cb.equal(root.get("period"), period));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
