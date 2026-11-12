package io.github.k1mvasyok.billing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Часы для тестов: время можно переводить. По умолчанию — 1 января 2060 года, позже всех периодов в тестах,
 * поэтому существующие сценарии выставления видят свои месяцы прошедшими.
 */
public final class MutableClock extends Clock {

    static final LocalDate DEFAULT_DAY = LocalDate.of(2060, 1, 1);

    private static final ZoneId ZONE = ZoneOffset.UTC;

    private volatile Instant now = DEFAULT_DAY.atStartOfDay(ZONE).toInstant();

    /** Переводит часы на начало указанного дня. */
    public void setDate(LocalDate day) {
        this.now = day.atStartOfDay(ZONE).toInstant();
    }

    /** Возвращает часы к значению по умолчанию. */
    public void reset() {
        setDate(DEFAULT_DAY);
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
