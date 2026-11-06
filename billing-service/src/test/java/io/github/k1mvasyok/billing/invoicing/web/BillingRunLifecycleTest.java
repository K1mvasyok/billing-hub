package io.github.k1mvasyok.billing.invoicing.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозные сценарии выставления счетов через HTTP на PostgreSQL.
 * Месяцы 2031 года не пересекаются с данными других тестов; закрытие периодов проверяет отдельный класс.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BillingRunLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    @Test
    @DisplayName("BR-02, BR-03, BR-04: расчёт по дням, повторный запуск не дублирует счёт")
    void runBillsContractOnceAndSkipsItOnRerun() throws Exception {
        // 16–31 мая: 16 дней из 31, 1500 × 2 × 16 / 31 = 1548.39
        String contractId = fixtures.activeContractWithItem(
                "2031-01-01", "2031-12-31", "2031-05-16", "2031-06-01", 2);

        awaitCompleted(startRun("2031-05"));
        mockMvc.perform(get("/api/v1/invoices").param("contractId", contractId).param("period", "2031-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ISSUED"))
                .andExpect(jsonPath("$.content[0].kind").value("REGULAR"))
                .andExpect(jsonPath("$.content[0].total").value("1548.39"));

        String invoiceId = JsonPath.read(mockMvc.perform(get("/api/v1/invoices")
                        .param("contractId", contractId).param("period", "2031-05"))
                .andReturn().getResponse().getContentAsString(), "$.content[0].id");
        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2031-06-14"))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].days").value(16))
                .andExpect(jsonPath("$.lines[0].daysInMonth").value(31))
                .andExpect(jsonPath("$.lines[0].amount").value("1548.39"));

        String secondRun = awaitCompleted(startRun("2031-05"));
        assertThat(JsonPath.<Integer>read(secondRun, "$.invoicesCreated")).isZero();
        mockMvc.perform(get("/api/v1/invoices").param("contractId", contractId).param("period", "2031-05"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("BR-05: корректировка может быть отрицательной, отмена выставленного счёта — один раз (BR-06)")
    void correctionCanBeNegativeAndIssuedInvoiceCancelsOnce() throws Exception {
        String contractId = fixtures.contract("2031-01-01", "2031-12-31");
        String correctionId = fixtures.correction(contractId, "2031-06", "-50.00");

        mockMvc.perform(get("/api/v1/invoices/{id}", correctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("CORRECTION"))
                .andExpect(jsonPath("$.total").value("-50.00"));

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", correctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", correctionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:invoice-invalid-transition"));
    }

    @Test
    @DisplayName("BR-05: нулевая корректировка не имеет смысла")
    void zeroCorrectionIsRejected() throws Exception {
        String contractId = fixtures.contract("2031-01-01", "2031-12-31");

        mockMvc.perform(post("/api/v1/invoices/corrections")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"contractId": "%s", "period": "2031-06", "amount": "0.00"}
                                """.formatted(contractId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:invalid-value"));
    }

    private String startRun(String period) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/billing-runs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"period\": \"%s\"}".formatted(period)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    /** Расчёт идёт в фоне: ждём окончания, не дольше 30 секунд. Возвращает тело ответа о запуске. */
    private String awaitCompleted(String runId) throws Exception {
        for (int attempt = 0; attempt < 300; attempt++) {
            String body = mockMvc.perform(get("/api/v1/billing-runs/{id}", runId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (!"RUNNING".equals(JsonPath.<String>read(body, "$.status"))) {
                assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("COMPLETED");
                return body;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Выставление счетов не завершилось за 30 секунд");
    }
}
