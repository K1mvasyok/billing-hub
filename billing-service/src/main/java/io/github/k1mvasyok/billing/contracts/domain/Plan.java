package io.github.k1mvasyok.billing.contracts.domain;

import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Тарифный план. Цена хранится отдельно по периодам в {@link PlanPrice}. */
@Entity
@Table(name = "plan")
public class Plan extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    protected Plan() {
    }

    public Plan(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
