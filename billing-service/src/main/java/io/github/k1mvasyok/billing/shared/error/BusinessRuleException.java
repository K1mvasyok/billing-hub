package io.github.k1mvasyok.billing.shared.error;

/**
 * Нарушено бизнес-правило (например, недопустимый переход статуса договора).
 * {@code code} попадает в тип ошибки ProblemDetail, по нему клиент различает причины.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
