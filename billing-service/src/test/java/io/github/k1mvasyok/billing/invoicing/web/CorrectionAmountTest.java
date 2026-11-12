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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BR-01: сумма корректировки принимает не больше копеек, ответ совпадает с сохранённым значением. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorrectionAmountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    @Test
    @DisplayName("BR-01: корректировка на 0.001 отклоняется (400), а не сохраняется как нулевая")
    void tinyCorrectionIsRejected() throws Exception {
        assertRejected("0.001");
    }

    @Test
    @DisplayName("BR-01: корректировка с третьим знаком после запятой отклоняется (400)")
    void correctionWithThirdDecimalIsRejected() throws Exception {
        assertRejected("10.005");
        assertRejected("-0.001");
    }

    @Test
    @DisplayName("BR-01: корректировка с одним знаком сохраняется и возвращается с двумя знаками")
    void correctionWithOneDecimalIsStoredWithTwo() throws Exception {
        String contractId = fixtures.contract("2031-01-01", "2032-01-01");

        String body = mockMvc.perform(post("/api/v1/invoices/corrections")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"contractId": "%s", "period": "2031-06", "amount": "10.5"}
                                """.formatted(contractId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value("10.50"))
                .andReturn().getResponse().getContentAsString();
        String invoiceId = JsonPath.read(body, "$.id");

        BigDecimal stored = jdbc.queryForObject("select total from invoice where id = ?::uuid",
                BigDecimal.class, invoiceId);
        assertThat(stored.toPlainString()).isEqualTo("10.50");
    }

    private void assertRejected(String amount) throws Exception {
        String contractId = fixtures.contract("2031-01-01", "2032-01-01");
        mockMvc.perform(post("/api/v1/invoices/corrections")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"contractId": "%s", "period": "2031-06", "amount": "%s"}
                                """.formatted(contractId, amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:invalid-value"));
    }
}
