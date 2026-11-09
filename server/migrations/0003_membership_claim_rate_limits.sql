CREATE TABLE membership_claim_rate_limits (
  scope text NOT NULL CHECK (scope IN ('account', 'ip')),
  bucket_key text NOT NULL,
  attempts bigint[] NOT NULL CHECK (
    cardinality(attempts) BETWEEN 1 AND CASE scope WHEN 'account' THEN 5 ELSE 30 END
  ),
  expires_at bigint NOT NULL,
  PRIMARY KEY (scope, bucket_key)
);

CREATE INDEX membership_claim_rate_limits_expiry_idx
  ON membership_claim_rate_limits (expires_at);
