# Pilot secrets rotation: TOTP key, database password, and dependents

Status: scripted and tested on a scratch database. Not run on the pilot. Each step needs the owner's approval, and the services are stopped for the window. Secret values are never printed or pasted into chat. Keys live only in the API env file (mode 600).

## Scripts
- `../pilot-totp-reencrypt/`: `count-totp-rows.sql` (read-only), `reencrypt-totp.js` (dry run by default, `--apply --confirm=<db>` for one transaction, verifies every row, rolls back on any failure), `test-reencrypt.js` (9+ scratch tests, including key-swap rollback and dump restore).
- `../pilot-password-rotation/rotate-db-password.js`: dry run by default; `--apply --confirm=<role>` changes the role password and the `DATABASE_URL` line; `--rollback <backup env file> --confirm=<role>` restores both. Tests: `../pilot-password-rotation/test-rotate.js` (dry run, apply, rollback, failure after ALTER ROLE, guards).

## Findings from the repo
- Encrypted TOTP secrets: table `admin_security`, column `totp_secret_cipher` (migration 0002). Format `v1:<iv>:<tag>:<ciphertext>` (AES-256-GCM, `server/src/phase-b/totp.js`). Read in `server/src/routes/adminMembership.js` (step-up). Pilot has 1 row (Codex). Decrypt failure returns STEP_UP_UNAVAILABLE. Nothing else uses `PHASE_B_ADMIN_KEY`.
- `JWT_SECRET`: access tokens (15 min) are signed with it, so they fail after the restart. Refresh tokens are SHA-256 hashes in `refresh_tokens` (migration 0021) and don't depend on the secret, so sessions survive through one refresh.
- `YOUTUBE_API_KEY`: when empty, `/api/media/church-feed` returns 503, and Sermons falls back to the public channel feed and then to the bundled list. Safe to unset for the pilot.
- `DATABASE_URL` readers: `server/src/db.js`, `server/src/index.js`, `server/src/phase-b/security.js` (the running API); `server/scripts/db-migrate.js` (migrations, and the pilot backup it takes); `server/scripts/bootstrap-synthetic-admin.js` (synthetic test DB only); `server/scripts/ops/nightly-backup.sh` (sources `.runtime/pilot/api.env` at 02:00 and passes the URL to `backup-db.sh` as an argument, so the password appears in the process list during the dump); `server/scripts/ops/backup-db.sh`; the reconciliation scripts in `ops/pilot-reconciliation-v3/` (`backup-pilot.sh`, `pre-flight.sql`, `reconcile-and-migrate.js`, `recover-from-backup.sh`). `server/scripts/local-backend.sh` builds its own local role, so it is not affected. CI (`.github/workflows/backend.yml`) uses its own CI password, so it is not affected.
- `server/docker-compose.yml` and `server/deploy/compose.yml` set `DATABASE_URL` and `POSTGRES_PASSWORD`. The pilot runs in podman container `perazim-pilot-postgres` instead, so these files are not used for the pilot, but if a container was started with `POSTGRES_PASSWORD` in its environment, `podman inspect` still shows the old value. Check it, and remove the value if it is stale.
- Docs that mention the URL and need updating after the rotation: `docs/operations/pilot-database-upgrade-2026-11-18.md` and the runbook.

## Order (after approval)
1. Freeze: `systemctl --user stop perazim-api-local-review perazim-api` (only the one that runs). Pause the 02:00 backup timer for the window if it could run.
2. Fresh dump: `ops/pilot-reconciliation-v3/backup-pilot.sh` with the pilot URL; verify with `sha256sum --check`.
3. TOTP: count rows (`count-totp-rows.sql`). Generate the new key with `openssl rand -hex 32` into a temp file (mode 600). Run `reencrypt-totp.js` dry run with OLD and NEW keys, then `--apply --confirm=<db>` with `PRE_CHANGE_DUMP` set. Then set `PHASE_B_ADMIN_KEY` to the new key in `api.env`, keeping the old value in a backup copy.
4a. Local access first: `hba-scram-cutover.js` (see `../pilot-hba-scram/README.md`). Host connections become SCRAM, and the postgres OS user keeps peer access for recovery. Admin commands need `-p 55438` inside the container and `-u postgres`.
4. Database password: `rotate-db-password.js` dry run (`ENV_FILE`, `DB_ADMIN_CMD` for the container, e.g. `podman exec -i perazim-pilot-postgres psql -v ON_ERROR_STOP=1 -U postgres -d postgres`), then `--apply --confirm=<role>`.
5. Verify before starting: decrypt check with the new key (`reencrypt-totp.js` dry run with OLD set to the new key and NEW set to any valid key, which should decrypt 1 of 1); `pre-flight.sql`.
6. Start units. Check `/api/health` 200, `/api/community/posts` 200, `/api/community/groups` 401 without auth. Check a refresh round-trip works.
7. JWT_SECRET: rotate in the same window (generate, update `api.env`), restart, check refresh.
8. YOUTUBE_API_KEY: unset until a new key is issued. The Sermons fallback covers it.
9. Remove old copies: backup env files, temp key files, and the old dump's secret copies, after everything is verified. Do not keep the old password anywhere except the encrypted dump.

## Rollback
- Database password: `rotate-db-password.js --rollback <backup env> --confirm=<role>`.
- TOTP: re-run `reencrypt-totp.js --apply` with OLD and NEW swapped (tested), or restore the pre-change dump (tested).
- If any service fails after a restart, restore the backup `api.env` and restart. Rolling back TOTP and the password together requires both the backups.

## Owner decisions
- Rotate `PHASE_B_ADMIN_KEY` now (with re-encryption), or keep it and rely on the database password and JWT rotation?
- Rotate `YOUTUBE_API_KEY` now or leave the feed off until a key is issued?
