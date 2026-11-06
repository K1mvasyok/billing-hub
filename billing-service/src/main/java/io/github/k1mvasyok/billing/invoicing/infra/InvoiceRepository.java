package io.github.k1mvasyok.billing.invoicing.infra;

import io.github.k1mvasyok.billing.invoicing.domain.Invoice;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceKind;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    /** BR-04: есть ли у договора обычный счёт за период, который не отменён. */
    boolean existsByContractIdAndPeriodAndKindAndStatusNot(UUID contractId, LocalDate period,
                                                           InvoiceKind kind, InvoiceStatus status);

    @Query(value = "select nextval('invoice_number_seq')", nativeQuery = true)
    long nextNumber();
}
