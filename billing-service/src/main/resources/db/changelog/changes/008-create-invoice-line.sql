--liquibase formatted sql

--changeset billing:008-create-invoice-line
-- Строка счёта: план × количество за отрезок дат внутри периода (BR-02, BR-03).
-- amount округляется до копеек на уровне строки (HALF_UP в приложении), итог счёта — сумма строк.
CREATE TABLE invoice_line (
    id            uuid          PRIMARY KEY,
    invoice_id    uuid          NOT NULL REFERENCES invoice (id),
    plan_id       uuid          NOT NULL REFERENCES plan (id),
    description   varchar(255)  NOT NULL,
    quantity      integer       NOT NULL CHECK (quantity > 0),
    unit_price    numeric(19,2) NOT NULL CHECK (unit_price >= 0),
    from_date     date          NOT NULL,
    to_date       date          NOT NULL,
    days          integer       NOT NULL CHECK (days > 0),
    days_in_month integer       NOT NULL CHECK (days_in_month BETWEEN 28 AND 31),
    amount        numeric(19,2) NOT NULL,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    version       bigint        NOT NULL DEFAULT 0,
    CONSTRAINT ck_invoice_line_period CHECK (from_date < to_date)
);

CREATE INDEX ix_invoice_line_invoice_id ON invoice_line (invoice_id);
