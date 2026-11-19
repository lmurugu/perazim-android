-- Member safety tools required for store listing: members can report content and block other members.
-- Blocks hide the blocked member's posts and comments and stop new messages in either direction.
-- Reports are kept for church review; erasure removes the reporter's own reports.
CREATE TABLE IF NOT EXISTS user_blocks (
    blocker_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked ON user_blocks(blocked_id);

CREATE TABLE IF NOT EXISTS content_reports (
    id VARCHAR(80) PRIMARY KEY,
    reporter_id VARCHAR(64) NOT NULL REFERENCES users(id),
    target_type VARCHAR(16) NOT NULL CHECK (target_type IN ('POST', 'COMMENT', 'MESSAGE', 'MEMBER')),
    target_id VARCHAR(128) NOT NULL,
    reported_user_id VARCHAR(64) REFERENCES users(id),
    reason VARCHAR(32) NOT NULL,
    details VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACTIONED', 'DISMISSED')),
    created_at BIGINT NOT NULL,
    reviewed_at BIGINT,
    reviewed_by VARCHAR(64) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_content_reports_status ON content_reports(status, created_at);

-- Push notification devices for the native apps. A device token is personal data:
-- it is removed on sign-out and erased with the member's account.
CREATE TABLE IF NOT EXISTS push_devices (
    token VARCHAR(512) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    platform VARCHAR(16) NOT NULL CHECK (platform IN ('ios', 'android', 'web')),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_push_devices_user ON push_devices(user_id);
