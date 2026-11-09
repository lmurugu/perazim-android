-- DATA-01: permit reviewed reconciliation for legacy accounts without a
-- registration identity snapshot. This does not backfill editable profile names.

ALTER TABLE membership_account_identities
    ADD COLUMN source TEXT NOT NULL DEFAULT 'REGISTRATION_TRIGGER'
        CHECK (source IN ('REGISTRATION_TRIGGER', 'ADMIN_RECONCILIATION')),
    ADD COLUMN created_at BIGINT NOT NULL DEFAULT (extract(epoch FROM clock_timestamp()) * 1000)::bigint;

CREATE TABLE membership_identity_reconciliation_requests (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    proposed_name TEXT NOT NULL CHECK (length(btrim(proposed_name)) BETWEEN 1 AND 128),
    proposed_phone TEXT NOT NULL CHECK (proposed_phone ~ '^\+254[17][0-9]{8}$'),
    source TEXT NOT NULL CHECK (source = 'USER_SUBMITTED'),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CORRECTION_REQUIRED')),
    submitted_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    reviewer_id VARCHAR(64) REFERENCES users(id),
    membership_record_id UUID REFERENCES membership_records(id),
    decision_reason TEXT,
    CHECK ((status = 'PENDING' AND reviewer_id IS NULL AND membership_record_id IS NULL) OR status <> 'PENDING')
);
CREATE UNIQUE INDEX idx_membership_identity_reconciliation_one_open
    ON membership_identity_reconciliation_requests(user_id)
    WHERE status IN ('PENDING', 'CORRECTION_REQUIRED');
CREATE INDEX idx_membership_identity_reconciliation_status
    ON membership_identity_reconciliation_requests(status,submitted_at,id);

CREATE TABLE membership_identity_reconciliation_revisions (
    reconciliation_id UUID NOT NULL REFERENCES membership_identity_reconciliation_requests(id),
    revision INT NOT NULL CHECK (revision > 0),
    proposed_name TEXT NOT NULL CHECK (length(btrim(proposed_name)) BETWEEN 1 AND 128),
    proposed_phone TEXT NOT NULL CHECK (proposed_phone ~ '^\+254[17][0-9]{8}$'),
    actor_id VARCHAR(64) NOT NULL REFERENCES users(id),
    source TEXT NOT NULL CHECK (source = 'USER_SUBMITTED'),
    created_at BIGINT NOT NULL,
    PRIMARY KEY (reconciliation_id,revision)
);
CREATE FUNCTION reject_membership_identity_reconciliation_revision_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'membership_identity_reconciliation_revisions is append-only';
END;
$$;
CREATE TRIGGER membership_identity_reconciliation_revisions_no_row_mutation
    BEFORE UPDATE OR DELETE ON membership_identity_reconciliation_revisions
    FOR EACH ROW EXECUTE FUNCTION reject_membership_identity_reconciliation_revision_mutation();
CREATE TRIGGER membership_identity_reconciliation_revisions_no_truncate
    BEFORE TRUNCATE ON membership_identity_reconciliation_revisions
    FOR EACH STATEMENT EXECUTE FUNCTION reject_membership_identity_reconciliation_revision_mutation();

CREATE TABLE membership_identity_reconciliation_events (
    id UUID PRIMARY KEY,
    reconciliation_id UUID NOT NULL REFERENCES membership_identity_reconciliation_requests(id),
    actor_id VARCHAR(64) REFERENCES users(id),
    event TEXT NOT NULL CHECK (event IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'CORRECTION_REQUIRED')),
    reason TEXT NOT NULL,
    membership_record_id UUID REFERENCES membership_records(id),
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_membership_identity_reconciliation_events
    ON membership_identity_reconciliation_events(reconciliation_id,created_at,id);

CREATE FUNCTION reject_membership_identity_reconciliation_event_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'membership_identity_reconciliation_events is append-only';
END;
$$;
CREATE TRIGGER membership_identity_reconciliation_events_no_row_mutation
    BEFORE UPDATE OR DELETE ON membership_identity_reconciliation_events
    FOR EACH ROW EXECUTE FUNCTION reject_membership_identity_reconciliation_event_mutation();
CREATE TRIGGER membership_identity_reconciliation_events_no_truncate
    BEFORE TRUNCATE ON membership_identity_reconciliation_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_membership_identity_reconciliation_event_mutation();

CREATE TABLE membership_account_identity_history (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    revision INT NOT NULL CHECK (revision > 0),
    registered_name TEXT NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('REGISTRATION_TRIGGER', 'ADMIN_RECONCILIATION')),
    changed_by VARCHAR(64) REFERENCES users(id),
    changed_at BIGINT NOT NULL,
    PRIMARY KEY (user_id,revision)
);
CREATE FUNCTION reject_membership_account_identity_history_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'membership_account_identity_history is append-only';
END;
$$;
CREATE TRIGGER membership_account_identity_history_no_row_mutation
    BEFORE UPDATE OR DELETE ON membership_account_identity_history
    FOR EACH ROW EXECUTE FUNCTION reject_membership_account_identity_history_mutation();
CREATE TRIGGER membership_account_identity_history_no_truncate
    BEFORE TRUNCATE ON membership_account_identity_history
    FOR EACH STATEMENT EXECUTE FUNCTION reject_membership_account_identity_history_mutation();
