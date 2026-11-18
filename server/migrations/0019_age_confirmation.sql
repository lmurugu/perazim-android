-- Records the member's confirmation that they are 18 or over (plan C.5, review item 2).
-- The confirmation time and the policy version it accepted are kept on the account.
ALTER TABLE users ADD COLUMN IF NOT EXISTS age_confirmed_at BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS age_policy_version TEXT;
