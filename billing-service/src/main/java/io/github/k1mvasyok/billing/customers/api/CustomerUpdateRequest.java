package io.github.k1mvasyok.billing.customers.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Частичное обновление (PATCH): поле null означает «не менять».
 * Очистить телефон или e-mail через API пока нельзя — это осознанное упрощение M1.
 */
public record CustomerUpdateRequest(
        @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 32) String phone
) {
}
