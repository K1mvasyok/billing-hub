package io.github.k1mvasyok.billing.customers.api;

import io.github.k1mvasyok.billing.customers.domain.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
        @NotNull CustomerType type,
        @NotBlank @Size(max = 255) String name,
        @NotBlank String taxId,
        @Email @Size(max = 255) String email,
        @Size(max = 32) String phone
) {
}
