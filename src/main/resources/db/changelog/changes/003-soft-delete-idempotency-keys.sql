--liquibase formatted sql

--changeset mbi:003-soft-delete-idempotency-keys
ALTER TABLE idempotency_keys ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE idempotency_keys DROP CONSTRAINT uk_idempotency_keys_scope;
CREATE INDEX ix_idempotency_keys_deleted_at ON idempotency_keys (deleted_at);

--rollback DROP INDEX ix_idempotency_keys_deleted_at;
--rollback ALTER TABLE idempotency_keys ADD CONSTRAINT uk_idempotency_keys_scope UNIQUE (actor_id, endpoint, idempotency_key);
--rollback ALTER TABLE idempotency_keys DROP COLUMN deleted_at;

--changeset mbi:003-active-idempotency-scope-postgresql dbms:postgresql
CREATE UNIQUE INDEX uk_idempotency_keys_scope
    ON idempotency_keys (actor_id, endpoint, idempotency_key)
    WHERE deleted_at IS NULL;

--rollback DROP INDEX uk_idempotency_keys_scope;

--changeset mbi:003-active-idempotency-scope-h2 dbms:h2
CREATE UNIQUE NULLS NOT DISTINCT INDEX uk_idempotency_keys_scope
    ON idempotency_keys (actor_id, endpoint, idempotency_key, deleted_at);

--rollback DROP INDEX uk_idempotency_keys_scope;
