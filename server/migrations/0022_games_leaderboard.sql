-- Church-wide Bible games leaderboard (owner decision 2026-11-18: whole church, opt-in, first name only).
-- A member appears only after turning the leaderboard on. Points are stars from correct answers, summed per week.
CREATE TABLE IF NOT EXISTS game_leaderboard_profiles (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id),
    opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS game_score_weeks (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    week_start DATE NOT NULL,
    points INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, week_start)
);
CREATE INDEX IF NOT EXISTS game_score_weeks_rank ON game_score_weeks(week_start, points DESC);

-- One row per finished game, so a retried upload never counts twice.
CREATE TABLE IF NOT EXISTS game_score_events (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    run_id VARCHAR(64) NOT NULL,
    game VARCHAR(32) NOT NULL,
    points INTEGER NOT NULL CHECK (points >= 0),
    created_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, run_id)
);
