-- Community reactions (emoji feelings) on posts and comments, and a review status for posts.
-- Draft for owner review. Not deployed. Numbered after 0022 (games leaderboard).
-- Existing posts stay APPROVED, so current content remains visible.

CREATE TABLE IF NOT EXISTS post_reactions (
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id, emoji)
);
CREATE INDEX IF NOT EXISTS idx_post_reactions_post ON post_reactions (post_id);

CREATE TABLE IF NOT EXISTS comment_reactions (
    comment_id VARCHAR(64) NOT NULL REFERENCES post_comments(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (comment_id, user_id, emoji)
);
CREATE INDEX IF NOT EXISTS idx_comment_reactions_comment ON comment_reactions (comment_id);

ALTER TABLE community_posts
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED'
    CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED'));
CREATE INDEX IF NOT EXISTS idx_posts_review_status ON community_posts (review_status);
