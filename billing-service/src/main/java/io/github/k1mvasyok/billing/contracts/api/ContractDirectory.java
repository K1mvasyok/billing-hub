package io.github.k1mvasyok.billing.contracts.api;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Публичный интерфейс договоров для модуля invoicing: данные для расчёта, без сущностей. */
public interface ContractDirectory {

    /**
     * Договоры, действующие в месяце, по возрастанию id, страницами.
     *
     * @param afterId последний id предыдущей страницы или null для первой
     * @param limit   размер страницы
     */
    List<BillableContract> findBillable(YearMonth month, UUID afterId, int limit);

    /** Договор для выставления корректировки. Бросает NotFoundException, если договора нет. */
    ContractRef requireContract(UUID contractId);
}
