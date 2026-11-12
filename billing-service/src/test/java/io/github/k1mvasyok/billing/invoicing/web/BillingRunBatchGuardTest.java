package io.github.k1mvasyok.billing.invoicing.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.TestClockConfiguration;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import io.github.k1mvasyok.billing.contracts.api.ContractDirectory;
import io.github.k1mvasyok.billing.invoicing.app.BillingPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Пачка по одному договору: два договора — две пачки. Между ними период закрывают,
 * и вторая пачка не должна писать счета в закрытый период.
 */
@SpringBootTest(properties = "billing.run.batch-size=1")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillingRunBatchGuardTest {

    private static final String PERIOD = "2050-06";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingPeriodService periods;

    @MockitoSpyBean
    private ContractDirectory contracts;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    @Test
    @DisplayName("BR-05: период закрыт между пачками — запуск FAILED, вторая пачка ничего не пишет")
    void periodClosedBetweenBatchesFailsRunAndWritesNothingAfter() throws Exception {
        String first = fixtures.activeContractWithItem("2050-01-01", "2050-12-31", "2050-01-01", "2050-12-31", 1);
        String second = fixtures.activeContractWithItem("2050-01-01", "2050-12-31", "2050-01-01", "2050-12-31", 1);

        doAnswer(invocation -> {
            // Второй вызов поиска идёт после первой пачки: период закрывают прямо перед второй пачкой.
            if (invocation.getArgument(1) != null) {
                periods.close(PERIOD);
            }
            return invocation.callRealMethod();
        }).when(contracts).findBillable(any(), any(), anyInt());

        String runId = JsonPath.read(mockMvc.perform(post("/api/v1/billing-runs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"period\": \"%s\"}".formatted(PERIOD)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.id");

        assertThat(awaitFinishedStatus(runId)).isEqualTo("FAILED");
        // Ровно один договор выставлен до закрытия; второй остался без счёта.
        int invoicesInPeriod = invoicesCount(first) + invoicesCount(second);
        assertThat(invoicesInPeriod).isEqualTo(1);
    }

    private String awaitFinishedStatus(String runId) throws Exception {
        for (int attempt = 0; attempt < 300; attempt++) {
            String body = mockMvc.perform(get("/api/v1/billing-runs/{id}", runId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String current = JsonPath.read(body, "$.status");
            if (!"RUNNING".equals(current)) {
                return current;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Запуск не завершился за 30 секунд");
    }

    private int invoicesCount(String contractId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/invoices")
                        .param("contractId", contractId)
                        .param("period", PERIOD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.content.length()");
    }
}
