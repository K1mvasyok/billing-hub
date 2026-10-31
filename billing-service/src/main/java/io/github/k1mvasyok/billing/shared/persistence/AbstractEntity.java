package io.github.k1mvasyok.billing.shared.persistence;

import io.github.k1mvasyok.billing.shared.id.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Общие поля всех таблиц: UUID v7, даты создания и изменения, версия для оптимистичной блокировки.
 *
 * <p>Id назначаем в коде, а не базой: id известен до сохранения, его можно вернуть клиенту
 * и использовать в событиях outbox в той же транзакции.
 */
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UuidV7.next();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * null у новой сущности: Spring Data по null понимает, что это insert, а не merge.
     * Hibernate сам выставит 0 при первом сохранении.
     */
    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
