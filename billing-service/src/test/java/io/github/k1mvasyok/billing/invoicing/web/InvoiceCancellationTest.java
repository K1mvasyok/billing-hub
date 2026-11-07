package io.github.k1mvasyok.billing.invoicing.web;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BR-06: отмена возможна только из ISSUED и только в открытом периоде.
 * Тест закрывает период, поэтому контекст не переиспользуется после класса (как в BillingPeriodLockTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InvoiceCancellationTest {

    @Autowired
    private MockMvc mockMvc;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    @Test
    @DisplayName("BR-06: ISSUED → CANCELLED в открытом периоде один раз; повтор и отмена в закрытом периоде запрещены")
    void issuedInvoiceCancelsOnceAndOnlyInOpenPeriod() throws Exception {
        String contractId = fixtures.contract("2047-01-01", "2047-12-31");
        String cancelledOnce = fixtures.correction(contractId, "2047-01", "10.00");
        String lockedLater = fixtures.correction(contractId, "2047-02", "10.00");

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", cancelledOnce))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", cancelledOnce))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:invoice-invalid-transition"));

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2047-02"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", lockedLater))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-closed"));
    }
}
