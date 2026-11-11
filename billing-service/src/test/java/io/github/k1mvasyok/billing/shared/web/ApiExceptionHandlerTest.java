package io.github.k1mvasyok.billing.shared.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    @DisplayName("BR-04: нарушение индекса идущих запусков — 409 billing-run-in-progress, а не общая ошибка")
    void runningIndexViolationBecomesBillingRunInProgress() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"ux_billing_run_running_period\""));

        ProblemDetail problem = new ApiExceptionHandler().constraintViolation(ex);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getType()).hasToString("urn:billing-hub:problem:billing-run-in-progress");
    }
}
