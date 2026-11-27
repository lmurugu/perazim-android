-- READ-ONLY. Counts stored admin TOTP secrets and how they look. Prints no secret values.
-- Run: psql -X -f count-totp-rows.sql "$DATABASE_URL"
SELECT to_regclass('public.admin_security') IS NOT NULL AS admin_security_exists;
SELECT count(*) AS admin_security_rows,
       count(*) FILTER (WHERE totp_secret_cipher ~ '^v1:[0-9a-f]{24}:[0-9a-f]{32}:([0-9a-f]{2})+$') AS well_formed_v1_rows,
       count(*) FILTER (WHERE totp_secret_cipher !~ '^v1:[0-9a-f]{24}:[0-9a-f]{32}:([0-9a-f]{2})+$') AS malformed_rows
  FROM admin_security;
SELECT count(*) AS pilot_bootstrap_rows FROM pilot_bootstrap;
