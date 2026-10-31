package io.github.k1mvasyok.billing.shared.id;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7Test {

    @Test
    void generatedIdHasVersion7() {
        assertThat(UuidV7.next().version()).isEqualTo(7);
    }

    @Test
    void generatedIdHasRfc4122Variant() {
        assertThat(UuidV7.next().variant()).isEqualTo(2);
    }

    @Test
    void timestampIsStoredInTheHighBits() {
        UUID id = UuidV7.at(1_700_000_000_123L);

        assertThat(UuidV7.timestampOf(id)).isEqualTo(1_700_000_000_123L);
    }

    @Test
    void idsGeneratedLaterSortAfterEarlierOnes() {
        UUID earlier = UuidV7.at(1_000L);
        UUID later = UuidV7.at(2_000L);

        assertThat(earlier.compareTo(later)).isNegative();
    }
}
