package io.github.k1mvasyok.billing.contracts.domain;

/**
 * Жизненный цикл договора. Допустимы только переходы вперёд:
 * DRAFT → ACTIVE → TERMINATED, а также DRAFT → TERMINATED.
 */
public enum ContractStatus {
    DRAFT,
    ACTIVE,
    TERMINATED;

    public boolean canMoveTo(ContractStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == TERMINATED;
            case ACTIVE -> target == TERMINATED;
            case TERMINATED -> false;
        };
    }
}
