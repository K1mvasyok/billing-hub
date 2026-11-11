package io.github.k1mvasyok.billing.contracts.domain;

import io.github.k1mvasyok.billing.shared.period.ValidityPeriod;

import io.github.k1mvasyok.billing.shared.error.BusinessRuleException;
import io.github.k1mvasyok.billing.shared.error.NotFoundException;
import io.github.k1mvasyok.billing.shared.persistence.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Договор клиента. Правила жизненного цикла живут здесь, а не в контроллере:
 * только DRAFT можно менять, активировать без позиций нельзя.
 */
@Entity
@Table(name = "contract")
@Audited
public class Contract extends AbstractEntity {

    /** Клиент из модуля customers. Храним только id, чтобы модули не делили сущности. */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, unique = true, length = 32)
    private String number;

    @Column(name = "signed_on", nullable = false)
    private LocalDate signedOn;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContractStatus status;

    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays;

    /** Дата активации. Пустая у черновика и у договора, расторгнутого до активации: такие договоры счетов не получают. */
    @Column(name = "activated_on")
    private LocalDate activatedOn;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<ContractItem> items = new ArrayList<>();

    protected Contract() {
    }

    public Contract(UUID customerId, String number, LocalDate signedOn,
                    ValidityPeriod period, int paymentTermDays) {
        if (paymentTermDays < 0) {
            throw new IllegalArgumentException("Срок оплаты не может быть отрицательным");
        }
        this.customerId = customerId;
        this.number = number;
        this.signedOn = signedOn;
        this.validFrom = period.from();
        this.validTo = period.to();
        this.paymentTermDays = paymentTermDays;
        this.status = ContractStatus.DRAFT;
    }

    public void activate(LocalDate activationDay) {
        requireTransition(ContractStatus.ACTIVE);
        if (items.isEmpty()) {
            throw new BusinessRuleException("contract-has-no-items",
                    "Нельзя активировать договор без позиций");
        }
        this.status = ContractStatus.ACTIVE;
        this.activatedOn = activationDay;
    }

    /**
     * Расторжение с даты effectiveDate. Эта дата — первый день, за который договор уже не действует.
     * Договор и позиции обрезаются по ней; позиции, которые начинаются не раньше неё, удаляются.
     */
    public void terminate(LocalDate effectiveDate) {
        requireTransition(ContractStatus.TERMINATED);
        requireTerminationDate(effectiveDate);
        this.status = ContractStatus.TERMINATED;
        this.validTo = effectiveDate;
        items.removeIf(item -> !item.getValidFrom().isBefore(effectiveDate));
        items.forEach(item -> item.endBefore(effectiveDate));
    }

    public ContractItem addItem(Plan plan, int quantity, ValidityPeriod period) {
        requireDraft();
        requireWithinContract(period);
        ContractItem item = new ContractItem(this, plan, quantity, period);
        items.add(item);
        return item;
    }

    public ContractItem updateItem(UUID itemId, int quantity, ValidityPeriod period) {
        requireDraft();
        requireWithinContract(period);
        ContractItem item = items.stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Позиция", itemId));
        item.update(quantity, period);
        return item;
    }

    public ValidityPeriod period() {
        return new ValidityPeriod(validFrom, validTo);
    }

    private void requireTransition(ContractStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("contract-invalid-transition",
                    "Переход " + status + " → " + target + " запрещён");
        }
    }

    private void requireDraft() {
        if (status != ContractStatus.DRAFT) {
            throw new BusinessRuleException("contract-not-draft",
                    "Позиции можно менять только в черновике договора");
        }
    }

    /** Дата расторжения должна лежать в срок договора: позже начала и не позже окончания (конец не включается). */
    private void requireTerminationDate(LocalDate effectiveDate) {
        boolean afterStart = effectiveDate.isAfter(validFrom);
        boolean beforeEnd = validTo == null || !effectiveDate.isAfter(validTo);
        if (!afterStart || !beforeEnd) {
            throw new BusinessRuleException("contract-termination-outside-term",
                    "Дата расторжения должна быть позже начала договора и не позже его окончания");
        }
    }

    private void requireWithinContract(ValidityPeriod itemPeriod) {
        if (!itemPeriod.isWithin(period())) {
            throw new BusinessRuleException("contract-item-outside-contract",
                    "Период позиции должен лежать внутри периода договора");
        }
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getSignedOn() {
        return signedOn;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public int getPaymentTermDays() {
        return paymentTermDays;
    }

    public LocalDate getActivatedOn() {
        return activatedOn;
    }

    public List<ContractItem> getItems() {
        return items;
    }
}
