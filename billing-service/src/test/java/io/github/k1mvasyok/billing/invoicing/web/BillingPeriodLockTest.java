package io.github.k1mvasyok.billing.invoicing.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.TestClockConfiguration;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BR-05: закрытие периода блокирует все периоды до него включительно.
 * Закрытый месяц влияет на соседние тесты, поэтому контекст (и база) не переиспользуются после класса.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillingPeriodLockTest {

    @Autowired
    private MockMvc mockMvc;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    @Test
    @DisplayName("BR-05: закрытый период и всё раньше него нельзя менять, закрыть дважды нельзя")
    void closedPeriodLocksItselfAndEarlierPeriods() throws Exception {
        String contractId = fixtures.contract("2040-01-01", "2041-01-01");
        String inMarch = fixtures.correction(contractId, "2040-03", "10.00");
        String inFebruary = fixtures.correction(contractId, "2040-02", "10.00");

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2040-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.period").value("2040-03-01"));

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2040-03"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-already-closed"));

        mockMvc.perform(post("/api/v1/billing-runs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"period\": \"2040-03\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-closed"));

        mockMvc.perform(post("/api/v1/invoices/corrections")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"contractId": "%s", "period": "2040-02", "amount": "5.00"}
                                """.formatted(contractId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-closed"));

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", inMarch))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-closed"));

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", inFebruary))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-closed"));
    }

    @Test
    @DisplayName("Неверный формат периода — 400, а не 500")
    void badPeriodFormatIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2040-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:invalid-value"));
    }
}
