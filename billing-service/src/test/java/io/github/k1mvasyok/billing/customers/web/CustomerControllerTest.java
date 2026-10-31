package io.github.k1mvasyok.billing.customers.web;

import io.github.k1mvasyok.billing.customers.app.CustomerService;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/** Слой веб: валидация входа и ошибки в формате ProblemDetail. Сервис заменён заглушкой. */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService service;

    @Test
    void rejectsBlankNameWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type": "ORG", "name": " ", "taxId": "1234567890"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsInvalidEmailOnPatch() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/customers/{id}", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "не-почта"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundProblemForUnknownCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(any())).thenThrow(new NotFoundException("Клиент", id));

        mockMvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:not-found"));
    }
}
