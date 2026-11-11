package io.github.k1mvasyok.billing.invoicing.web;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Данные для сценариев выставления счетов, создаются через HTTP, как это делал бы клиент API. */
final class BillingFixtures {

    private final MockMvc mockMvc;

    BillingFixtures(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** Активный договор с одной позицией: план с бессрочной ценой 1500.00, позиция как передано. */
    String activeContractWithItem(String validFrom, String validTo, String itemFrom, String itemTo,
                                  int quantity) throws Exception {
        String planId = createPlan();
        addPrice(planId, "2025-01-01", null, "1500.00");
        String contractId = createContract(createCustomer(), validFrom, validTo);
        mockMvc.perform(post("/api/v1/contracts/{id}/items", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planId": "%s", "quantity": %d, "validFrom": "%s", "validTo": "%s"}
                                """.formatted(planId, quantity, itemFrom, itemTo)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/contracts/{id}/activate", contractId)).andExpect(status().isOk());
        return contractId;
    }

    /** Договор без позиций: для корректировок. */
    String contract(String validFrom, String validTo) throws Exception {
        return createContract(createCustomer(), validFrom, validTo);
    }

    /** План с бессрочной ценой 1500.00 для сценариев, где договор собирается по шагам. */
    String pricedPlan() throws Exception {
        String planId = createPlan();
        addPrice(planId, "2025-01-01", null, "1500.00");
        return planId;
    }

    void addItem(String contractId, String planId, int quantity, String from, String to) throws Exception {
        mockMvc.perform(post("/api/v1/contracts/{id}/items", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planId": "%s", "quantity": %d, "validFrom": "%s", "validTo": "%s"}
                                """.formatted(planId, quantity, from, to)))
                .andExpect(status().isCreated());
    }

    void activate(String contractId) throws Exception {
        mockMvc.perform(post("/api/v1/contracts/{id}/activate", contractId)).andExpect(status().isOk());
    }

    void terminate(String contractId, String effectiveDate) throws Exception {
        mockMvc.perform(post("/api/v1/contracts/{id}/terminate", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"effectiveDate\": \"%s\"}".formatted(effectiveDate)))
                .andExpect(status().isOk());
    }

    String correction(String contractId, String period, String amount) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/invoices/corrections")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"contractId": "%s", "period": "%s", "amount": "%s"}
                                """.formatted(contractId, period, amount)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    String createCustomer() throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/customers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type": "ORG", "name": "Клиент для счетов", "taxId": "%s"}
                                """.formatted(newTaxId())))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String createContract(String customerId, String validFrom, String validTo) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/contracts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "number": "C-%d", "signedOn": "%s",
                                 "validFrom": "%s", "validTo": "%s", "paymentTermDays": 14}
                                """.formatted(customerId, System.nanoTime(), validFrom, validFrom, validTo)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String createPlan() throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/plans")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"code": "P_%d", "name": "Тестовый план"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void addPrice(String planId, String from, String to, String amount) throws Exception {
        String validTo = to == null ? "null" : "\"" + to + "\"";
        mockMvc.perform(post("/api/v1/plans/{id}/prices", planId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"validFrom": "%s", "validTo": %s, "monthlyPrice": "%s"}
                                """.formatted(from, validTo, amount)))
                .andExpect(status().isCreated());
    }

    /** ИНН организации — ровно 10 цифр. */
    private static String newTaxId() {
        return String.format("%010d", Math.floorMod(System.nanoTime(), 10_000_000_000L));
    }

    static String idOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
