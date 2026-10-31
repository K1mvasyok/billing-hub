package io.github.k1mvasyok.billing.customers.domain;

import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Клиент-организация или ИП. Пустой конструктор нужен Hibernate. */
@Entity
@Table(name = "customer")
public class Customer extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private CustomerType type;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_id", nullable = false, length = 12)
    private String taxId;

    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustomerStatus status;

    @Column(name = "dunning_opt_out", nullable = false)
    private boolean dunningOptOut;

    protected Customer() {
    }

    public Customer(CustomerType type, String name, String taxId, String email, String phone) {
        TaxId.requireValid(type, taxId);
        this.type = type;
        this.name = name;
        this.taxId = taxId;
        this.email = email;
        this.phone = phone;
        this.status = CustomerStatus.ACTIVE;
        this.dunningOptOut = false;
    }

    /** Меняем только контактные поля. Тип и ИНН после создания не меняются. */
    public void update(String name, String email, String phone) {
        if (name != null) {
            this.name = name;
        }
        if (email != null) {
            this.email = email;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public CustomerType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public boolean isDunningOptOut() {
        return dunningOptOut;
    }
}
