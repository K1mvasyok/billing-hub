package io.github.k1mvasyok.billing.contracts.domain;

import io.github.k1mvasyok.billing.shared.money.Money;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Цена плана за месяц на период. Пересечение периодов одного плана запрещает БД
 * (exclusion constraint в миграции 003). Здесь проверяем только то, что знает сама запись.
 */
@Entity
@Table(name = "plan_price")
@Audited
public class PlanPrice extends AbstractEntity {

    // План сам не аудируется (в аудите только цены), поэтому связь помечаем как NOT_AUDITED.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Plan plan;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "monthly_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPrice;

    protected PlanPrice() {
    }

    public PlanPrice(Plan plan, ValidityPeriod period, BigDecimal monthlyPrice) {
        if (monthlyPrice.signum() < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }
        this.plan = plan;
        this.validFrom = period.from();
        this.validTo = period.to();
        this.monthlyPrice = Money.toCents(monthlyPrice);
    }

    public ValidityPeriod period() {
        return new ValidityPeriod(validFrom, validTo);
    }

    /**
     * Закрывает открытую цену с указанного дня: он первый, по которому цена уже не действует.
     * Закрыть можно только открытую цену, и день должен быть позже её начала, иначе период станет пустым.
     */
    public void closeAt(LocalDate day) {
        if (validTo != null) {
            throw new IllegalArgumentException("Цена уже имеет дату окончания");
        }
        if (!day.isAfter(validFrom)) {
            throw new IllegalArgumentException("Цену нельзя закрыть днём не позже её начала");
        }
        this.validTo = day;
    }

    public Plan getPlan() {
        return plan;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }
}
