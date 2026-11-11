package io.github.k1mvasyok.billing.invoicing.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Общая блокировка всех операций биллинга, которые проверяют или меняют закрытие периодов и запуски:
 * старт, закрытие, отмена счёта, корректировка и каждая пачка расчёта.
 * <p>
 * Блокировка транзакционная: держится до конца транзакции, поэтому её нужно брать первой,
 * до чтения состояния периодов. Тогда проверка и запись идут как одно действие.
 * Ключ один на всё приложение: блокируем всё сразу, а не период за периодом.
 */
@Component
public class BillingLock {

    /** Произвольный, но постоянный ключ advisory-блокировки биллинга. */
    static final long KEY = 7_340_001L;

    private final JdbcTemplate jdbc;

    public BillingLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Берёт блокировку в текущей транзакции. Повторный вызов в той же транзакции не ждёт сам себя. */
    public void acquire() {
        jdbc.execute("SELECT pg_advisory_xact_lock(" + KEY + ")");
    }
}
