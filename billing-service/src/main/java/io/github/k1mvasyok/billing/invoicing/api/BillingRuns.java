package io.github.k1mvasyok.billing.invoicing.api;

import java.util.UUID;

/**
 * Фасад запуска выставления счетов для других частей приложения (например, демо-данных).
 * Снаружи модуля invoicing используется только он, а не классы app.
 */
public interface BillingRuns {

    /** Запускает выставление за период (yyyy-MM) в фоне. Ответ сразу, статус — в поле status. */
    BillingRunResponse start(String periodText);

    BillingRunResponse get(UUID id);
}
