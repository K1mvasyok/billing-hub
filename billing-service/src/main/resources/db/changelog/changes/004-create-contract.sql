--liquibase formatted sql

--changeset billing:004-create-contract
-- Договор клиента. customer_id ссылается на customer, но модуль contracts работает с клиентом только по id.
CREATE TABLE contract (
    id                uuid         PRIMARY KEY,
    customer_id       uuid         NOT NULL REFERENCES customer (id),
    number            varchar(32)  NOT NULL UNIQUE,
    signed_on         date         NOT NULL,
    valid_from        date         NOT NULL,
    valid_to          date,
    status            varchar(16)  NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'TERMINATED')),
    payment_term_days integer      NOT NULL CHECK (payment_term_days >= 0),
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    version           bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_contract_period CHECK (valid_to IS NULL OR valid_from < valid_to)
);

CREATE INDEX ix_contract_customer_id ON contract (customer_id);

--changeset billing:004-create-contract-item
-- Позиция договора: план × количество на свой период. Период позиции должен лежать внутри периода договора.
CREATE TABLE contract_item (
    id          uuid        PRIMARY KEY,
    contract_id uuid        NOT NULL REFERENCES contract (id),
    plan_id     uuid        NOT NULL REFERENCES plan (id),
    quantity    integer     NOT NULL CHECK (quantity > 0),
    valid_from  date        NOT NULL,
    valid_to    date,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    version     bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_contract_item_period CHECK (valid_to IS NULL OR valid_from < valid_to)
);

CREATE INDEX ix_contract_item_contract_id ON contract_item (contract_id);
