package io.github.k1mvasyok.billing.contracts.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContractStatusTest {

    @ParameterizedTest
    @CsvSource({
            "DRAFT, ACTIVE, true",
            "DRAFT, TERMINATED, true",
            "ACTIVE, TERMINATED, true",
            "ACTIVE, DRAFT, false",
            "ACTIVE, ACTIVE, false",
            "TERMINATED, ACTIVE, false",
            "TERMINATED, DRAFT, false",
            "DRAFT, DRAFT, false"
    })
    void allowsOnlyForwardTransitions(ContractStatus from, ContractStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}
