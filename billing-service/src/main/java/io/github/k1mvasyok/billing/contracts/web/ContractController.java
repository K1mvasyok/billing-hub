package io.github.k1mvasyok.billing.contracts.web;

import io.github.k1mvasyok.billing.contracts.api.ContractDetailResponse;
import io.github.k1mvasyok.billing.contracts.api.ContractRequest;
import io.github.k1mvasyok.billing.contracts.api.ContractResponse;
import io.github.k1mvasyok.billing.contracts.api.ItemRequest;
import io.github.k1mvasyok.billing.contracts.api.ItemResponse;
import io.github.k1mvasyok.billing.contracts.api.ItemUpdateRequest;
import io.github.k1mvasyok.billing.contracts.api.TerminateRequest;
import io.github.k1mvasyok.billing.contracts.app.ContractService;
import io.github.k1mvasyok.billing.contracts.domain.ContractStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        ContractResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/contracts/" + created.id())).body(created);
    }

    @GetMapping
    PagedModel<ContractResponse> list(@RequestParam(required = false) UUID customerId,
                                      @RequestParam(required = false) ContractStatus status,
                                      Pageable pageable) {
        return new PagedModel<>(service.search(customerId, status, pageable));
    }

    @GetMapping("/{id}")
    ContractDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/activate")
    ContractResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/terminate")
    ContractResponse terminate(@PathVariable UUID id, @Valid @RequestBody TerminateRequest request) {
        return service.terminate(id, request.effectiveDate());
    }

    @PostMapping("/{id}/items")
    ResponseEntity<ItemResponse> addItem(@PathVariable UUID id, @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(201).body(service.addItem(id, request));
    }

    @PatchMapping("/{id}/items/{itemId}")
    ItemResponse updateItem(@PathVariable UUID id, @PathVariable UUID itemId,
                            @Valid @RequestBody ItemUpdateRequest request) {
        return service.updateItem(id, itemId, request);
    }
}
