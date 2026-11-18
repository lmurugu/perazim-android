-- Append-only, exact-target rollback controls issued through step-up admin authority.
-- No user grant is created here; the permission must be granted through the
-- existing privileged permission workflow after the appropriate approval.
INSERT INTO permissions(code, description, active) VALUES
  ('knowledge.package.rollback.authorize', 'Authorize an exact signed offline knowledge package rollback', TRUE)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE ai_offline_rollback_authorizations (
    authorization_id UUID PRIMARY KEY,
    actor_id VARCHAR(64) NOT NULL REFERENCES users(id),
    target_package_id TEXT NOT NULL CHECK (target_package_id ~ '^[A-Za-z0-9._:-]{1,120}$'),
    target_digest CHAR(64) NOT NULL CHECK (target_digest ~ '^[a-f0-9]{64}$'),
    control_sequence BIGINT NOT NULL UNIQUE CHECK (control_sequence > 0),
    issued_at BIGINT NOT NULL CHECK (issued_at > 0),
    expires_at BIGINT NOT NULL CHECK (expires_at > issued_at),
    reason_code TEXT NOT NULL CHECK (reason_code IN ('PACKAGE_ROLLBACK','SOURCE_CORRECTION','SECURITY_REMEDIATION')),
    request_id UUID NOT NULL,
    authorization_envelope JSONB NOT NULL CHECK (jsonb_typeof(authorization_envelope)='object'),
    UNIQUE (actor_id, request_id)
);

CREATE INDEX ai_offline_rollback_target_expiry
  ON ai_offline_rollback_authorizations(target_package_id,target_digest,expires_at DESC,control_sequence DESC);

CREATE FUNCTION reject_ai_offline_rollback_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'AI offline rollback authorizations are append-only';
END;
$$;
CREATE TRIGGER ai_offline_rollback_no_row_mutation
    BEFORE UPDATE OR DELETE ON ai_offline_rollback_authorizations
    FOR EACH ROW EXECUTE FUNCTION reject_ai_offline_rollback_mutation();
CREATE TRIGGER ai_offline_rollback_no_truncate
    BEFORE TRUNCATE ON ai_offline_rollback_authorizations
    FOR EACH STATEMENT EXECUTE FUNCTION reject_ai_offline_rollback_mutation();
