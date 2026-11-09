-- Governed AI knowledge registry. Intentionally creates no approved sources.
-- Do not apply this migration to any existing database without separate approval.

CREATE TABLE ai_knowledge_sources (
    source_id TEXT NOT NULL,
    version TEXT NOT NULL,
    title TEXT NOT NULL,
    document_type TEXT NOT NULL,
    issuing_authority TEXT,
    authority_class TEXT NOT NULL,
    scope_of_authority TEXT,
    conflict_group TEXT,
    publication_date DATE,
    approval_status TEXT NOT NULL CHECK (approval_status IN (
        'APPROVED', 'DRAFT', 'PENDING_APPROVAL', 'PROPOSED', 'WITHDRAWN',
        'SUPERSEDED', 'UNVERIFIED', 'RESTRICTED'
    )),
    approval_authority TEXT,
    approval_reference TEXT,
    approved_at BIGINT,
    effective_at BIGINT,
    supersedes_source_id TEXT,
    supersedes_version TEXT,
    withdrawn_at BIGINT,
    withdrawal_reason TEXT,
    confidentiality TEXT NOT NULL,
    confidentiality_reviewed_by TEXT NOT NULL,
    confidentiality_reviewed_at BIGINT NOT NULL,
    permitted_audiences JSONB NOT NULL,
    permitted_uses JSONB NOT NULL,
    source_location TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    language TEXT NOT NULL,
    translation_id TEXT,
    ingested_at BIGINT NOT NULL,
    created_by TEXT,
    PRIMARY KEY (source_id, version),
    FOREIGN KEY (supersedes_source_id, supersedes_version)
      REFERENCES ai_knowledge_sources(source_id, version),
    CHECK (jsonb_typeof(permitted_audiences) = 'array'),
    CHECK (jsonb_typeof(permitted_uses) = 'array'),
    CHECK (approval_status <> 'APPROVED' OR
      (approval_authority IS NOT NULL AND approval_reference IS NOT NULL AND approved_at IS NOT NULL)),
    CHECK (approval_status <> 'WITHDRAWN' OR (withdrawn_at IS NOT NULL AND withdrawal_reason IS NOT NULL)),
    CHECK ((supersedes_source_id IS NULL) = (supersedes_version IS NULL))
);

CREATE INDEX ai_knowledge_sources_retrieval
    ON ai_knowledge_sources (approval_status, authority_class, effective_at);

CREATE TABLE ai_knowledge_chunks (
    chunk_id CHAR(64) PRIMARY KEY CHECK (chunk_id ~ '^[0-9a-f]{64}$'),
    source_id TEXT NOT NULL,
    source_version TEXT NOT NULL,
    section TEXT NOT NULL,
    location TEXT NOT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    created_at BIGINT NOT NULL,
    FOREIGN KEY (source_id, source_version)
      REFERENCES ai_knowledge_sources(source_id, version)
);

CREATE INDEX ai_knowledge_chunks_source
    ON ai_knowledge_chunks (source_id, source_version, section);

CREATE TABLE ai_knowledge_events (
    event_id UUID PRIMARY KEY,
    source_id TEXT NOT NULL,
    source_version TEXT NOT NULL,
    event_type TEXT NOT NULL CHECK (event_type IN (
        'INGESTED', 'APPROVED', 'WITHDRAWN', 'SUPERSEDED', 'RECLASSIFIED'
    )),
    actor_id VARCHAR(64) REFERENCES users(id),
    actor_role TEXT,
    authority_reference TEXT,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    FOREIGN KEY (source_id, source_version)
      REFERENCES ai_knowledge_sources(source_id, version)
);

CREATE INDEX ai_knowledge_events_source_time
    ON ai_knowledge_events(source_id, source_version, created_at, event_id);

CREATE FUNCTION reject_ai_knowledge_event_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ai_knowledge_events is append-only';
END;
$$;

CREATE TRIGGER ai_knowledge_events_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_knowledge_events
    FOR EACH ROW EXECUTE FUNCTION reject_ai_knowledge_event_mutation();
CREATE TRIGGER ai_knowledge_events_no_truncate
    BEFORE TRUNCATE ON ai_knowledge_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_knowledge_event_mutation();

-- There is deliberately no public approval endpoint and no source is approved
-- by this migration. Source changes must be performed through a future,
-- explicitly permission-checked administrative workflow.
