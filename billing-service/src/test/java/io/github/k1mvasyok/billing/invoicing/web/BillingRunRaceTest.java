package io.github.k1mvasyok.billing.invoicing.web;

import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import io.github.k1mvasyok.billing.invoicing.app.BillingLock;
import io.github.k1mvasyok.billing.invoicing.app.BillingPeriodService;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRun;
import io.github.k1mvasyok.billing.invoicing.domain.BillingRunStatus;
import io.github.k1mvasyok.billing.invoicing.infra.BillingRunRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Гонки запусков, закрытия и отмены. Тесты держат общую блокировку в одной транзакции и проверяют,
 * что второй запрос ждёт её и видит результат первого. Без блокировки второй запрос прошёл бы раньше коммита.
 * <p>
 * Закрытие периода блокирует все более ранние периоды, поэтому периоды подобраны по порядку выполнения:
 * закрытие 2045-03, затем 2047-02, затем 2048-02. Порядок задан через {@link Order}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillingRunRaceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingLock billingLock;

    @Autowired
    private BillingPeriodService periods;

    @Autowired
    private BillingRunRepository runs;

    @Autowired
    private TransactionTemplate transactions;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    /** Незавершённые запуски в тестах не должны блокировать закрытия в следующих тестах класса. */
    @AfterEach
    void finishOpenRuns() {
        transactions.executeWithoutResult(status -> runs.findByStatus(BillingRunStatus.RUNNING)
                .forEach(run -> run.fail(Instant.now())));
    }

    @Test
    @Order(1)
    @DisplayName("BR-05: закрытие месяца блокируется запуском за более ранний период")
    void closeIsBlockedByRunForEarlierPeriod() throws Exception {
        runs.saveAndFlush(new BillingRun(LocalDate.of(2045, 4, 1), Instant.now()));

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2045-05"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:billing-run-in-progress"));

        // Запуск за апрель не пишет в март, поэтому закрыть март можно
        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2045-03"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("BR-04: второй идущий запуск за тот же период отклоняет индекс, завершённые запуски допускаются")
    void secondRunningRunForSamePeriodIsRejectedByIndex() {
        LocalDate period = LocalDate.of(2046, 1, 1);
        runs.saveAndFlush(new BillingRun(period, Instant.now()));

        Throwable thrown = catchThrowable(() -> runs.saveAndFlush(new BillingRun(period, Instant.now())));

        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(((DataIntegrityViolationException) thrown).getMostSpecificCause().getMessage())
                .contains("ux_billing_run_running_period");

        BillingRun finishedOne = new BillingRun(period, Instant.now());
        finishedOne.complete(Instant.now());
        BillingRun finishedTwo = new BillingRun(period, Instant.now());
        finishedTwo.complete(Instant.now());
        runs.saveAndFlush(finishedOne);
        runs.saveAndFlush(finishedTwo);
    }

    @Test
    @Order(3)
    @DisplayName("BR-04: второй старт ждёт первый и получает 409, а не 202")
    void startWaitsForInFlightStartAndGetsConflict() throws Exception {
        MvcResult result = raceAgainstLockHolder(
                () -> runs.saveAndFlush(new BillingRun(LocalDate.of(2049, 1, 1), Instant.now())),
                () -> mockMvc.perform(post("/api/v1/billing-runs")
                                .contentType(APPLICATION_JSON)
                                .content("{\"period\": \"2049-01\"}"))
                        .andReturn());

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("billing-run-in-progress");
    }

    @Test
    @Order(4)
    @DisplayName("BR-05: отмена счёта ждёт закрытие периода и получает 409")
    void cancelWaitsForPeriodCloseAndGetsClosedConflict() throws Exception {
        String contractId = fixtures.contract("2047-01-01", "2047-12-31");
        String invoiceId = fixtures.correction(contractId, "2047-02", "10.00");

        MvcResult result = raceAgainstLockHolder(
                () -> periods.close("2047-02"),
                () -> mockMvc.perform(post("/api/v1/invoices/{id}/cancel", invoiceId)).andReturn());

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("period-closed");
    }

    @Test
    @Order(5)
    @DisplayName("BR-05: корректировка ждёт закрытие периода и получает 409")
    void correctionWaitsForPeriodCloseAndGetsClosedConflict() throws Exception {
        String contractId = fixtures.contract("2048-01-01", "2048-12-31");

        MvcResult result = raceAgainstLockHolder(
                () -> periods.close("2048-02"),
                () -> mockMvc.perform(post("/api/v1/invoices/corrections")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {"contractId": "%s", "period": "2048-02", "amount": "5.00"}
                                        """.formatted(contractId)))
                        .andReturn());

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("period-closed");
    }

    /**
     * Поток A выполняет work внутри транзакции под общей блокировкой и держит её 1,5 с до коммита.
     * Поток B выполняет contender, когда блокировка уже занята. Возвращает результат B.
     */
    private MvcResult raceAgainstLockHolder(Runnable work, Callable<MvcResult> contender) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        try {
            Future<?> holder = pool.submit(() -> transactions.executeWithoutResult(status -> {
                billingLock.acquire();
                work.run();
                locked.countDown();
                sleepMillis(1500);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<MvcResult> waiting = pool.submit(contender);
            holder.get(30, TimeUnit.SECONDS);
            return waiting.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
