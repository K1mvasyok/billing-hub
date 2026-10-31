package io.github.k1mvasyok.billing.customers.api;

import java.util.UUID;

/**
 * Публичный вход модуля клиентов для других модулей. Модуль contracts не лезет в репозитории customers,
 * а спрашивает через этот интерфейс (правило границ из CLAUDE.md).
 */
public interface CustomerDirectory {

    /** Бросает NotFoundException, если клиента с таким id нет. */
    void requireExists(UUID customerId);
}
