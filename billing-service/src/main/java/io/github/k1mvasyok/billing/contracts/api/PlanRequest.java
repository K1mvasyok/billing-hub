package io.github.k1mvasyok.billing.contracts.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlanRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Z0-9_]+") String code,
        @NotBlank @Size(max = 255) String name
) {
}
