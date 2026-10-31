package io.github.k1mvasyok.billing.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Своя запись ревизии Envers. Так имена таблицы и колонок совпадают со схемой Liquibase (миграция 005),
 * а не с именами по умолчанию Envers.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity
public class RevisionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @SequenceGenerator(name = "revinfo_seq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    private int rev;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private long revtstmp;

    public int getRev() {
        return rev;
    }

    public long getRevtstmp() {
        return revtstmp;
    }
}
