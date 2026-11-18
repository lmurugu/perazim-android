CREATE TABLE online_ai_daily_budget (
  budget_date date NOT NULL,
  bucket text NOT NULL CHECK (bucket = 'global' OR bucket ~ '^[0-9a-f]{64}$'),
  used integer NOT NULL CHECK (used > 0),
  PRIMARY KEY (budget_date, bucket)
);
