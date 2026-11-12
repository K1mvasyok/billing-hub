package io.github.k1mvasyok.billing.contracts.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BR-01: цена тарифа принимает не больше двух знаков, ответ совпадает с сохранённым значением. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PriceAmountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("BR-01: цена с третьим знаком после запятой отклоняется (400)")
    void priceWithThirdDecimalIsRejected() throws Exception {
        String planId = createPlan();

        mockMvc.perform(addPrice(planId, "1500.005"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:invalid-value"));
    }

    @Test
    @DisplayName("BR-01: цена с одним знаком сохраняется и возвращается с двумя знаками")
    void priceWithOneDecimalIsStoredWithTwo() throws Exception {
        String planId = createPlan();

        String body = mockMvc.perform(addPrice(planId, "1500.5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyPrice").value("1500.50"))
                .andReturn().getResponse().getContentAsString();
        String priceId = JsonPath.read(body, "$.id");

        BigDecimal stored = jdbc.queryForObject("select monthly_price from plan_price where id = ?::uuid",
                BigDecimal.class, priceId);
        assertThat(stored.toPlainString()).isEqualTo("1500.50");
    }

    @Test
    @DisplayName("BR-01: хвостовой ноль после третьего знака допустим, цена 1500.50")
    void trailingZeroBeyondCentIsAccepted() throws Exception {
        String planId = createPlan();

        mockMvc.perform(addPrice(planId, "1500.500"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyPrice").value("1500.50"));
    }

    private MockHttpServletRequestBuilder addPrice(String planId, String amount) {
        return post("/api/v1/plans/{id}/prices", planId)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"validFrom": "2025-01-01", "validTo": null, "monthlyPrice": "%s"}
                        """.formatted(amount));
    }

    private String createPlan() throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/plans")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\": \"AMT_%d\", \"name\": \"Тарифы с суммами\"}".formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }
}
