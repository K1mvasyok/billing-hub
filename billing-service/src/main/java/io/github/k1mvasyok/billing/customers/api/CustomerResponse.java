package io.github.k1mvasyok.billing.customers.api;

import io.github.k1mvasyok.billing.customers.domain.CustomerStatus;
import io.github.k1mvasyok.billing.customers.domain.CustomerType;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        CustomerType type,
        String name,
        String taxId,
        String email,
        String phone,
        CustomerStatus status,
        boolean dunningOptOut,
        Long version
) {
}
