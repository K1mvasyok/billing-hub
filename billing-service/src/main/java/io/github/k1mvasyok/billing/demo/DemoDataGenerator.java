package io.github.k1mvasyok.billing.demo;

import io.github.k1mvasyok.billing.invoicing.api.BillingRunResponse;
import io.github.k1mvasyok.billing.invoicing.app.BillingRunService;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRunStatus;
import io.github.k1mvasyok.billing.shared.id.UuidV7;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Синтетические данные для замеров (профиль demo). Все имена, адреса и телефоны — из Datafaker.
 * Генерация идёт прямыми INSERT-ами пачками: для замеров нужен объём, а не проверка правил.
 * Выставление счетов запускается через BillingRunService, поэтому замеряется настоящий путь.
 * Повторный запуск ничего не делает, если клиенты уже есть.
 */
@Component
@Profile("demo")
public class DemoDataGenerator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataGenerator.class);

    private static final int BATCH = 1000;
    private static final int PLAN_COUNT = 5;
    private static final LocalDate YEAR_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2026, 12, 31);
    private static final LocalDate PRICE_CHANGE = LocalDate.of(2026, 7, 1);

    private final JdbcTemplate jdbc;
    private final BillingRunService billingRuns;
    private final int customerCount;
    private final int contractCount;
    private final Random random = new Random(42);
    private final Faker faker = new Faker(random);

    public DemoDataGenerator(JdbcTemplate jdbc, BillingRunService billingRuns,
                             @Value("${billing.demo.customers:10000}") int customerCount,
                             @Value("${billing.demo.contracts:15000}") int contractCount) {
        this.jdbc = jdbc;
        this.billingRuns = billingRuns;
        this.customerCount = customerCount;
        this.contractCount = contractCount;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long existing = jdbc.queryForObject("select count(*) from customer", Long.class);
        if (existing != null && existing > 0) {
            log.info("Демо-данные уже есть ({} клиентов), генерацию пропускаю", existing);
            return;
        }
        Instant started = Instant.now();
        List<UUID> plans = insertPlans();
        List<UUID> customers = insertCustomers();
        insertContracts(plans, customers);
        log.info("Демо-данные добавлены: {} клиентов, {} договоров за {}",
                customerCount, contractCount, Duration.between(started, Instant.now()));
        runBillingForYear();
    }

    /** По одному плану на тариф: цена меняется 1 июля 2026, чтобы у позиций был разрез по ценам (BR-03). */
    private List<UUID> insertPlans() {
        List<UUID> plans = new ArrayList<>();
        Timestamp now = Timestamp.from(Instant.now());
        for (int i = 1; i <= PLAN_COUNT; i++) {
            UUID planId = UuidV7.next();
            plans.add(planId);
            jdbc.update("insert into plan (id, code, name, created_at, updated_at, version) values (?, ?, ?, ?, ?, 0)",
                    planId, "DEMO_" + i, "Тариф " + i, now, now);
            jdbc.update("""
                            insert into plan_price (id, plan_id, valid_from, valid_to, monthly_price, created_at, updated_at, version)
                            values (?, ?, ?, ?, ?, ?, ?, 0)
                            """,
                    UuidV7.next(), planId, Date.valueOf("2025-01-01"), Date.valueOf(PRICE_CHANGE),
                    BigDecimal.valueOf(800L + i * 100), now, now);
            jdbc.update("""
                            insert into plan_price (id, plan_id, valid_from, valid_to, monthly_price, created_at, updated_at, version)
                            values (?, ?, ?, null, ?, ?, ?, 0)
                            """,
                    UuidV7.next(), planId, Date.valueOf(PRICE_CHANGE), BigDecimal.valueOf(1000L + i * 150), now, now);
        }
        return plans;
    }

    private List<UUID> insertCustomers() {
        List<UUID> ids = new ArrayList<>(customerCount);
        List<Object[]> rows = new ArrayList<>(BATCH);
        Timestamp now = Timestamp.from(Instant.now());
        for (int i = 0; i < customerCount; i++) {
            UUID id = UuidV7.next();
            ids.add(id);
            rows.add(new Object[]{
                    id,
                    faker.company().name(),
                    // Десятизначные ИНН, уникальные по номеру строки: синтетические, не настоящие.
                    String.format("%010d", 1_000_000_000L + i),
                    truncate(faker.internet().emailAddress(), 255),
                    truncate(faker.phoneNumber().cellPhone(), 32),
                    now, now});
            if (rows.size() == BATCH) {
                flushCustomers(rows);
            }
        }
        flushCustomers(rows);
        return ids;
    }

    private void flushCustomers(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                        insert into customer (id, type, name, tax_id, email, phone, status, dunning_opt_out, created_at, updated_at, version)
                        values (?, 'ORG', ?, ?, ?, ?, 'ACTIVE', false, ?, ?, 0)
                        """, rows);
        rows.clear();
    }

    private void insertContracts(List<UUID> plans, List<UUID> customers) {
        List<Object[]> contractRows = new ArrayList<>(BATCH);
        List<Object[]> itemRows = new ArrayList<>(BATCH * 2);
        Timestamp now = Timestamp.from(Instant.now());
        for (int i = 0; i < contractCount; i++) {
            UUID contractId = UuidV7.next();
            contractRows.add(new Object[]{
                    contractId, customers.get(random.nextInt(customers.size())), "D-%06d".formatted(i + 1),
                    Date.valueOf(YEAR_START), Date.valueOf(YEAR_START), Date.valueOf(YEAR_END), now, now});
            int items = 1 + random.nextInt(2);
            for (int item = 0; item < items; item++) {
                LocalDate from = random.nextInt(10) < 3 ? randomDayIn2026() : YEAR_START;
                itemRows.add(new Object[]{
                        UuidV7.next(), contractId, plans.get(random.nextInt(plans.size())),
                        1 + random.nextInt(5), Date.valueOf(from), Date.valueOf(YEAR_END), now, now});
            }
            if (contractRows.size() == BATCH) {
                flushContracts(contractRows, itemRows);
            }
        }
        flushContracts(contractRows, itemRows);
    }

    private void flushContracts(List<Object[]> contractRows, List<Object[]> itemRows) {
        if (!contractRows.isEmpty()) {
            jdbc.batchUpdate("""
                            insert into contract (id, customer_id, number, signed_on, valid_from, valid_to, status,
                                                  payment_term_days, created_at, updated_at, version)
                            values (?, ?, ?, ?, ?, ?, 'ACTIVE', 14, ?, ?, 0)
                            """, contractRows);
            contractRows.clear();
        }
        if (!itemRows.isEmpty()) {
            jdbc.batchUpdate("""
                            insert into contract_item (id, contract_id, plan_id, quantity, valid_from, valid_to,
                                                       created_at, updated_at, version)
                            values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                            """, itemRows);
            itemRows.clear();
        }
    }

    /** Выставление за каждый месяц 2026 года, по одному запуску; замеры пишем в лог и в docs/perf.md. */
    private void runBillingForYear() {
        for (int month = 1; month <= 12; month++) {
            YearMonth period = YearMonth.of(2026, month);
            Instant started = Instant.now();
            BillingRunResponse run = billingRuns.start(period.toString());
            BillingRunResponse finished = awaitFinished(run.id());
            log.info("Запуск биллинга {}: статус={}, договоров={}, создано={}, пропущено={}, ошибок={}, время={}",
                    period, finished.status(), finished.contractsTotal(), finished.invoicesCreated(),
                    finished.skipped(), finished.errors(), Duration.between(started, Instant.now()));
        }
    }

    private BillingRunResponse awaitFinished(UUID runId) {
        while (true) {
            BillingRunResponse run = billingRuns.get(runId);
            if (run.status() != BillingRunStatus.RUNNING) {
                return run;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание выставления прервано", ex);
            }
        }
    }

    private LocalDate randomDayIn2026() {
        return YEAR_START.plusDays(60 + random.nextInt(240));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
