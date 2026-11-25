-- Privacy notice v0.2 (PR 86, round 50). The notice text changed after version 0.1 was recorded, so the
-- active row no longer matched its file. Version 0.1 stays recorded and unchanged, and is made inactive.
-- Version 0.2 is registered as a DRAFT, not published: publication needs Levi's approval and is recorded separately.
UPDATE privacy_notice_versions SET is_active = FALSE WHERE version = '0.1' AND is_active;
INSERT INTO privacy_notice_versions (version,content_hash,document_path,publication_status,effective_at,is_active,created_at)
VALUES ('0.2','7b198323f99cdf35d677468c87252304636babd7d35fee60bd6fda505737c05e',
        'privacy-notice-v0.2.md','DRAFT',NULL,TRUE,(extract(epoch FROM clock_timestamp()) * 1000)::bigint)
ON CONFLICT (version) DO NOTHING;
