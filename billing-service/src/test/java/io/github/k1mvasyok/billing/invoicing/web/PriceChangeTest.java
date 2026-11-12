package io.github.k1mvasyok.billing.invoicing.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.MutableClock;
import io.github.k1mvasyok.billing.TestClockConfiguration;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Смена цены тарифа с открытым концом. Смена возможна только с первого числа текущего месяца
 * и только открытой новой ценой. Часы — 1 декабря 2026 года, для прогона часы переводятся на февраль 2027-го.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PriceChangeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
        clock.setDate(LocalDate.of(2026, 12, 1));
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    @DisplayName("смена цены закрывает старую цену на дату новой, обе видны в списке цен")
    void changeClosesOldPriceAtNewStart() throws Exception {
        String planId = fixtures.pricedPlan();

        addPrice(planId, "2027-01-01", null, "1800.00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/plans/{id}/prices", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].validTo").value("2027-01-01"))
                .andExpect(jsonPath("$[1].validFrom").value("2027-01-01"))
                .andExpect(jsonPath("$[1].validTo").value(nullValue()));
    }

    @Test
    @DisplayName("BR-03: смена цены внутри месяца делит счёт на две строки: 15 дней по старой цене и 16 по новой")
    void priceChangeInsideMonthBillsTwoLines() throws Exception {
        String planId = fixtures.pricedPlan();
        String contractId = fixtures.contract("2026-01-01", "2028-01-01");
        fixtures.addItem(contractId, planId, 1, "2026-01-01", "2028-01-01");
        fixtures.activate(contractId);
        addPrice(planId, "2027-01-16", null, "1800.00")
                .andExpect(status().isCreated());

        // 1–15 января: 1500 × 15 / 31 = 725.81; 16–31 января: 1800 × 16 / 31 = 929.03
        clock.setDate(LocalDate.of(2027, 2, 1));
        awaitFinished(startRun("2027-01"));

        String invoiceId = JsonPath.read(mockMvc.perform(get("/api/v1/invoices")
                                .param("contractId", contractId).param("period", "2027-01"))
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andReturn().getResponse().getContentAsString(),
                "$.content[0].id");
        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(jsonPath("$.total").value("1654.84"))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    @DisplayName("смена цены задним числом, раньше первого числа текущего месяца, отклоняется (409)")
    void backdatedChangeIsRejected() throws Exception {
        String planId = fixtures.pricedPlan();

        addPrice(planId, "2026-11-01", null, "1800.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:plan-price-change-in-past"));
    }

    @Test
    @DisplayName("новая цена при смене должна быть открытой: с датой окончания отклоняется (409)")
    void boundedChangeIsRejected() throws Exception {
        String planId = fixtures.pricedPlan();

        addPrice(planId, "2027-01-01", "2027-06-01", "1800.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:plan-price-change-must-be-open"));
    }

    @Test
    @DisplayName("цена с той же датой начала, что у открытой, отклоняется как пересечение (409)")
    void changeWithSameStartIsOverlap() throws Exception {
        String planId = fixtures.pricedPlan();

        addPrice(planId, "2025-01-01", null, "1800.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:plan-price-overlap"));
    }

    @Test
    @DisplayName("список цен плана отдаётся по дате начала, для неизвестного плана — 404")
    void pricesAreListedByStartDate() throws Exception {
        String planId = fixtures.pricedPlan();
        addPrice(planId, "2027-01-01", null, "1800.00").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/plans/{id}/prices", planId))
                .andExpect(jsonPath("$[0].validFrom").value("2025-01-01"))
                .andExpect(jsonPath("$[1].validFrom").value("2027-01-01"));
        mockMvc.perform(get("/api/v1/plans/{id}/prices", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions addPrice(String planId, String from, String to,
                                                                        String amount) throws Exception {
        String validTo = to == null ? "null" : "\"" + to + "\"";
        return mockMvc.perform(post("/api/v1/plans/{id}/prices", planId)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"validFrom": "%s", "validTo": %s, "monthlyPrice": "%s"}
                        """.formatted(from, validTo, amount)));
    }

    private String startRun(String period) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/billing-runs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"period\": \"%s\"}".formatted(period)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void awaitFinished(String runId) throws Exception {
        for (int attempt = 0; attempt < 300; attempt++) {
            String body = mockMvc.perform(get("/api/v1/billing-runs/{id}", runId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (!"RUNNING".equals(JsonPath.<String>read(body, "$.status"))) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Выставление счетов не завершилось за 30 секунд");
    }
}
