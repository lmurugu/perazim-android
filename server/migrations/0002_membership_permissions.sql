-- B2: membership records, scoped permissions, and auditable privileged access.
-- UUID values are supplied by the application; no database extension is needed.

CREATE TABLE membership_imports (
    id UUID PRIMARY KEY,
    source TEXT NOT NULL,
    checksum TEXT NOT NULL,
    actor_id VARCHAR(64) NOT NULL REFERENCES users(id),
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_membership_imports_created ON membership_imports (created_at DESC);

CREATE TABLE membership_records (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES membership_imports(id),
    reference TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    record_status TEXT NOT NULL CHECK (record_status IN ('ACTIVE', 'REVIEW', 'REVOKED')),
    revision INT NOT NULL DEFAULT 1 CHECK (revision > 0),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX idx_membership_records_import ON membership_records (import_id);
CREATE INDEX idx_membership_records_status ON membership_records (record_status, updated_at DESC);

CREATE TABLE membership_claims (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    request_id UUID NOT NULL UNIQUE,
    user_name TEXT NOT NULL,
    user_phone TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'REVIEW', 'APPROVED', 'REJECTED')),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX idx_membership_claims_user_status ON membership_claims (user_id, status, created_at DESC);
CREATE INDEX idx_membership_claims_status_updated ON membership_claims (status, updated_at DESC);

CREATE TABLE membership_verifications (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id),
    membership_record_id UUID REFERENCES membership_records(id),
    status TEXT NOT NULL DEFAULT 'REGISTERED' CHECK (status IN (
        'REGISTERED',
        'VERIFICATION_PENDING',
        'VERIFIED_MEMBER',
        'VERIFICATION_REVIEW',
        'VERIFICATION_REJECTED',
        'VERIFICATION_REVOKED'
    )),
    revision INT NOT NULL DEFAULT 1 CHECK (revision > 0),
    updated_at BIGINT NOT NULL,
    CHECK (status <> 'VERIFIED_MEMBER' OR membership_record_id IS NOT NULL)
);
CREATE INDEX idx_membership_verifications_status ON membership_verifications (status, updated_at DESC);
CREATE UNIQUE INDEX idx_membership_verifications_one_verified_account
    ON membership_verifications (membership_record_id)
    WHERE status = 'VERIFIED_MEMBER' AND membership_record_id IS NOT NULL;

CREATE TABLE membership_verification_history (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    claim_id UUID REFERENCES membership_claims(id),
    previous_status TEXT CHECK (previous_status IS NULL OR previous_status IN (
        'REGISTERED', 'VERIFICATION_PENDING', 'VERIFIED_MEMBER',
        'VERIFICATION_REVIEW', 'VERIFICATION_REJECTED', 'VERIFICATION_REVOKED'
    )),
    new_status TEXT NOT NULL CHECK (new_status IN (
        'REGISTERED', 'VERIFICATION_PENDING', 'VERIFIED_MEMBER',
        'VERIFICATION_REVIEW', 'VERIFICATION_REJECTED', 'VERIFICATION_REVOKED'
    )),
    actor_id VARCHAR(64) REFERENCES users(id),
    reason TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_membership_verification_history_user_created
    ON membership_verification_history (user_id, created_at DESC);
CREATE INDEX idx_membership_verification_history_claim ON membership_verification_history (claim_id);

CREATE TABLE permissions (
    code TEXT PRIMARY KEY,
    description TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_permissions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    permission_code TEXT NOT NULL REFERENCES permissions(code),
    scope TEXT NOT NULL DEFAULT 'global',
    granted_by VARCHAR(64) NOT NULL REFERENCES users(id),
    granted_at BIGINT NOT NULL,
    effective_at BIGINT NOT NULL,
    expires_at BIGINT,
    revoked_at BIGINT,
    revoked_by VARCHAR(64) REFERENCES users(id),
    CHECK (length(btrim(scope)) > 0),
    CHECK (expires_at IS NULL OR expires_at > effective_at)
);
CREATE INDEX idx_user_permissions_lookup
    ON user_permissions (user_id, permission_code, scope, effective_at);
CREATE INDEX idx_user_permissions_active_expiry
    ON user_permissions (user_id, expires_at) WHERE revoked_at IS NULL;

CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(64) REFERENCES users(id),
    action TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    outcome TEXT NOT NULL,
    reason TEXT NOT NULL DEFAULT '',
    request_id TEXT,
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_admin_audit_log_created ON admin_audit_log (created_at DESC);
CREATE INDEX idx_admin_audit_log_actor_created ON admin_audit_log (actor_id, created_at DESC);

CREATE FUNCTION reject_admin_audit_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'admin_audit_log is append-only';
END;
$$;
CREATE TRIGGER admin_audit_log_no_row_mutation
    BEFORE UPDATE OR DELETE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_admin_audit_mutation();
CREATE TRIGGER admin_audit_log_no_truncate
    BEFORE TRUNCATE ON admin_audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION reject_admin_audit_mutation();

CREATE TABLE admin_security (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id),
    totp_secret_cipher TEXT NOT NULL,
    totp_last_counter BIGINT NOT NULL DEFAULT -1,
    security_version INT NOT NULL DEFAULT 0,
    auth_not_before BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL
);

CREATE TABLE pilot_bootstrap (
    singleton BOOLEAN PRIMARY KEY CHECK (singleton IS TRUE),
    user_id VARCHAR(64) NOT NULL UNIQUE REFERENCES users(id),
    approval_ref TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE privileged_sessions (
    token_hash CHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    security_version INT NOT NULL,
    auth_token_hash CHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    revoked_at BIGINT,
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_privileged_sessions_user_created ON privileged_sessions (user_id, created_at DESC);
CREATE INDEX idx_privileged_sessions_active_expiry
    ON privileged_sessions (expires_at) WHERE revoked_at IS NULL;

INSERT INTO permissions (code, description, active) VALUES
    ('membership.status.self.read', 'Read own membership verification status', TRUE),
    ('membership.claim.self.submit', 'Submit a membership claim for own account', TRUE),
    ('membership.record.read', 'Read membership records', TRUE),
    ('membership.register.import', 'Import membership registry records', TRUE),
    ('membership.claim.review', 'Review membership claims', TRUE),
    ('membership.verification.confirm', 'Confirm membership verification', TRUE),
    ('membership.record.correct', 'Correct membership registry records', TRUE),
    ('membership.verification.correct', 'Correct membership verification status', TRUE),
    ('membership.verification.revoke', 'Revoke membership verification', TRUE),
    ('access.permissions.self.read', 'Read own effective permissions', TRUE),
    ('admin.permissions.read', 'Read permission assignments', TRUE),
    ('admin.permissions.grant', 'Grant scoped permissions', TRUE),
    ('admin.permissions.revoke', 'Revoke scoped permissions', TRUE),
    ('admin.audit.read', 'Read administrative audit entries', TRUE)
ON CONFLICT (code) DO NOTHING;
