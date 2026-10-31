--liquibase formatted sql

--changeset billing:002-create-customer
CREATE TABLE customer (
    id              uuid         PRIMARY KEY,
    type            varchar(8)   NOT NULL CHECK (type IN ('ORG', 'IE')),
    name            varchar(255) NOT NULL,
    tax_id          varchar(12)  NOT NULL,
    email           varchar(255),
    phone           varchar(32),
    status          varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    dunning_opt_out boolean      NOT NULL DEFAULT false,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    version         bigint       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_customer_tax_id_active ON customer (tax_id) WHERE status = 'ACTIVE';
