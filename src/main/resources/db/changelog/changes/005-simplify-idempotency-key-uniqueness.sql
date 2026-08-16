--liquibase formatted sql

--changeset mbi:005-simplify-idempotency-key-uniqueness
ALTER TABLE idempotency_keys DROP CONSTRAINT IF EXISTS uk_idempotency_keys_scope;
DROP INDEX IF EXISTS uk_idempotency_keys_scope;
ALTER TABLE idempotency_keys DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE idempotency_keys
    ADD CONSTRAINT uk_idempotency_keys_scope UNIQUE (actor_id, endpoint, idempotency_key);

--rollback ALTER TABLE idempotency_keys DROP CONSTRAINT uk_idempotency_keys_scope;
