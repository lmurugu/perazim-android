-- Tokens issued before this instant (in seconds since the epoch) are rejected.
-- Set by a PIN change, so an old or stolen token stops working on other devices.
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_valid_after_seconds BIGINT NOT NULL DEFAULT 0;
