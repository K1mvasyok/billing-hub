package io.github.k1mvasyok.billing.contracts.web;

import io.github.k1mvasyok.billing.contracts.api.PlanRequest;
import io.github.k1mvasyok.billing.contracts.api.PlanResponse;
import io.github.k1mvasyok.billing.contracts.api.PriceRequest;
import io.github.k1mvasyok.billing.contracts.api.PriceResponse;
import io.github.k1mvasyok.billing.contracts.app.PlanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        PlanResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/plans/" + created.id())).body(created);
    }

    @GetMapping
    PagedModel<PlanResponse> list(Pageable pageable) {
        return new PagedModel<>(service.list(pageable));
    }

    @PostMapping("/{id}/prices")
    ResponseEntity<PriceResponse> addPrice(@PathVariable UUID id, @Valid @RequestBody PriceRequest request) {
        return ResponseEntity.status(201).body(service.addPrice(id, request));
    }
}
