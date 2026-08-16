--liquibase formatted sql

--changeset mbi:004-add-reservation-version
ALTER TABLE reservations ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

--rollback ALTER TABLE reservations DROP COLUMN version;
