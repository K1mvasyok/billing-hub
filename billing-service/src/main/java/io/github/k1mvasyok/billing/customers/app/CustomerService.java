package io.github.k1mvasyok.billing.customers.app;

import io.github.k1mvasyok.billing.customers.api.CustomerCreateRequest;
import io.github.k1mvasyok.billing.customers.api.CustomerDirectory;
import io.github.k1mvasyok.billing.customers.api.CustomerResponse;
import io.github.k1mvasyok.billing.customers.api.CustomerUpdateRequest;
import io.github.k1mvasyok.billing.customers.domain.Customer;
import io.github.k1mvasyok.billing.customers.domain.CustomerStatus;
import io.github.k1mvasyok.billing.customers.infra.CustomerRepository;
import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerService implements CustomerDirectory {

    private final CustomerRepository customers;
    private final CustomerMapper mapper;

    public CustomerService(CustomerRepository customers, CustomerMapper mapper) {
        this.customers = customers;
        this.mapper = mapper;
    }

    public CustomerResponse create(CustomerCreateRequest request) {
        // Частичный индекс в БД тоже не даст два активных клиента с одним ИНН.
        // Проверяем заранее, чтобы клиент получил понятное сообщение, а не общую ошибку ограничения.
        if (customers.existsByTaxIdAndStatus(request.taxId(), CustomerStatus.ACTIVE)) {
            throw new BusinessRuleException("customer-tax-id-exists",
                    "Активный клиент с таким ИНН уже есть");
        }
        Customer customer = new Customer(request.type(), request.name(), request.taxId(),
                request.email(), request.phone());
        // saveAndFlush: сразу получаем версию и ошибки базы внутри этого вызова.
        return mapper.toResponse(customers.saveAndFlush(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return mapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String query, CustomerStatus status, Pageable pageable) {
        return customers.search(blankToNull(query), status, pageable).map(mapper::toResponse);
    }

    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = load(id);
        customer.update(request.name(), request.email(), request.phone());
        return mapper.toResponse(customers.saveAndFlush(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireExists(UUID customerId) {
        if (!customers.existsById(customerId)) {
            throw new NotFoundException("Клиент", customerId);
        }
    }

    private Customer load(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Клиент", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
