-- Member photos added to event posts. Each photo waits for leader approval before it shows.
-- Draft for owner review. Not deployed. Files use the existing free local /uploads store.

CREATE TABLE IF NOT EXISTS event_photos (
    id VARCHAR(64) PRIMARY KEY,
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    uploader_id VARCHAR(64) NOT NULL REFERENCES users(id),
    media_url TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by VARCHAR(64) REFERENCES users(id),
    reviewed_at BIGINT,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_event_photos_post_status ON event_photos (post_id, status);
