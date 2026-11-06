package io.github.k1mvasyok.billing.invoicing.web;

import io.github.k1mvasyok.billing.invoicing.api.BillingRunRequest;
import io.github.k1mvasyok.billing.invoicing.api.BillingRunResponse;
import io.github.k1mvasyok.billing.invoicing.app.BillingRunService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing-runs")
public class BillingRunController {

    private final BillingRunService service;

    public BillingRunController(BillingRunService service) {
        this.service = service;
    }

    /** 202: расчёт идёт в фоне, статус — по Location. */
    @PostMapping
    ResponseEntity<BillingRunResponse> start(@Valid @RequestBody BillingRunRequest request) {
        BillingRunResponse run = service.start(request.period());
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/billing-runs/" + run.id()))
                .body(run);
    }

    @GetMapping("/{id}")
    BillingRunResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
