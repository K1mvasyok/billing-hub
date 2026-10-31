package io.github.k1mvasyok.billing.shared.web;

import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Единая точка превращения исключений в ProblemDetail (RFC 9457).
 * Ошибки валидации полей (400) отдаёт сам Spring, если включён spring.mvc.problemdetails.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String TYPE_PREFIX = "urn:billing-hub:problem:";

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException ex) {
        return problem(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidValue(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-value", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "concurrent-update",
                "Запись изменили параллельно. Загрузите её заново и повторите");
    }

    /**
     * Нарушение ограничения БД. Для известных ограничений даём понятный код, остальные — общий.
     * Имя ограничения приходит в сообщении драйвера PostgreSQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail constraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("ex_plan_price_no_overlap")) {
            return problem(HttpStatus.CONFLICT, "plan-price-overlap",
                    "Период цены пересекается с уже существующим периодом этого плана");
        }
        return problem(HttpStatus.CONFLICT, "constraint-violation",
                "Нарушено ограничение данных: проверьте уникальность и связанные записи");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
