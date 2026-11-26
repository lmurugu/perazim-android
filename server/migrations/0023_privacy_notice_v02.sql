-- Privacy notice v0.2 (PR 86, round 50). The notice text changed after version 0.1 was recorded, so the
-- active row no longer matched its file. Version 0.1 stays recorded and unchanged, and is made inactive.
-- Version 0.2 is registered as a DRAFT, not published: publication needs Levi's approval and is recorded separately.
UPDATE privacy_notice_versions SET is_active = FALSE WHERE version = '0.1' AND is_active;
INSERT INTO privacy_notice_versions (version,content_hash,document_path,publication_status,effective_at,is_active,created_at)
VALUES ('0.2','2ddb3e68f57cbf64d47aa676f3f8ce7b81c4c1a6c0bbc445f864fa38c731ae4a',
        'privacy-notice-v0.2.md','DRAFT',NULL,TRUE,(extract(epoch FROM clock_timestamp()) * 1000)::bigint)
ON CONFLICT (version) DO NOTHING;
