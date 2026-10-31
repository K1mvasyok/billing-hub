package io.github.k1mvasyok.billing.customers.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class TaxIdTest {

    @ParameterizedTest
    @CsvSource({"ORG, 1234567890", "IE, 123456789012"})
    void acceptsCorrectLengthForCustomerType(CustomerType type, String taxId) {
        assertThatCode(() -> TaxId.requireValid(type, taxId)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"ORG, 123456789012", "ORG, 123", "IE, 1234567890"})
    void rejectsWrongLengthForCustomerType(CustomerType type, String taxId) {
        assertThatThrownBy(() -> TaxId.requireValid(type, taxId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345abcde", "123 4567890", "1234567890-"})
    void rejectsNonDigits(String taxId) {
        assertThatThrownBy(() -> TaxId.requireValid(CustomerType.ORG, taxId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingValue(String taxId) {
        assertThatThrownBy(() -> TaxId.requireValid(CustomerType.ORG, taxId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
