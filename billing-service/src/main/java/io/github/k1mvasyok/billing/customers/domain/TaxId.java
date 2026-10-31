package io.github.k1mvasyok.billing.customers.domain;

/**
 * Проверка формата ИНН. Контрольную сумму не считаем: данные синтетические (см. допущения в плане M1).
 */
public final class TaxId {

    private TaxId() {
    }

    public static void requireValid(CustomerType type, String taxId) {
        int expectedLength = switch (type) {
            case ORG -> 10;
            case IE -> 12;
        };
        // Character::isDigit пропустил бы и не-ASCII цифры, поэтому проверяем диапазон явно.
        if (taxId == null || taxId.length() != expectedLength || !taxId.matches("[0-9]+")) {
            throw new IllegalArgumentException(
                    "ИНН для типа " + type + " должен состоять из " + expectedLength + " цифр");
        }
    }
}
