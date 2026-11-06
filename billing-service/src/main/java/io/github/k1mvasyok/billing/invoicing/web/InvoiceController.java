package io.github.k1mvasyok.billing.invoicing.web;

import io.github.k1mvasyok.billing.invoicing.api.CorrectionRequest;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceDetailResponse;
import io.github.k1mvasyok.billing.invoicing.api.InvoiceResponse;
import io.github.k1mvasyok.billing.invoicing.app.InvoiceService;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
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
