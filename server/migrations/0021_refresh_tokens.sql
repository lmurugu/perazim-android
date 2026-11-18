-- Rotating refresh tokens (review fix F-02). Only a hash of each token is stored.
-- A refresh token is replaced on every use; a reused token revokes the account's sessions.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id TEXT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    revoked_at BIGINT,
    replaced_by TEXT
);
CREATE INDEX IF NOT EXISTS refresh_tokens_user ON refresh_tokens(user_id);
