--liquibase formatted sql

--changeset billing:009-create-invoice-number-sequence
-- Сквозной номер счёта: INV-yyyy-MM-NNNNNN. Пропуски в номерах возможны (откат транзакции расходует значение).
CREATE SEQUENCE invoice_number_seq START WITH 1 INCREMENT BY 1;
