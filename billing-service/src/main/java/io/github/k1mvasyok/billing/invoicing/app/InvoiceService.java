package io.github.k1mvasyok.billing.invoicing.app;

import io.github.k1mvasyok.billing.contracts.api.ContractDirectory;
import io.github.k1mvasyok.billing.contracts.api.ContractRef;
import io.github.k1mvasyok.billing.invoicing.api.CorrectionRequest;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceDetailResponse;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceResponse;
import io.github.k1mvasyok.billing.invoicing.domain.Invoice;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import io.github.k1mvasyok.billing.invoicing.infra.InvoiceRepository;
import io.github.k1mvasyok.billing.invoicing.infra.InvoiceSpecifications;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final BillingPeriodService periods;
    private final BillingLock billingLock;
    private final ContractDirectory contracts;
    private final InvoicingMapper mapper;

    public InvoiceService(InvoiceRepository invoices, BillingPeriodService periods, BillingLock billingLock,
                          ContractDirectory contracts, InvoicingMapper mapper) {
        this.invoices = invoices;
        this.periods = periods;
        this.billingLock = billingLock;
        this.contracts = contracts;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> search(UUID customerId, UUID contractId, InvoiceStatus status,
                                        String period, Pageable pageable) {
        LocalDate month = period == null ? null : BillingPeriodService.parseMonth(period);
        return invoices.findAll(InvoiceSpecifications.matching(customerId, contractId, status, month), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse get(UUID id) {
        return mapper.toDetail(load(id));
    }

    /**
     * Отмена выставленного счёта в открытом периоде (BR-05, BR-06).
     * Разноски по счёту появятся в M3: тогда отмена будет запрещена, если по счёту есть оплаты.
     */
    public InvoiceResponse cancel(UUID id) {
        billingLock.acquire();
        Invoice invoice = load(id);
        periods.requireOpen(invoice.getPeriod());
        invoice.cancel();
        return mapper.toResponse(invoices.saveAndFlush(invoice));
    }

    /** Корректировка в открытом периоде: отдельный счёт, сумма может быть отрицательной (BR-05). */
    public InvoiceResponse createCorrection(CorrectionRequest request) {
        LocalDate period = BillingPeriodService.parseMonth(request.period());
        billingLock.acquire();
        periods.requireOpen(period);
        ContractRef contract = contracts.requireContract(request.contractId());
        LocalDate issueDate = period.withDayOfMonth(period.lengthOfMonth());
        LocalDate dueDate = issueDate.plusDays(contract.paymentTermDays());
        Invoice correction = Invoice.correction(
                Invoice.numberFor(period, invoices.nextNumber()),
                contract.contractId(), contract.customerId(), period, issueDate, dueDate, request.amount());
        return mapper.toResponse(invoices.saveAndFlush(correction));
    }

    private Invoice load(UUID id) {
        return invoices.findById(id).orElseThrow(() -> new NotFoundException("Счёт", id));
    }
}
