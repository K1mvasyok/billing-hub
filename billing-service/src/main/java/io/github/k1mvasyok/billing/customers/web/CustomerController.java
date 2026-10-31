package io.github.k1mvasyok.billing.customers.web;

import io.github.k1mvasyok.billing.customers.api.CustomerCreateRequest;
import io.github.k1mvasyok.billing.customers.api.CustomerResponse;
import io.github.k1mvasyok.billing.customers.api.CustomerUpdateRequest;
import io.github.k1mvasyok.billing.customers.app.CustomerService;
import io.github.k1mvasyok.billing.customers.domain.CustomerStatus;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id()))
                .body(created);
    }

    @GetMapping
    PagedModel<CustomerResponse> list(@RequestParam(required = false) String query,
                                      @RequestParam(required = false) CustomerStatus status,
                                      Pageable pageable) {
        return new PagedModel<>(service.search(query, status, pageable));
    }

    @GetMapping("/{id}")
    CustomerResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerUpdateRequest request) {
        return service.update(id, request);
    }
}
