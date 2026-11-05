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
    grace_points INT NOT NULL DEFAULT 2,
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
