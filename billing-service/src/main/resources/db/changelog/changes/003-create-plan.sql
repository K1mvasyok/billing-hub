--liquibase formatted sql

--changeset billing:003-create-plan
-- Тарифный план. code — стабильный бизнес-код, например 'BASIC'.
CREATE TABLE plan (
    id          uuid         PRIMARY KEY,
    code        varchar(32)  NOT NULL UNIQUE,
    name        varchar(255) NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0
);

--changeset billing:003-create-plan-price
-- Цена плана на период [valid_from, valid_to). valid_to NULL — бессрочно.
-- EXCLUDE не даёт двум периодам одного плана пересекаться: это правило держит БД, а не только код.
-- Стык [a, b) и [b, c) разрешён, потому что диапазоны полуоткрытые.
CREATE TABLE plan_price (
    id            uuid          PRIMARY KEY,
    plan_id       uuid          NOT NULL REFERENCES plan (id),
    valid_from    date          NOT NULL,
    valid_to      date,
    monthly_price numeric(19,2) NOT NULL CHECK (monthly_price >= 0),
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    version       bigint        NOT NULL DEFAULT 0,
    CONSTRAINT ck_plan_price_period CHECK (valid_to IS NULL OR valid_from < valid_to),
    CONSTRAINT ex_plan_price_no_overlap
        EXCLUDE USING gist (plan_id WITH =, daterange(valid_from, valid_to, '[)') WITH &&)
);
