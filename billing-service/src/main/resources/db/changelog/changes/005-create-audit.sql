--liquibase formatted sql

--changeset billing:005-create-revinfo
-- Envers: revinfo — общий журнал ревизий. Одна ревизия = одна транзакция, которая меняла аудируемые сущности.
CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE revinfo (
    rev      integer PRIMARY KEY,
    revtstmp bigint  NOT NULL
);

--changeset billing:005-create-audit-tables
-- *_aud хранят снимок строки на каждую ревизию. revtype: 0 — добавлена, 1 — изменена, 2 — удалена.
-- Колонки повторяют поля сущностей: при расхождении Hibernate validate упадёт на старте.
CREATE TABLE contract_aud (
    id                uuid        NOT NULL,
    rev               integer     NOT NULL REFERENCES revinfo (rev),
    revtype           smallint,
    customer_id       uuid,
    number            varchar(32),
    signed_on         date,
    valid_from        date,
    valid_to          date,
    status            varchar(16),
    payment_term_days integer,
    PRIMARY KEY (id, rev)
);

CREATE TABLE contract_item_aud (
    id          uuid     NOT NULL,
    rev         integer  NOT NULL REFERENCES revinfo (rev),
    revtype     smallint,
    contract_id uuid,
    plan_id     uuid,
    quantity    integer,
    valid_from  date,
    valid_to    date,
    PRIMARY KEY (id, rev)
);

CREATE TABLE plan_price_aud (
    id            uuid          NOT NULL,
    rev           integer       NOT NULL REFERENCES revinfo (rev),
    revtype       smallint,
    plan_id       uuid,
    valid_from    date,
    valid_to      date,
    monthly_price numeric(19,2),
    PRIMARY KEY (id, rev)
);
