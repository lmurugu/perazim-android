-- Review, approval and publication are separate stages. No account receives
-- any source-governance grant through this migration.

CREATE TABLE ai_knowledge_conflict_groups (
    conflict_group TEXT PRIMARY KEY,
    classification TEXT NOT NULL CHECK (classification IN (
        'AUTHORITY_SCOPE', 'TEXTUAL_CONTRADICTION', 'VERSION', 'INCOMPLETE_SOURCE', 'OTHER'
    )),
    status TEXT NOT NULL DEFAULT 'UNRESOLVED' CHECK (status = 'UNRESOLVED'),
    classification_reference TEXT NOT NULL,
    created_by VARCHAR(64) NOT NULL REFERENCES users(id),
    created_at BIGINT NOT NULL
);

ALTER TABLE ai_knowledge_sources
    ADD COLUMN publication_status TEXT NOT NULL DEFAULT 'UNPUBLISHED'
      CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED', 'WITHDRAWN', 'SUPERSEDED')),
    ADD COLUMN reviewed_by VARCHAR(64) REFERENCES users(id),
    ADD COLUMN review_decision TEXT CHECK (review_decision IN ('RECOMMEND_APPROVAL', 'REJECT')),
    ADD COLUMN reviewed_at BIGINT,
    ADD COLUMN approval_actor_id VARCHAR(64) REFERENCES users(id),
    ADD COLUMN published_by VARCHAR(64) REFERENCES users(id),
    ADD COLUMN published_at BIGINT,
    ADD COLUMN withdrawn_by VARCHAR(64) REFERENCES users(id),
    ADD COLUMN superseded_by_source_id TEXT,
    ADD COLUMN superseded_by_version TEXT,
    ADD COLUMN updated_at BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ai_knowledge_source_review_fields_check CHECK (
      (reviewed_by IS NULL AND review_decision IS NULL AND reviewed_at IS NULL) OR
      (reviewed_by IS NOT NULL AND review_decision IS NOT NULL AND reviewed_at IS NOT NULL AND
       created_by IS NOT NULL AND reviewed_by <> created_by)
    ),
    ADD CONSTRAINT ai_knowledge_source_approval_actor_check CHECK (
      (approval_status <> 'APPROVED') OR
      (approval_actor_id IS NOT NULL AND created_by IS NOT NULL AND reviewed_by IS NOT NULL AND
       approval_actor_id <> created_by AND approval_actor_id <> reviewed_by AND
       approval_authority IS NOT NULL AND approval_reference IS NOT NULL AND approved_at IS NOT NULL)
    ),
    ADD CONSTRAINT ai_knowledge_source_publication_fields_check CHECK (
      (publication_status <> 'PUBLISHED') OR
      (approval_status = 'APPROVED' AND published_by IS NOT NULL AND published_at IS NOT NULL AND
       published_by <> created_by AND published_by <> reviewed_by AND published_by <> approval_actor_id)
    ),
    ADD CONSTRAINT ai_knowledge_source_unpublished_fields_check CHECK (
      (publication_status <> 'UNPUBLISHED') OR (published_by IS NULL AND published_at IS NULL)
    ),
    ADD CONSTRAINT ai_knowledge_source_withdrawal_fields_check CHECK (
      (approval_status <> 'WITHDRAWN') OR (withdrawn_by IS NOT NULL AND withdrawn_at IS NOT NULL AND withdrawal_reason IS NOT NULL)
    ),
    ADD CONSTRAINT ai_knowledge_source_supersession_fields_check CHECK (
      ((superseded_by_source_id IS NULL) = (superseded_by_version IS NULL)) AND
      ((approval_status = 'SUPERSEDED') = (superseded_by_source_id IS NOT NULL))
    ),
    ADD CONSTRAINT ai_knowledge_source_not_self_superseding CHECK (
      supersedes_source_id IS NULL OR source_id <> supersedes_source_id OR version <> supersedes_version
    ),
    ADD CONSTRAINT ai_knowledge_source_status_pair_check CHECK (
      ((publication_status = 'WITHDRAWN') = (approval_status = 'WITHDRAWN')) AND
      ((publication_status = 'SUPERSEDED') = (approval_status = 'SUPERSEDED'))
    ),
    ADD CONSTRAINT ai_knowledge_source_conflict_group_fk FOREIGN KEY (conflict_group)
      REFERENCES ai_knowledge_conflict_groups(conflict_group);

UPDATE ai_knowledge_sources SET updated_at = ingested_at;

CREATE UNIQUE INDEX ai_knowledge_one_published_version
    ON ai_knowledge_sources(source_id) WHERE publication_status = 'PUBLISHED';
CREATE INDEX ai_knowledge_published_retrieval
    ON ai_knowledge_sources(publication_status, approval_status, effective_at);

CREATE TABLE ai_knowledge_source_reviews (
    review_id UUID PRIMARY KEY,
    source_id TEXT NOT NULL,
    source_version TEXT NOT NULL,
    reviewer_id VARCHAR(64) NOT NULL REFERENCES users(id),
    decision TEXT NOT NULL CHECK (decision IN ('RECOMMEND_APPROVAL', 'REJECT')),
    reason_code TEXT NOT NULL CHECK (reason_code ~ '^[A-Z][A-Z0-9_]{2,39}$'),
    request_id UUID NOT NULL,
    created_at BIGINT NOT NULL,
    FOREIGN KEY (source_id, source_version) REFERENCES ai_knowledge_sources(source_id, version),
    UNIQUE (reviewer_id, request_id)
);

CREATE INDEX ai_knowledge_reviews_source_time
    ON ai_knowledge_source_reviews(source_id, source_version, created_at DESC);

CREATE TABLE ai_knowledge_conflict_sources (
    conflict_group TEXT NOT NULL REFERENCES ai_knowledge_conflict_groups(conflict_group),
    source_id TEXT NOT NULL,
    source_version TEXT NOT NULL,
    PRIMARY KEY (conflict_group, source_id, source_version),
    FOREIGN KEY (source_id, source_version) REFERENCES ai_knowledge_sources(source_id, version)
);

CREATE FUNCTION reject_ai_knowledge_conflict_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% is immutable conflict evidence', TG_TABLE_NAME;
END;
$$;
CREATE TRIGGER ai_knowledge_conflict_groups_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_knowledge_conflict_groups
    FOR EACH ROW EXECUTE FUNCTION reject_ai_knowledge_conflict_mutation();
CREATE TRIGGER ai_knowledge_conflict_groups_no_truncate
    BEFORE TRUNCATE ON ai_knowledge_conflict_groups
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_knowledge_conflict_mutation();
CREATE TRIGGER ai_knowledge_conflict_sources_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_knowledge_conflict_sources
    FOR EACH ROW EXECUTE FUNCTION reject_ai_knowledge_conflict_mutation();
CREATE TRIGGER ai_knowledge_conflict_sources_no_truncate
    BEFORE TRUNCATE ON ai_knowledge_conflict_sources
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_knowledge_conflict_mutation();

ALTER TABLE ai_knowledge_events
    DROP CONSTRAINT ai_knowledge_events_event_type_check,
    ADD CONSTRAINT ai_knowledge_events_event_type_check CHECK (event_type IN (
        'INGESTED', 'REVIEWED', 'APPROVED', 'PUBLISHED', 'WITHDRAWN', 'SUPERSEDED',
        'RECLASSIFIED', 'CONFLICT_CLASSIFIED'
    )),
    ADD COLUMN request_id UUID;
CREATE UNIQUE INDEX ai_knowledge_events_actor_request
    ON ai_knowledge_events(actor_id, request_id, source_id, source_version) WHERE request_id IS NOT NULL;

CREATE FUNCTION reject_ai_knowledge_review_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ai_knowledge_source_reviews is append-only';
END;
$$;
CREATE TRIGGER ai_knowledge_reviews_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_knowledge_source_reviews
    FOR EACH ROW EXECUTE FUNCTION reject_ai_knowledge_review_mutation();
CREATE TRIGGER ai_knowledge_reviews_no_truncate
    BEFORE TRUNCATE ON ai_knowledge_source_reviews
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_knowledge_review_mutation();

CREATE FUNCTION protect_ai_knowledge_source_identity() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.source_id, NEW.version, NEW.title, NEW.document_type, NEW.issuing_authority,
        NEW.authority_class, NEW.scope_of_authority, NEW.publication_date, NEW.confidentiality,
        NEW.confidentiality_reviewed_by, NEW.confidentiality_reviewed_at, NEW.permitted_audiences,
        NEW.permitted_uses, NEW.source_location, NEW.content_hash, NEW.language, NEW.translation_id,
        NEW.ingested_at, NEW.created_by, NEW.supersedes_source_id, NEW.supersedes_version)
       IS DISTINCT FROM
       (OLD.source_id, OLD.version, OLD.title, OLD.document_type, OLD.issuing_authority,
        OLD.authority_class, OLD.scope_of_authority, OLD.publication_date, OLD.confidentiality,
        OLD.confidentiality_reviewed_by, OLD.confidentiality_reviewed_at, OLD.permitted_audiences,
        OLD.permitted_uses, OLD.source_location, OLD.content_hash, OLD.language, OLD.translation_id,
        OLD.ingested_at, OLD.created_by, OLD.supersedes_source_id, OLD.supersedes_version) THEN
        RAISE EXCEPTION 'AI knowledge source identity and content are immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER ai_knowledge_source_identity_immutable
    BEFORE UPDATE ON ai_knowledge_sources
    FOR EACH ROW EXECUTE FUNCTION protect_ai_knowledge_source_identity();

CREATE FUNCTION guard_ai_knowledge_source_transitions() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.reviewed_by IS NOT NULL AND
       (NEW.reviewed_by, NEW.review_decision, NEW.reviewed_at) IS DISTINCT FROM
       (OLD.reviewed_by, OLD.review_decision, OLD.reviewed_at) THEN
        RAISE EXCEPTION 'source review evidence is immutable once recorded';
    END IF;
    IF OLD.approval_actor_id IS NOT NULL AND
       (NEW.approval_actor_id, NEW.approval_authority, NEW.approval_reference, NEW.approved_at) IS DISTINCT FROM
       (OLD.approval_actor_id, OLD.approval_authority, OLD.approval_reference, OLD.approved_at) THEN
        RAISE EXCEPTION 'source approval evidence is immutable once recorded';
    END IF;
    IF OLD.published_by IS NOT NULL AND (NEW.published_by, NEW.published_at) IS DISTINCT FROM (OLD.published_by, OLD.published_at) THEN
        RAISE EXCEPTION 'source publication evidence is immutable once recorded';
    END IF;
    IF OLD.withdrawn_by IS NOT NULL AND (NEW.withdrawn_by, NEW.withdrawn_at, NEW.withdrawal_reason) IS DISTINCT FROM
       (OLD.withdrawn_by, OLD.withdrawn_at, OLD.withdrawal_reason) THEN
        RAISE EXCEPTION 'source withdrawal evidence is immutable once recorded';
    END IF;
    IF OLD.superseded_by_source_id IS NOT NULL AND
       (NEW.superseded_by_source_id, NEW.superseded_by_version) IS DISTINCT FROM
       (OLD.superseded_by_source_id, OLD.superseded_by_version) THEN
        RAISE EXCEPTION 'source supersession evidence is immutable once recorded';
    END IF;
    IF OLD.conflict_group IS NOT NULL AND NEW.conflict_group IS DISTINCT FROM OLD.conflict_group THEN
        RAISE EXCEPTION 'source conflict classification is immutable once recorded';
    END IF;
    IF NEW.approval_status IS DISTINCT FROM OLD.approval_status AND NOT (
        (OLD.approval_status IN ('UNVERIFIED','DRAFT','PROPOSED') AND NEW.approval_status IN ('DRAFT','PENDING_APPROVAL')) OR
        (OLD.approval_status = 'PENDING_APPROVAL' AND NEW.approval_status IN ('DRAFT','APPROVED')) OR
        (OLD.approval_status = 'APPROVED' AND NEW.approval_status IN ('WITHDRAWN','SUPERSEDED'))
    ) THEN
        RAISE EXCEPTION 'invalid AI knowledge approval transition';
    END IF;
    IF NEW.publication_status IS DISTINCT FROM OLD.publication_status AND NOT (
        (OLD.publication_status = 'UNPUBLISHED' AND NEW.publication_status = 'PUBLISHED') OR
        (OLD.publication_status = 'PUBLISHED' AND NEW.publication_status IN ('WITHDRAWN','SUPERSEDED'))
    ) THEN
        RAISE EXCEPTION 'invalid AI knowledge publication transition';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER ai_knowledge_source_transition_guard
    BEFORE UPDATE ON ai_knowledge_sources
    FOR EACH ROW EXECUTE FUNCTION guard_ai_knowledge_source_transitions();

CREATE FUNCTION reject_ai_knowledge_source_delete() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'AI knowledge sources are retained for audit';
END;
$$;
CREATE TRIGGER ai_knowledge_sources_no_delete
    BEFORE DELETE ON ai_knowledge_sources
    FOR EACH ROW EXECUTE FUNCTION reject_ai_knowledge_source_delete();
CREATE TRIGGER ai_knowledge_sources_no_truncate
    BEFORE TRUNCATE ON ai_knowledge_sources
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_knowledge_source_delete();
CREATE FUNCTION reject_ai_knowledge_chunk_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'AI knowledge chunks are immutable';
END;
$$;
CREATE TRIGGER ai_knowledge_chunks_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_knowledge_chunks
    FOR EACH ROW EXECUTE FUNCTION reject_ai_knowledge_chunk_mutation();
CREATE TRIGGER ai_knowledge_chunks_no_truncate
    BEFORE TRUNCATE ON ai_knowledge_chunks
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_knowledge_chunk_mutation();

-- Separate grants preserve authority boundaries. No user_permissions rows are
-- created. Doctrinal approval remains disabled until a verified delegation exists.
INSERT INTO permissions(code, description, active) VALUES
    ('knowledge.source.manage', 'Register and inspect AI knowledge sources', TRUE),
    ('knowledge.source.review', 'Review source classification and recommend publication eligibility', TRUE),
    ('knowledge.source.approve.institutional', 'Record documented institutional doctrinal approval', FALSE),
    ('knowledge.source.publish', 'Publish, withdraw, or supersede an approved source', TRUE),
    ('knowledge.scripture.import', 'Stage a rights-verified Scripture corpus', TRUE),
    ('knowledge.scripture.publish', 'Publish a rights-verified Scripture corpus', TRUE)
ON CONFLICT (code) DO NOTHING;
UPDATE permissions SET active = FALSE WHERE code = 'knowledge.source.approve.institutional';

CREATE TABLE ai_scripture_corpora (
    corpus_id TEXT PRIMARY KEY,
    translation_id TEXT NOT NULL,
    source_name TEXT NOT NULL,
    source_version TEXT NOT NULL,
    license_evidence_reference TEXT NOT NULL,
    license_status TEXT NOT NULL CHECK (license_status IN ('AUTHORIZED', 'TEST_FIXTURE')),
    corpus_hash CHAR(64) NOT NULL CHECK (corpus_hash ~ '^[0-9a-f]{64}$'),
    book_count INTEGER NOT NULL CHECK (book_count > 0),
    chapter_count INTEGER NOT NULL CHECK (chapter_count > 0),
    verse_count INTEGER NOT NULL CHECK (verse_count > 0),
    integrity_validation_result TEXT NOT NULL CHECK (integrity_validation_result IN ('VALIDATED', 'TEST_ONLY')),
    imported_at BIGINT NOT NULL,
    imported_by VARCHAR(64) NOT NULL REFERENCES users(id),
    publication_status TEXT NOT NULL DEFAULT 'STAGED' CHECK (publication_status IN ('STAGED', 'PUBLISHED', 'RETIRED')),
    published_at BIGINT,
    published_by VARCHAR(64) REFERENCES users(id),
    manifest JSONB NOT NULL,
    CHECK ((publication_status = 'STAGED' AND published_at IS NULL AND published_by IS NULL) OR
           (publication_status IN ('PUBLISHED', 'RETIRED') AND published_at IS NOT NULL AND published_by IS NOT NULL)),
    CHECK ((license_status = 'AUTHORIZED') = (integrity_validation_result = 'VALIDATED'))
);

CREATE UNIQUE INDEX ai_scripture_one_published_translation
    ON ai_scripture_corpora(translation_id) WHERE publication_status = 'PUBLISHED';
CREATE INDEX ai_scripture_corpus_status
    ON ai_scripture_corpora(translation_id, publication_status, imported_at DESC);

CREATE TABLE ai_scripture_corpus_events (
    event_id UUID PRIMARY KEY,
    corpus_id TEXT NOT NULL REFERENCES ai_scripture_corpora(corpus_id),
    event_type TEXT NOT NULL CHECK (event_type IN ('IMPORTED', 'PUBLISHED', 'RETIRED')),
    actor_id VARCHAR(64) NOT NULL REFERENCES users(id),
    request_id UUID NOT NULL,
    reason TEXT NOT NULL CHECK (reason ~ '^[A-Z][A-Z0-9_]{2,39}$'),
    created_at BIGINT NOT NULL,
    UNIQUE(actor_id, request_id, corpus_id)
);
CREATE INDEX ai_scripture_corpus_events_time ON ai_scripture_corpus_events(corpus_id, created_at DESC);
CREATE FUNCTION reject_ai_scripture_corpus_event_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ai_scripture_corpus_events is append-only';
END;
$$;
CREATE TRIGGER ai_scripture_corpus_events_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_scripture_corpus_events
    FOR EACH ROW EXECUTE FUNCTION reject_ai_scripture_corpus_event_mutation();
CREATE TRIGGER ai_scripture_corpus_events_no_truncate
    BEFORE TRUNCATE ON ai_scripture_corpus_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_scripture_corpus_event_mutation();

CREATE TABLE ai_scripture_books (
    corpus_id TEXT NOT NULL REFERENCES ai_scripture_corpora(corpus_id),
    book_number INTEGER NOT NULL CHECK (book_number BETWEEN 1 AND 66),
    book_id TEXT NOT NULL,
    name TEXT NOT NULL,
    aliases JSONB NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(aliases) = 'array'),
    chapter_count INTEGER NOT NULL CHECK (chapter_count > 0),
    verse_count INTEGER NOT NULL CHECK (verse_count > 0),
    PRIMARY KEY (corpus_id, book_number),
    UNIQUE (corpus_id, book_id)
);

CREATE TABLE ai_scripture_verses (
    corpus_id TEXT NOT NULL,
    book_number INTEGER NOT NULL,
    chapter_number INTEGER NOT NULL CHECK (chapter_number > 0),
    verse_number INTEGER NOT NULL CHECK (verse_number > 0),
    text TEXT NOT NULL CHECK (length(btrim(text)) > 0),
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    PRIMARY KEY (corpus_id, book_number, chapter_number, verse_number),
    FOREIGN KEY (corpus_id, book_number) REFERENCES ai_scripture_books(corpus_id, book_number)
);

CREATE FUNCTION reject_ai_scripture_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'published Scripture corpus records are immutable';
END;
$$;
CREATE TRIGGER ai_scripture_books_no_mutation
    BEFORE UPDATE OR DELETE ON ai_scripture_books
    FOR EACH ROW EXECUTE FUNCTION reject_ai_scripture_mutation();
CREATE FUNCTION require_staged_scripture_corpus() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE publication_state TEXT;
BEGIN
    SELECT publication_status INTO publication_state FROM ai_scripture_corpora WHERE corpus_id=NEW.corpus_id;
    IF publication_state IS DISTINCT FROM 'STAGED' THEN
        RAISE EXCEPTION 'Scripture records may only be inserted into a staged corpus';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER ai_scripture_books_insert_staged_only
    BEFORE INSERT ON ai_scripture_books
    FOR EACH ROW EXECUTE FUNCTION require_staged_scripture_corpus();
CREATE TRIGGER ai_scripture_books_no_truncate
    BEFORE TRUNCATE ON ai_scripture_books
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_scripture_mutation();
CREATE TRIGGER ai_scripture_verses_no_mutation
    BEFORE UPDATE OR DELETE ON ai_scripture_verses
    FOR EACH ROW EXECUTE FUNCTION reject_ai_scripture_mutation();
CREATE TRIGGER ai_scripture_verses_insert_staged_only
    BEFORE INSERT ON ai_scripture_verses
    FOR EACH ROW EXECUTE FUNCTION require_staged_scripture_corpus();
CREATE TRIGGER ai_scripture_verses_no_truncate
    BEFORE TRUNCATE ON ai_scripture_verses
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_scripture_mutation();

CREATE FUNCTION protect_ai_scripture_corpus_identity() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.published_at IS NOT NULL AND (NEW.published_at, NEW.published_by) IS DISTINCT FROM (OLD.published_at, OLD.published_by) THEN
        RAISE EXCEPTION 'Scripture publication evidence is immutable once recorded';
    END IF;
    IF (NEW.corpus_id, NEW.translation_id, NEW.source_name, NEW.source_version,
        NEW.license_evidence_reference, NEW.license_status, NEW.corpus_hash,
        NEW.book_count, NEW.chapter_count, NEW.verse_count, NEW.integrity_validation_result,
        NEW.imported_at, NEW.imported_by, NEW.manifest)
       IS DISTINCT FROM
       (OLD.corpus_id, OLD.translation_id, OLD.source_name, OLD.source_version,
        OLD.license_evidence_reference, OLD.license_status, OLD.corpus_hash,
        OLD.book_count, OLD.chapter_count, OLD.verse_count, OLD.integrity_validation_result,
        OLD.imported_at, OLD.imported_by, OLD.manifest) THEN
        RAISE EXCEPTION 'Scripture corpus identity and content are immutable';
    END IF;
    IF NOT ((OLD.publication_status = 'STAGED' AND NEW.publication_status IN ('STAGED', 'PUBLISHED')) OR
            (OLD.publication_status = 'PUBLISHED' AND NEW.publication_status = 'RETIRED') OR
            (OLD.publication_status = 'RETIRED' AND NEW.publication_status = 'RETIRED')) THEN
        RAISE EXCEPTION 'invalid Scripture corpus publication transition';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER ai_scripture_corpus_identity_immutable
    BEFORE UPDATE ON ai_scripture_corpora
    FOR EACH ROW EXECUTE FUNCTION protect_ai_scripture_corpus_identity();
CREATE FUNCTION require_staged_scripture_corpus_insert() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.publication_status <> 'STAGED' OR NEW.published_at IS NOT NULL OR NEW.published_by IS NOT NULL THEN
        RAISE EXCEPTION 'Scripture corpus must be inserted as staged';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER ai_scripture_corpus_insert_staged_only
    BEFORE INSERT ON ai_scripture_corpora
    FOR EACH ROW EXECUTE FUNCTION require_staged_scripture_corpus_insert();
CREATE FUNCTION reject_ai_scripture_corpus_delete() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Scripture corpus records are retained for audit';
END;
$$;
CREATE TRIGGER ai_scripture_corpora_no_delete
    BEFORE DELETE OR TRUNCATE ON ai_scripture_corpora
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_scripture_corpus_delete();
