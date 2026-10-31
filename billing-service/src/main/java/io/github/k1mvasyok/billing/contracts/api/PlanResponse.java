package io.github.k1mvasyok.billing.contracts.api;

import java.util.UUID;

public record PlanResponse(UUID id, String code, String name, Long version) {
}
