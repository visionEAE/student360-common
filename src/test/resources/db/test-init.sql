-- Mirrors student360-infra/infra/init-db/03-audit.sql; the library must work against the real
-- table shape, and the shape is owned by infra. Plus an outbox table as a service would create it
-- through Flyway in its own schema.
CREATE SCHEMA audit;

CREATE TABLE audit.audit_record (
    id                   BIGSERIAL PRIMARY KEY,
    occurred_at          TIMESTAMPTZ  NOT NULL,
    request_id           TEXT         NOT NULL,
    trace_id             TEXT,
    service_name         TEXT         NOT NULL,
    record_type          TEXT         NOT NULL,
    action               TEXT         NOT NULL,
    actor_id             UUID,
    actor_roles          TEXT[],
    subject_type         TEXT,
    subject_id           TEXT,
    authorization_basis  TEXT,
    outcome              TEXT         NOT NULL,
    source_ip            TEXT,
    details              JSONB,
    CONSTRAINT chk_audit_record_type    CHECK (record_type IN ('DATA_ACCESS', 'SECURITY', 'STATE_CHANGE')),
    CONSTRAINT chk_audit_record_outcome CHECK (outcome IN ('ALLOWED', 'DENIED'))
);

CREATE TABLE outbox_event (
    id              UUID PRIMARY KEY,
    event_type      TEXT        NOT NULL,
    aggregate_type  TEXT        NOT NULL,
    aggregate_id    TEXT        NOT NULL,
    payload         JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ
);
