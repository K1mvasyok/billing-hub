--liquibase formatted sql

--changeset billing:006-create-billing-period
-- Расчётный месяц. period — всегда первое число месяца. Закрытый период защищён правилами приложения (BR-05).
CREATE TABLE billing_period (
    id          uuid         PRIMARY KEY,
    period      date         NOT NULL UNIQUE,
    status      varchar(16)  NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    closed_at   timestamptz,
    closed_by   varchar(255),
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_billing_period_first_day CHECK (EXTRACT(DAY FROM period) = 1)
);

--changeset billing:006-create-billing-run
-- Запуск выставления счетов за период. Статистика пишется по мере работы, поэтому счётчики не NULL.
CREATE TABLE billing_run (
    id                uuid         PRIMARY KEY,
    period            date         NOT NULL,
    status            varchar(16)  NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    started_at        timestamptz  NOT NULL,
    finished_at       timestamptz,
    contracts_total   integer      NOT NULL DEFAULT 0 CHECK (contracts_total >= 0),
    invoices_created  integer      NOT NULL DEFAULT 0 CHECK (invoices_created >= 0),
    skipped           integer      NOT NULL DEFAULT 0 CHECK (skipped >= 0),
    errors            integer      NOT NULL DEFAULT 0 CHECK (errors >= 0),
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    version           bigint       NOT NULL DEFAULT 0
);

CREATE INDEX ix_billing_run_period ON billing_run (period);
