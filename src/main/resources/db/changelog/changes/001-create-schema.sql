--liquibase formatted sql

--changeset mbi:001-create-schema
CREATE SEQUENCE app_users_seq START WITH 1000 INCREMENT BY 50;
CREATE SEQUENCE events_seq START WITH 1000 INCREMENT BY 50;
CREATE SEQUENCE reservations_seq START WITH 1000 INCREMENT BY 50;
CREATE SEQUENCE idempotency_keys_seq START WITH 1000 INCREMENT BY 50;
CREATE SEQUENCE audit_logs_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE app_users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_roles_role CHECK (role IN ('ADMIN', 'ORGANIZER', 'CUSTOMER'))
);

CREATE TABLE events (
    id BIGINT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(255),
    venue VARCHAR(255),
    starts_at TIMESTAMP WITH TIME ZONE,
    ends_at TIMESTAMP WITH TIME ZONE,
    capacity INTEGER NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_events_owner FOREIGN KEY (owner_id) REFERENCES app_users (id),
    CONSTRAINT ck_events_capacity_positive CHECK (capacity > 0),
    CONSTRAINT ck_events_date_range CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
);

CREATE INDEX ix_events_owner_id ON events (owner_id);
CREATE INDEX ix_events_public_search ON events (published, starts_at);

CREATE TABLE reservations (
    id BIGINT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    seats INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reservations_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_reservations_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_reservations_seats_positive CHECK (seats > 0)
);

CREATE INDEX ix_reservations_event_status ON reservations (event_id, status);
CREATE INDEX ix_reservations_user_id ON reservations (user_id);

CREATE TABLE idempotency_keys (
    id BIGINT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_idempotency_keys_actor FOREIGN KEY (actor_id) REFERENCES app_users (id),
    CONSTRAINT uk_idempotency_keys_scope UNIQUE (actor_id, endpoint, idempotency_key),
    CONSTRAINT ck_idempotency_keys_status CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT ck_idempotency_keys_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_idempotency_keys_response_status CHECK (response_status IS NULL OR response_status BETWEEN 100 AND 599)
);

CREATE INDEX ix_idempotency_keys_expiry ON idempotency_keys (expires_at);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id BIGINT,
    ip VARCHAR(45),
    user_agent VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_audit_logs_actor_created_at ON audit_logs (actor_id, created_at);
CREATE INDEX ix_audit_logs_resource ON audit_logs (resource_type, resource_id);

--rollback DROP TABLE audit_logs;
--rollback DROP TABLE idempotency_keys;
--rollback DROP TABLE reservations;
--rollback DROP TABLE events;
--rollback DROP TABLE user_roles;
--rollback DROP TABLE app_users;
--rollback DROP SEQUENCE audit_logs_seq;
--rollback DROP SEQUENCE idempotency_keys_seq;
--rollback DROP SEQUENCE reservations_seq;
--rollback DROP SEQUENCE events_seq;
--rollback DROP SEQUENCE app_users_seq;
