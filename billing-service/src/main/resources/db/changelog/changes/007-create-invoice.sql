--liquibase formatted sql

--changeset billing:007-create-invoice
-- Счёт. Обычный счёт (REGULAR) на договор и период — один, пока он не отменён (BR-04).
-- Корректировка (CORRECTION) может быть отрицательной, поэтому total не ограничиваем.
CREATE TABLE invoice (
    id            uuid          PRIMARY KEY,
    number        varchar(32)   NOT NULL UNIQUE,
    contract_id   uuid          NOT NULL REFERENCES contract (id),
    customer_id   uuid          NOT NULL REFERENCES customer (id),
    period        date          NOT NULL,
    kind          varchar(16)   NOT NULL CHECK (kind IN ('REGULAR', 'CORRECTION')),
    issue_date    date          NOT NULL,
    due_date      date          NOT NULL,
    status        varchar(16)   NOT NULL CHECK (status IN ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')),
    total         numeric(19,2) NOT NULL,
    paid          numeric(19,2) NOT NULL DEFAULT 0 CHECK (paid >= 0),
    accounting_ref varchar(64),
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    version       bigint        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_invoice_regular_contract_period
    ON invoice (contract_id, period)
    WHERE kind = 'REGULAR' AND status <> 'CANCELLED';

CREATE INDEX ix_invoice_customer_due
    ON invoice (customer_id, due_date)
    WHERE status IN ('ISSUED', 'PARTIALLY_PAID');
