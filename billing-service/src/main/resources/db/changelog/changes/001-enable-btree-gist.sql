--liquibase formatted sql

--changeset billing:001-enable-btree-gist
CREATE EXTENSION IF NOT EXISTS btree_gist;
