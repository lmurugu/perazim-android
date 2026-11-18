-- Groups: an owner can mark a group open, so members can join it themselves.
-- Closed (private) groups stay invisible to non-members, and anyone else must send a request.
-- Draft for owner review. Not deployed. Numbered after 0024 (event photos).

ALTER TABLE community_groups ADD COLUMN IF NOT EXISTS is_open BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS group_join_requests (
    group_id VARCHAR(64) NOT NULL REFERENCES community_groups(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED')),
    created_at BIGINT NOT NULL,
    decided_by VARCHAR(64) REFERENCES users(id),
    decided_at BIGINT,
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_group_join_requests_status ON group_join_requests (group_id, status);
