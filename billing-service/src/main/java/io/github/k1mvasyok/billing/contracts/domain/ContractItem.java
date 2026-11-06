package io.github.k1mvasyok.billing.contracts.domain;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;

import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDate;

/** Позиция договора: план × количество на собственный период. Меняется только через {@link Contract}. */
@Entity
@Table(name = "contract_item")
@Audited
public class ContractItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Plan plan;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    protected ContractItem() {
    }

    ContractItem(Contract contract, Plan plan, int quantity, ValidityPeriod period) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше нуля");
        }
        this.contract = contract;
        this.plan = plan;
        this.quantity = quantity;
        this.validFrom = period.from();
        this.validTo = period.to();
    }

    void update(int quantity, ValidityPeriod period) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше нуля");
        }
        this.quantity = quantity;
        this.validFrom = period.from();
        this.validTo = period.to();
    }

    public ValidityPeriod period() {
        return new ValidityPeriod(validFrom, validTo);
    }

    public Plan getPlan() {
        return plan;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }
}
