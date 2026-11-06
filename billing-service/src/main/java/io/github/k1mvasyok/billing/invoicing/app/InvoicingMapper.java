package io.github.k1mvasyok.billing.invoicing.app;

import io.github.k1mvasyok.billing.invoicing.api.BillingPeriodResponse;
import io.github.k1mvasyok.billing.invoicing.api.BillingRunResponse;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceDetailResponse;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceLineResponse;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceResponse;
import io.github.k1mvasyok.billing.invoicing.domain.BillingPeriod;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRun;
import io.github.k1mvasyok.billing.invoicing.domain.Invoice;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceLine;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoicingMapper {

    BillingPeriodResponse toResponse(BillingPeriod period);

    BillingRunResponse toResponse(BillingRun run);

    InvoiceResponse toResponse(Invoice invoice);

    InvoiceDetailResponse toDetail(Invoice invoice);

    InvoiceLineResponse toLineResponse(InvoiceLine line);
}
