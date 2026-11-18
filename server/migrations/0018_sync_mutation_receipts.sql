-- Persist each authenticated mutation result so retried requests never apply twice.
CREATE TABLE IF NOT EXISTS sync_receipts (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mutation_id VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    response_json JSONB NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, mutation_id)
);
