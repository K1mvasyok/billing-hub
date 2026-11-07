package io.github.k1mvasyok.billing.invoicing.web;

import io.github.k1mvasyok.billing.invoicing.api.CorrectionRequest;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceDetailResponse;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceResponse;
import io.github.k1mvasyok.billing.invoicing.app.BillingPeriodService;
import io.github.k1mvasyok.billing.invoicing.app.InvoiceExportService;
import io.github.k1mvasyok.billing.invoicing.app.InvoiceService;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final InvoiceService service;
    private final InvoiceExportService exportService;

    public InvoiceController(InvoiceService service, InvoiceExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    /** Реестр счетов в XLSX. Период разбираем до начала потока: после заголовка 200 статус уже не поменять. */
    @GetMapping("/export.xlsx")
    ResponseEntity<StreamingResponseBody> export(@RequestParam(required = false) UUID customerId,
                                                 @RequestParam(required = false) UUID contractId,
                                                 @RequestParam(required = false) InvoiceStatus status,
                                                 @RequestParam(required = false) String period) {
        LocalDate month = period == null ? null : BillingPeriodService.parseMonth(period);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoices.xlsx\"")
                .body(out -> exportService.export(customerId, contractId, status, month, out));
    }

    @GetMapping
    PagedModel<InvoiceResponse> list(@RequestParam(required = false) UUID customerId,
                                     @RequestParam(required = false) UUID contractId,
                                     @RequestParam(required = false) InvoiceStatus status,
                                     @RequestParam(required = false) String period,
                                     Pageable pageable) {
        return new PagedModel<>(service.search(customerId, contractId, status, period, pageable));
    }

    @GetMapping("/{id}")
    InvoiceDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    InvoiceResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PostMapping("/corrections")
    ResponseEntity<InvoiceResponse> createCorrection(@Valid @RequestBody CorrectionRequest request) {
        InvoiceResponse created = service.createCorrection(request);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + created.id())).body(created);
    }
}
