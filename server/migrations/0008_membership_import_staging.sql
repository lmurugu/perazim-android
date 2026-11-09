-- DATA-01: stage incomplete and ambiguous import rows for human review.
-- This migration adds storage only; it does not import or inspect member data.

ALTER TABLE membership_imports
    ADD COLUMN idempotency_key CHAR(64);

CREATE UNIQUE INDEX idx_membership_imports_idempotency
    ON membership_imports (idempotency_key);

CREATE TABLE membership_import_staging_rows (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES membership_imports(id),
    row_number INT NOT NULL CHECK (row_number > 0),
    source_reference TEXT,
    full_name TEXT,
    phone_value TEXT,
    normalized_phone TEXT,
    row_status TEXT NOT NULL CHECK (row_status IN (
        'INCOMPLETE', 'REVIEW', 'REJECTED', 'PROMOTED'
    )),
    issue_codes TEXT[] NOT NULL DEFAULT '{}',
    fingerprint CHAR(64) NOT NULL CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
    promoted_record_id UUID REFERENCES membership_records(id),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (import_id, row_number),
    CHECK ((row_status = 'PROMOTED') = (promoted_record_id IS NOT NULL))
);
CREATE INDEX idx_membership_import_staging_batch ON membership_import_staging_rows(import_id,row_number);
CREATE INDEX idx_membership_import_staging_status ON membership_import_staging_rows(row_status,created_at DESC);
CREATE INDEX idx_membership_import_staging_phone ON membership_import_staging_rows(normalized_phone)
    WHERE normalized_phone IS NOT NULL;

CREATE TABLE membership_import_staging_events (
    id UUID PRIMARY KEY,
    staging_row_id UUID NOT NULL REFERENCES membership_import_staging_rows(id),
    actor_id VARCHAR(64) REFERENCES users(id),
    event TEXT NOT NULL CHECK (event IN ('STAGED', 'PROMOTED', 'REJECTED', 'CORRECTED')),
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_membership_import_staging_events_row ON membership_import_staging_events(staging_row_id,created_at,id);

CREATE FUNCTION reject_membership_import_staging_event_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'membership_import_staging_events is append-only';
END;
$$;
CREATE TRIGGER membership_import_staging_events_no_row_mutation
    BEFORE UPDATE OR DELETE ON membership_import_staging_events
    FOR EACH ROW EXECUTE FUNCTION reject_membership_import_staging_event_mutation();
CREATE TRIGGER membership_import_staging_events_no_truncate
    BEFORE TRUNCATE ON membership_import_staging_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_membership_import_staging_event_mutation();
