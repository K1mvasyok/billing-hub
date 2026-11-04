package io.github.k1mvasyok.billing.invoicing.infra;

import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Правила выставления счетов, которые держит схема БД. Идём на настоящем PostgreSQL:
 * частичные уникальные индексы H2 не поддерживает так же.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class InvoiceSchemaTest {

    private static final LocalDate NOVEMBER = LocalDate.of(2026, 11, 1);

    @Autowired
    private EntityManager entityManager;

    /** BR-04: один обычный счёт на договор и период. */
    @Test
    void rejectsSecondRegularInvoiceForSameContractAndPeriod() {
        UUID contractId = newContract();
        insertInvoice(contractId, NOVEMBER, "REGULAR", "ISSUED");

        assertThatThrownBy(() -> insertInvoice(contractId, NOVEMBER, "REGULAR", "ISSUED"))
                .hasStackTraceContaining("ux_invoice_regular_contract_period");
    }

    /** BR-04: отменённый обычный счёт не занимает слот, можно выставить новый. */
    @Test
    void allowsNewRegularInvoiceAfterPreviousWasCancelled() {
        UUID contractId = newContract();
        insertInvoice(contractId, NOVEMBER, "REGULAR", "CANCELLED");

        assertThatCode(() -> insertInvoice(contractId, NOVEMBER, "REGULAR", "ISSUED"))
                .doesNotThrowAnyException();
    }

    /** BR-05: исправления идут отдельными корректировочными счетами, рядом с обычным. */
    @Test
    void allowsCorrectionInTheSamePeriodAsRegularInvoice() {
        UUID contractId = newContract();
        insertInvoice(contractId, NOVEMBER, "REGULAR", "ISSUED");

        assertThatCode(() -> insertInvoice(contractId, NOVEMBER, "CORRECTION", "ISSUED"))
                .doesNotThrowAnyException();
    }

    /** Период закрытия — всегда первое число месяца. */
    @Test
    void rejectsBillingPeriodNotOnFirstDayOfMonth() {
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                        INSERT INTO billing_period (id, period, status, created_at, updated_at, version)
                        VALUES (:id, :period, 'OPEN', now(), now(), 0)
                        """)
                        .setParameter("id", UUID.randomUUID())
                        .setParameter("period", LocalDate.of(2026, 11, 15))
                        .executeUpdate())
                .hasStackTraceContaining("ck_billing_period_first_day");
    }

    private UUID newContract() {
        UUID customerId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO customer (id, type, name, tax_id, status, dunning_opt_out, created_at, updated_at, version)
                        VALUES (:id, 'ORG', 'Тестовый клиент', :taxId, 'ACTIVE', false, now(), now(), 0)
                        """)
                .setParameter("id", customerId)
                .setParameter("taxId", String.format("%010d", Math.floorMod(System.nanoTime(), 10_000_000_000L)))
                .executeUpdate();

        UUID contractId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO contract (id, customer_id, number, signed_on, valid_from, status, payment_term_days,
                                              created_at, updated_at, version)
                        VALUES (:id, :customerId, :number, '2026-01-01', '2026-01-01', 'ACTIVE', 14, now(), now(), 0)
                        """)
                .setParameter("id", contractId)
                .setParameter("customerId", customerId)
                .setParameter("number", "C-" + System.nanoTime())
                .executeUpdate();
        return contractId;
    }

    private void insertInvoice(UUID contractId, LocalDate period, String kind, String status) {
        entityManager.createNativeQuery("""
                        INSERT INTO invoice (id, number, contract_id, customer_id, period, kind, issue_date, due_date,
                                             status, total, paid, created_at, updated_at, version)
                        VALUES (:id, :number, :contractId,
                                (SELECT customer_id FROM contract WHERE id = :contractId),
                                :period, :kind, :period, :period, :status, 100.00, 0, now(), now(), 0)
                        """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("number", "INV-" + System.nanoTime())
                .setParameter("contractId", contractId)
                .setParameter("period", period)
                .setParameter("kind", kind)
                .setParameter("status", status)
                .executeUpdate();
    }
}
