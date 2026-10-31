package io.github.k1mvasyok.billing.shared.error;

/** Ресурс не найден. Отдаётся как 404 с ProblemDetail. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super(resource + " с id " + id + " не найден");
    }
}
