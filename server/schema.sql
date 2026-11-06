-- ============================================================================
-- Perazim Mission Church Database Schema (PostgreSQL)
-- ============================================================================

CREATE TABLE IF NOT EXISTS campuses (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    location VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL
);

INSERT INTO campuses (id, name, location, contact_phone, is_active, created_at)
VALUES
    ('campus_embu', 'Embu Headquarters', 'Blue Valley, Embu', '+254710772227', TRUE, 1700000000000),
    ('campus_nairobi', 'Nairobi Fellowship', 'CBD, Nairobi', '+254710772227', TRUE, 1700000000000),
    ('campus_meru', 'Meru Outreach', 'Town Center, Meru', '+254710772227', TRUE, 1700000000000),
    ('campus_online', 'Online Global Campus', 'Worldwide Broadcast', '+254710772227', TRUE, 1700000000000)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    phone VARCHAR(32) UNIQUE NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    name VARCHAR(128) NOT NULL,
    campus_id VARCHAR(64) REFERENCES campuses(id) DEFAULT 'campus_embu',
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    avatar_url TEXT DEFAULT '',
    xp INT NOT NULL DEFAULT 0,
    streak INT NOT NULL DEFAULT 0,
    grace_points INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS prayers (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_id VARCHAR(64) NOT NULL,
    author_name VARCHAR(128) NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
    prayer_count INT NOT NULL DEFAULT 0,
    is_answered BOOLEAN NOT NULL DEFAULT FALSE,
    testimony TEXT DEFAULT '',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prayers_updated_id ON prayers (updated_at ASC, id ASC);

CREATE TABLE IF NOT EXISTS prayer_amens (
    prayer_id VARCHAR(64) REFERENCES prayers(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (prayer_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    sender_id VARCHAR(64) NOT NULL,
    receiver_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS connections (
    id VARCHAR(64) PRIMARY KEY,
    requester_id VARCHAR(64) NOT NULL,
    recipient_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACCEPTED',
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_events (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    payload TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS revoked_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    expires_at BIGINT NOT NULL,
    revoked_at BIGINT NOT NULL
);

-- ============================================================================
-- Community Upgrade Schema (Phase B)
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT DEFAULT '';

CREATE TABLE IF NOT EXISTS community_posts (
    id VARCHAR(64) PRIMARY KEY,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id),
    author_name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    media_url TEXT DEFAULT '',
    group_id VARCHAR(64) DEFAULT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_answered BOOLEAN NOT NULL DEFAULT FALSE,
    event_date BIGINT DEFAULT NULL,
    event_location VARCHAR(255) DEFAULT '',
    amen_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_posts_created_at ON community_posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_type ON community_posts (type);
CREATE INDEX IF NOT EXISTS idx_posts_group ON community_posts (group_id);

CREATE TABLE IF NOT EXISTS post_amens (
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS post_comments (
    id VARCHAR(64) PRIMARY KEY,
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id),
    author_name VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_post_id ON post_comments (post_id, created_at ASC);

CREATE TABLE IF NOT EXISTS community_groups (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    created_by VARCHAR(64) NOT NULL REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS group_memberships (
    group_id VARCHAR(64) NOT NULL REFERENCES community_groups(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    joined_at BIGINT NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

ALTER TABLE messages ADD COLUMN IF NOT EXISTS media_url TEXT DEFAULT '';
ALTER TABLE messages ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED';
ALTER TABLE messages ADD COLUMN IF NOT EXISTS delivered_at BIGINT DEFAULT NULL;

CREATE TABLE IF NOT EXISTS event_rsvps (
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'ATTENDING',
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS in_app_notifications (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id VARCHAR(64) NOT NULL,
    actor_name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notif_user ON in_app_notifications (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS daily_challenges (
    date VARCHAR(32) PRIMARY KEY,
    riddle_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    hint TEXT DEFAULT '',
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_challenge_completions (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date VARCHAR(32) NOT NULL,
    completed_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, date)
);

-- Apply the new-account allowance to existing installations without resetting balances.
ALTER TABLE users ALTER COLUMN grace_points SET DEFAULT 0;
