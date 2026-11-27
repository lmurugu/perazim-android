-- READ-ONLY pre-flight for perazim_pilot. Changes nothing.
-- Run: psql -X -f pre-flight.sql "$DATABASE_URL"
SHOW server_version;
SELECT version, publication_status, is_active, effective_at, withdrawn_at, content_hash, document_path
  FROM privacy_notice_versions ORDER BY created_at;
SELECT notice_version, count(*) AS acknowledgements
  FROM privacy_notice_acknowledgements GROUP BY notice_version ORDER BY notice_version;
SELECT version, name, checksum, applied_at FROM perazim_schema_migrations ORDER BY version;
