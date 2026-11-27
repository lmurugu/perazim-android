-- PILOT-ONLY reconciliation step. Operator SQL, NOT a migration; never placed in server/migrations.
-- This file has NO BEGIN/COMMIT. Do not run it by itself: reconcile-and-migrate.js runs it inside
-- the same transaction as the pending migrations (0022, 0023). If you run it manually, use psql -1.
-- Effect: sets is_active = FALSE on version '0.1-TEST' only. No row is deleted, and no content,
-- hash, version, publication_status, withdrawn_at, effective_at or acknowledgement is changed.

DO $$
DECLARE
  active_count integer;
  active_version text;
  active_status text;
  v02_count integer;
BEGIN
  SELECT count(*) INTO active_count FROM privacy_notice_versions WHERE is_active;
  IF active_count <> 1 THEN
    RAISE EXCEPTION 'RECONCILE STOP: expected exactly one active notice, found %', active_count;
  END IF;
  SELECT version, publication_status INTO active_version, active_status
    FROM privacy_notice_versions WHERE is_active;
  IF active_version <> '0.1-TEST' OR active_status <> 'PUBLISHED' THEN
    RAISE EXCEPTION 'RECONCILE STOP: active notice is % (%), expected 0.1-TEST (PUBLISHED)', active_version, active_status;
  END IF;
  SELECT count(*) INTO v02_count FROM privacy_notice_versions WHERE version = '0.2';
  IF v02_count <> 0 THEN
    RAISE EXCEPTION 'RECONCILE STOP: version 0.2 is already recorded; this reconciliation applies only once';
  END IF;
END
$$;

UPDATE privacy_notice_versions
   SET is_active = FALSE
 WHERE version = '0.1-TEST' AND is_active = TRUE AND publication_status = 'PUBLISHED';

DO $$
BEGIN
  IF (SELECT count(*) FROM privacy_notice_versions WHERE is_active) <> 0 THEN
    RAISE EXCEPTION 'RECONCILE STOP: a notice is still active after deactivating 0.1-TEST';
  END IF;
END
$$;
