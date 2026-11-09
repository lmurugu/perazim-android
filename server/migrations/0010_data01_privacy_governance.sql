-- DATA-01: governance infrastructure only. No purpose, retention duration,
-- publication approval, or real-data record is seeded by this migration.

ALTER TABLE privacy_notice_versions ADD COLUMN withdrawn_at BIGINT;
ALTER TABLE privacy_notice_versions ADD CONSTRAINT privacy_notice_withdrawal_status_check
    CHECK (withdrawn_at IS NULL OR publication_status <> 'PUBLISHED');

CREATE TABLE privacy_policy_versions (
    version TEXT PRIMARY KEY,
    document_kind TEXT NOT NULL CHECK (document_kind IN ('PUBLIC_POLICY', 'APP_NOTICE')),
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    document_path TEXT NOT NULL,
    publication_status TEXT NOT NULL CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'WITHDRAWN', 'SUPERSEDED')),
    effective_at BIGINT,
    withdrawn_at BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    requires_acknowledgement BOOLEAN NOT NULL DEFAULT FALSE,
    approval_reference TEXT,
    created_at BIGINT NOT NULL,
    CHECK ((publication_status = 'PUBLISHED') = (effective_at IS NOT NULL)),
    CHECK (publication_status <> 'WITHDRAWN' OR withdrawn_at IS NOT NULL)
);
CREATE UNIQUE INDEX privacy_policy_one_active_published_per_kind
    ON privacy_policy_versions(document_kind)
    WHERE publication_status = 'PUBLISHED' AND is_active;

INSERT INTO permissions (code, description, active)
VALUES ('privacy.document.publish', 'Publish or withdraw reviewed public privacy documents', TRUE)
ON CONFLICT (code) DO NOTHING;

CREATE FUNCTION protect_privacy_policy_version() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.version, NEW.document_kind, NEW.content_hash, NEW.document_path)
       IS DISTINCT FROM
       (OLD.version, OLD.document_kind, OLD.content_hash, OLD.document_path) THEN
        RAISE EXCEPTION 'privacy document identity and content are immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER privacy_policy_version_identity_immutable
    BEFORE UPDATE ON privacy_policy_versions
    FOR EACH ROW EXECUTE FUNCTION protect_privacy_policy_version();

CREATE TABLE privacy_publication_events (
    id UUID PRIMARY KEY,
    document_version TEXT NOT NULL REFERENCES privacy_policy_versions(version),
    event TEXT NOT NULL CHECK (event IN ('SUBMITTED', 'APPROVED', 'PUBLISHED', 'WITHDRAWN', 'SUPERSEDED')),
    actor_id VARCHAR(64) REFERENCES users(id),
    actor_role TEXT,
    decision_reference TEXT,
    reason_code TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX privacy_publication_events_version_time
    ON privacy_publication_events(document_version, created_at, id);

CREATE TABLE privacy_presentation_receipts (
    receipt_hash CHAR(64) PRIMARY KEY CHECK (receipt_hash ~ '^[0-9a-f]{64}$'),
    user_id VARCHAR(64) REFERENCES users(id),
    notice_version TEXT NOT NULL REFERENCES privacy_notice_versions(version),
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    presented_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    consumed_at BIGINT,
    CHECK (expires_at > presented_at),
    CHECK (consumed_at IS NULL OR consumed_at >= presented_at)
);
CREATE INDEX privacy_presentation_receipts_expiry ON privacy_presentation_receipts(expires_at);
CREATE FUNCTION protect_privacy_presentation_receipt() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.receipt_hash, NEW.user_id, NEW.notice_version, NEW.content_hash, NEW.presented_at, NEW.expires_at)
       IS DISTINCT FROM
       (OLD.receipt_hash, OLD.user_id, OLD.notice_version, OLD.content_hash, OLD.presented_at, OLD.expires_at)
       OR OLD.consumed_at IS NOT NULL OR NEW.consumed_at IS NULL THEN
        RAISE EXCEPTION 'privacy presentation receipt is immutable except for one-time consumption';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER privacy_presentation_receipt_single_use
    BEFORE UPDATE ON privacy_presentation_receipts
    FOR EACH ROW EXECUTE FUNCTION protect_privacy_presentation_receipt();
CREATE FUNCTION reject_data01_row_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$;
CREATE TRIGGER privacy_policy_versions_no_delete
    BEFORE DELETE ON privacy_policy_versions
    FOR EACH ROW EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER privacy_policy_versions_no_truncate
    BEFORE TRUNCATE ON privacy_policy_versions
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER privacy_presentation_receipts_no_delete
    BEFORE DELETE ON privacy_presentation_receipts
    FOR EACH ROW EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER privacy_presentation_receipts_no_truncate
    BEFORE TRUNCATE ON privacy_presentation_receipts
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER privacy_publication_events_no_row_mutation
    BEFORE UPDATE OR DELETE ON privacy_publication_events
    FOR EACH ROW EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER privacy_publication_events_no_truncate
    BEFORE TRUNCATE ON privacy_publication_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_row_mutation();

-- The purpose catalogue intentionally remains empty until PMCI supplies decisions.
CREATE TABLE consent_purposes (
    purpose_key TEXT PRIMARY KEY,
    wording_version TEXT NOT NULL,
    wording_hash CHAR(64) NOT NULL CHECK (wording_hash ~ '^[0-9a-f]{64}$'),
    purpose_text TEXT NOT NULL,
    lawful_basis_reference TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);
CREATE TABLE consent_events (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    purpose_key TEXT NOT NULL REFERENCES consent_purposes(purpose_key),
    wording_version TEXT NOT NULL,
    wording_hash CHAR(64) NOT NULL CHECK (wording_hash ~ '^[0-9a-f]{64}$'),
    decision TEXT NOT NULL CHECK (decision IN ('GRANTED', 'DECLINED', 'WITHDRAWN')),
    channel TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX consent_events_current_lookup ON consent_events(user_id, purpose_key, created_at DESC, id DESC);
CREATE TRIGGER consent_events_no_row_mutation
    BEFORE UPDATE OR DELETE ON consent_events
    FOR EACH ROW EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER consent_events_no_truncate
    BEFORE TRUNCATE ON consent_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_row_mutation();

CREATE TABLE retention_rules (
    rule_key TEXT PRIMARY KEY,
    data_category TEXT NOT NULL,
    purpose_key TEXT,
    trigger_definition TEXT NOT NULL,
    duration_days INTEGER CHECK (duration_days IS NULL OR duration_days > 0),
    review_due_at BIGINT,
    disposal_method TEXT,
    policy_version TEXT,
    approval_reference TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'APPROVED', 'SUSPENDED', 'RETIRED')),
    created_at BIGINT NOT NULL,
    CHECK (status <> 'APPROVED' OR (duration_days IS NOT NULL AND review_due_at IS NOT NULL AND approval_reference IS NOT NULL))
);
CREATE TABLE retention_reviews (
    id UUID PRIMARY KEY,
    rule_key TEXT NOT NULL REFERENCES retention_rules(rule_key),
    subject_ref TEXT NOT NULL,
    trigger_at BIGINT,
    reviewed_by VARCHAR(64) REFERENCES users(id),
    review_outcome TEXT NOT NULL CHECK (review_outcome IN ('ELIGIBLE', 'REVIEW_REQUIRED', 'BLOCKED', 'NOT_ELIGIBLE')),
    reason_code TEXT NOT NULL,
    reviewed_at BIGINT NOT NULL
);
CREATE TABLE legal_holds (
    id UUID PRIMARY KEY,
    subject_ref TEXT NOT NULL,
    data_category TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'RELEASED')),
    reason_reference TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    released_at BIGINT,
    CHECK ((status = 'RELEASED') = (released_at IS NOT NULL))
);
CREATE TABLE disposal_events (
    id UUID PRIMARY KEY,
    rule_key TEXT NOT NULL REFERENCES retention_rules(rule_key),
    subject_ref TEXT NOT NULL,
    outcome TEXT NOT NULL CHECK (outcome IN ('DISPOSED', 'DEFERRED')),
    reason_code TEXT NOT NULL,
    authorized_by VARCHAR(64) NOT NULL REFERENCES users(id),
    confirmed_by VARCHAR(64) NOT NULL REFERENCES users(id),
    created_at BIGINT NOT NULL,
    CHECK (authorized_by <> confirmed_by)
);
CREATE FUNCTION reject_data01_evidence_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% is append-only evidence', TG_TABLE_NAME;
END;
$$;
CREATE TRIGGER retention_reviews_no_row_mutation BEFORE UPDATE OR DELETE ON retention_reviews
    FOR EACH ROW EXECUTE FUNCTION reject_data01_evidence_mutation();
CREATE TRIGGER retention_reviews_no_truncate BEFORE TRUNCATE ON retention_reviews
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_evidence_mutation();
CREATE TRIGGER disposal_events_no_row_mutation BEFORE UPDATE OR DELETE ON disposal_events
    FOR EACH ROW EXECUTE FUNCTION reject_data01_evidence_mutation();
CREATE TRIGGER disposal_events_no_truncate BEFORE TRUNCATE ON disposal_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_evidence_mutation();

CREATE TABLE data_subject_requests (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    requester_id VARCHAR(64) NOT NULL REFERENCES users(id),
    request_type TEXT NOT NULL CHECK (request_type IN ('ACCESS', 'CORRECTION', 'ERASURE')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_REVIEW', 'COMPLETED', 'DEFERRED', 'REJECTED')),
    verified_at BIGINT,
    verification_method TEXT,
    scope TEXT,
    decision_reason_code TEXT,
    submitted_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX data_subject_requests_requester ON data_subject_requests(requester_id, submitted_at DESC);
CREATE TABLE data_subject_request_events (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES data_subject_requests(id),
    actor_id VARCHAR(64) REFERENCES users(id),
    event TEXT NOT NULL CHECK (event IN ('SUBMITTED', 'VERIFIED', 'REVISION_CREATED', 'COMPLETED', 'DEFERRED', 'REJECTED')),
    reason_code TEXT NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
    created_at BIGINT NOT NULL
);
CREATE TABLE data_subject_request_revisions (
    request_id UUID NOT NULL REFERENCES data_subject_requests(id),
    revision INTEGER NOT NULL CHECK (revision > 0),
    request_type TEXT NOT NULL CHECK (request_type IN ('ACCESS', 'CORRECTION', 'ERASURE')),
    actor_id VARCHAR(64) NOT NULL REFERENCES users(id),
    created_at BIGINT NOT NULL,
    PRIMARY KEY (request_id, revision)
);
CREATE INDEX data_subject_request_events_timeline ON data_subject_request_events(request_id, created_at, id);
CREATE TRIGGER data_subject_request_events_no_row_mutation BEFORE UPDATE OR DELETE ON data_subject_request_events
    FOR EACH ROW EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER data_subject_request_events_no_truncate BEFORE TRUNCATE ON data_subject_request_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER data_subject_request_revisions_no_row_mutation BEFORE UPDATE OR DELETE ON data_subject_request_revisions
    FOR EACH ROW EXECUTE FUNCTION reject_data01_row_mutation();
CREATE TRIGGER data_subject_request_revisions_no_truncate BEFORE TRUNCATE ON data_subject_request_revisions
    FOR EACH STATEMENT EXECUTE FUNCTION reject_data01_row_mutation();

-- Down migration intentionally omitted: publication, consent, request and
-- disposal evidence is append-only and must not be silently discarded.
