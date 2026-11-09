-- DATA-01: keep the active registration notice version and acknowledgement evidence server-side.
CREATE TABLE privacy_notice_versions (
    version TEXT PRIMARY KEY,
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    document_path TEXT NOT NULL,
    publication_status TEXT NOT NULL CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    effective_at BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    CHECK (publication_status <> 'PUBLISHED' OR effective_at IS NOT NULL)
);
CREATE UNIQUE INDEX idx_privacy_notice_one_active ON privacy_notice_versions(is_active) WHERE is_active;

INSERT INTO privacy_notice_versions (version,content_hash,document_path,publication_status,effective_at,is_active,created_at)
VALUES ('0.1','4959b39f7b969d7f891800853ab292f477943d563a4e48bf2d52f6b8fb4e79dd',
        'privacy-notice-v0.1.md','DRAFT',NULL,TRUE,(extract(epoch FROM clock_timestamp()) * 1000)::bigint)
ON CONFLICT (version) DO NOTHING;

CREATE TABLE privacy_notice_acknowledgements (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    notice_version TEXT NOT NULL REFERENCES privacy_notice_versions(version),
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    presented_at BIGINT NOT NULL,
    acknowledged_at BIGINT NOT NULL,
    acknowledgement_text TEXT NOT NULL,
    client_platform TEXT NOT NULL DEFAULT 'android',
    PRIMARY KEY (user_id, notice_version),
    CHECK (length(btrim(acknowledgement_text)) > 0)
);
CREATE INDEX idx_privacy_acknowledgements_version ON privacy_notice_acknowledgements(notice_version, acknowledged_at DESC);

CREATE FUNCTION reject_privacy_acknowledgement_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'privacy_notice_acknowledgements is append-only';
END;
$$;
CREATE TRIGGER privacy_ack_no_update_delete
    BEFORE UPDATE OR DELETE ON privacy_notice_acknowledgements
    FOR EACH ROW EXECUTE FUNCTION reject_privacy_acknowledgement_mutation();
CREATE TRIGGER privacy_ack_no_truncate
    BEFORE TRUNCATE ON privacy_notice_acknowledgements
    FOR EACH STATEMENT EXECUTE FUNCTION reject_privacy_acknowledgement_mutation();
