package io.github.k1mvasyok.billing.shared.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Генератор UUID версии 7 (RFC 9562): первые 48 бит — время в миллисекундах,
 * поэтому новые идентификаторы сортируются по времени создания. Это лучше для индексов B-tree,
 * чем случайные UUID v4: новые записи не разбрасываются по всему индексу.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT_RFC_4122 = 0x8000000000000000L;

    private UuidV7() {
    }

    public static UUID next() {
        return at(System.currentTimeMillis());
    }

    /** Отдельный метод с временем нужен, чтобы тесты могли проверить порядок и биты. */
    public static UUID at(long epochMillis) {
        // Старшие 48 бит msb — временная метка, следующие 4 бита — версия 7, затем 12 случайных бит.
        long msb = ((epochMillis & 0xFFFFFFFFFFFFL) << 16) | VERSION_7 | (RANDOM.nextInt() & 0x0FFF);
        // Старшие 2 бита lsb — вариант RFC 4122 (10), остальные 62 бита случайные.
        long lsb = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | VARIANT_RFC_4122;
        return new UUID(msb, lsb);
    }

    public static long timestampOf(UUID id) {
        return id.getMostSignificantBits() >>> 16;
    }
}
