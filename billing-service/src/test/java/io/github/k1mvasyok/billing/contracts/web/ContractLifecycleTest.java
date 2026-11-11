package io.github.k1mvasyok.billing.contracts.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import io.github.k1mvasyok.billing.contracts.domain.Contract;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий через HTTP на настоящем PostgreSQL: клиент → план → цена → договор → позиция → статусы.
 * Данные в одной базе на все тесты, поэтому каждый тест берёт уникальные номера.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContractLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void contractGoesFromDraftToActiveToTerminated() throws Exception {
        String planId = createPlanWithPrice();
        String contractId = createContract(createCustomer(), unique("CTR"));
        addItem(contractId, planId, "2026-01-01", "2026-12-31");

        mockMvc.perform(post("/api/v1/contracts/{id}/activate", contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(terminate(contractId, "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    @DisplayName("расторжение без даты отклоняется")
    void terminationWithoutDateIsRejected() throws Exception {
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(post("/api/v1/contracts/{id}/terminate", contractId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("дата расторжения в день начала договора отклоняется с кодом contract-termination-outside-term")
    void terminationOnContractStartIsRejected() throws Exception {
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(terminate(contractId, "2026-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:contract-termination-outside-term"));
    }

    @Test
    @DisplayName("дата расторжения после окончания договора отклоняется с кодом contract-termination-outside-term")
    void terminationAfterContractEndIsRejected() throws Exception {
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(terminate(contractId, "2027-01-02"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:contract-termination-outside-term"));
    }

    @Test
    void contractListWorksWithoutFilters() throws Exception {
        createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(get("/api/v1/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void activationWithoutItemsIsRejected() throws Exception {
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(post("/api/v1/contracts/{id}/activate", contractId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:contract-has-no-items"));
    }

    @Test
    void itemOutsideContractPeriodIsRejected() throws Exception {
        String planId = createPlan(unique("PLAN"));
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(post("/api/v1/contracts/{id}/items", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planId": "%s", "quantity": 1, "validFrom": "2027-01-01", "validTo": "2027-02-01"}
                                """.formatted(planId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:contract-item-outside-contract"));
    }

    @Test
    void itemOfPlanWithoutPriceIsRejected() throws Exception {
        String planId = createPlan(unique("PLAN"));
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(post("/api/v1/contracts/{id}/items", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planId": "%s", "quantity": 1, "validFrom": "2026-01-01", "validTo": "2026-12-31"}
                                """.formatted(planId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:plan-price-missing"));
    }

    @Test
    void itemPeriodWithPriceGapIsRejected() throws Exception {
        String planId = createPlan(unique("PLAN"));
        addPrice(planId, "2026-01-01", "2026-07-01", "100.00");
        addPrice(planId, "2026-08-01", "2027-01-01", "120.00");
        String contractId = createContract(createCustomer(), unique("CTR"));

        mockMvc.perform(post("/api/v1/contracts/{id}/items", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planId": "%s", "quantity": 1, "validFrom": "2026-01-01", "validTo": "2026-12-31"}
                                """.formatted(planId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:plan-price-missing"));
    }

    @Test
    void itemCannotBeChangedAfterActivation() throws Exception {
        String planId = createPlanWithPrice();
        String contractId = createContract(createCustomer(), unique("CTR"));
        String itemId = addItem(contractId, planId, "2026-01-01", "2026-12-31");
        mockMvc.perform(post("/api/v1/contracts/{id}/activate", contractId)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/contracts/{id}/items/{itemId}", contractId, itemId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 5, "validFrom": "2026-01-01", "validTo": "2026-12-31"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:contract-not-draft"));
    }

    @Test
    void overlappingPriceIsRejectedWithPlanPriceOverlapCode() throws Exception {
        String planId = createPlan(unique("PLAN"));
        addPrice(planId, "2026-01-01", "2026-07-01", "100.00");

        mockMvc.perform(post("/api/v1/plans/{id}/prices", planId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"validFrom": "2026-06-01", "validTo": "2026-08-01", "monthlyPrice": "120.00"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:plan-price-overlap"));
    }

    @Test
    void duplicateActiveTaxIdIsRejected() throws Exception {
        String taxId = newTaxId();
        createCustomerWithTaxId(taxId);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type": "ORG", "name": "Дубликат", "taxId": "%s"}
                                """.formatted(taxId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:customer-tax-id-exists"));
    }

    @Test
    void enversRecordsEveryRevisionOfTheContract() throws Exception {
        String planId = createPlanWithPrice();
        String contractId = createContract(createCustomer(), unique("CTR"));
        addItem(contractId, planId, "2026-01-01", "2026-12-31");
        mockMvc.perform(post("/api/v1/contracts/{id}/activate", contractId)).andExpect(status().isOk());

        int revisions = transactions.execute(state -> AuditReaderFactory.get(entityManager)
                .getRevisions(Contract.class, UUID.fromString(contractId)).size());

        // Создание договора и активация — две отдельные ревизии.
        assertThat(revisions).isGreaterThanOrEqualTo(2);
    }

    // --- вспомогательные методы: создают данные через HTTP, как это сделал бы клиент API ---

    private String createCustomer() throws Exception {
        return createCustomerWithTaxId(newTaxId());
    }

    /** ИНН организации — ровно 10 цифр, дополняем нулями слева. */
    private static String newTaxId() {
        return String.format("%010d", Math.floorMod(System.nanoTime(), 10_000_000_000L));
    }

    private String createCustomerWithTaxId(String taxId) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/customers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type": "ORG", "name": "Тестовый клиент", "taxId": "%s"}
                                """.formatted(taxId)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String createPlan(String code) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/plans")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "Тестовый план"}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    /** План с бессрочной ценой, покрывающей любой период в тестах договоров 2026 года. */
    private String createPlanWithPrice() throws Exception {
        String planId = createPlan(unique("PLAN"));
        addPrice(planId, "2025-01-01", "2030-01-01", "1500.00");
        return planId;
    }

    private void addPrice(String planId, String from, String to, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/plans/{id}/prices", planId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"validFrom": "%s", "validTo": "%s", "monthlyPrice": "%s"}
                                """.formatted(from, to, amount)))
                .andExpect(status().isCreated());
    }

    private String createContract(String customerId, String number) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/contracts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "number": "%s", "signedOn": "2026-01-01",
                                 "validFrom": "2026-01-01", "validTo": "2026-12-31", "paymentTermDays": 14}
                                """.formatted(customerId, number)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String addItem(String contractId, String planId, String from, String to) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/contracts/{id}/items", contractId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"planId": "%s", "quantity": 2, "validFrom": "%s", "validTo": "%s"}
                                """.formatted(planId, from, to)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private static MockHttpServletRequestBuilder terminate(String contractId, String effectiveDate) {
        return post("/api/v1/contracts/{id}/terminate", contractId)
                .contentType(APPLICATION_JSON)
                .content("{\"effectiveDate\": \"%s\"}".formatted(effectiveDate));
    }

    private static String idOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }
}
