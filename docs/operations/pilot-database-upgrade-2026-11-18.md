# Pilot database upgrade (2026-11-18)

The local pilot database (`perazim_local`, on the owner's machine, loopback only) had tables but no migration history. It was upgraded to migration 0021 with the verified runner.

## Steps taken
1. Backup: `pg_dump -Fc` to `server/.runtime/backups/perazim_local_before_refresh_20261118_102151.dump` (97 objects, readable with `pg_restore --list`). This was taken before any change.
2. Baseline: `node scripts/db-migrate.js baseline --confirm=perazim_local` with `MIGRATE_CONFIRM=perazim_local`. The command first took its own backup (`..._before_baseline_...dump`).
3. Migrate: `node scripts/db-migrate.js migrate --confirm=perazim_local` with `MIGRATE_CONFIRM`. The command took its own backup first (`..._before_migrate_...dump`). Result: 21 migrations applied.
4. Restarted the local API (`server/scripts/local-backend.sh start`) on port 3000. The previous API process was still running the old code and was stopped before the restart.

## Data check
- The database held one account before and after the upgrade. No rows were deleted or changed by the migrations apart from the 0007 rule (verified members become registered).

## Restore (if needed)
Stop the API, then restore the backup taken before the change:
```bash
pg_restore --clean --if-exists -d "$DATABASE_URL" server/.runtime/backups/perazim_local_before_refresh_20261118_102151.dump
```

## Commands now guarded
`scripts/db-migrate.js` allows writes to a database whose name does not end in `_test` only with `--confirm=<name>`, `MIGRATE_CONFIRM=<name>`, and a `pg_dump` backup written first. Connections must be to loopback.

## Correction: the original pilot server was not `perazim_local`
The original server used the database in the `perazim-pilot-postgres` container, with configuration in `server/.runtime/pilot/api.env`. On 2026-11-18 the API was restarted with `backend.env` (which points at `perazim_local`), so the admin accounts in the original database were not reachable. The upgrade above was applied to `perazim_local` and did not touch the original database.

Recovery:
1. Backed up the original database: `server/.runtime/backups/pilot_before_restore_20261118_103231.dump` (335 objects, 12 accounts).
2. An exact-schema baseline on the original database was refused (the schema has changes that are not recorded as migrations). Nothing was changed.
3. The original code (commit `040b723`, which still boots from the schema file, a git worktree at `<repo-parent>/worktrees/restore-pilot`) was started with `server/.runtime/pilot/api.env`. It is healthy and serving port 3000. Logs: `server/.runtime/pilot-restore.log`.

The original database still has no migration history. Moving it to the new migrations needs a decision on how to reconcile its extra schema changes.

## Cutover to the current code (decision, 2026-11-18)
The original database was adopted into the migration history rather than rebuilt, because it already held the 0001 to 0012 tables and their columns (checked against a freshly migrated database: the only differences in shared tables were the three user columns added by 0019 and 0020).
1. Backup taken: `server/.runtime/backups/pilot_before_cutover_*.dump` (339 objects). A rehearsal on a restored copy passed before this was done on the real database.
2. Migrations 0001 to 0012 recorded in the history table with their file checksums.
3. `scripts/db-migrate.js migrate` applied 0013 to 0021 (ledger: 21, accounts: 12, admins: 4 active).
4. The current code runs on port 3000 with `server/.runtime/pilot/api.env`. Health check passes.

## Migration 0007 check on the live pilot database (2026-11-18)

- **Database:** `perazim_phaseb_d6f49421bd_test` (the pilot database the upgrade record refers to).
- **Method:** count-only query, `SELECT status, verification_source, count(*) FROM membership_verifications GROUP BY 1,2`. No member rows were read.
- **Before (status | source | rows):**
  - REGISTERED|MANUAL|12
- **Contact-list rows (`CONTACT_LIST`) remaining:** none. Migration 0007 has nothing to demote, so nothing was applied and nothing was written.
- **After:** unchanged. No backup was needed because no data changed.

## Rename to perazim_pilot and production settings (2026-11-18)

- **Owner decisions:** the 12 accounts are real members. Move the pilot to production settings along with the rename. Stop the service briefly. Reboot test immediately after.
- **Before the change:** encrypted backup taken (`perazim_phaseb_d6f49421bd_test-20261118T151627Z.dump.enc`). A rehearsal copy restored from that backup started in production mode with no pending migrations and passed `/api/health`. The rehearsal copy was dropped afterwards.
- **Change:** `ALTER DATABASE perazim_phaseb_d6f49421bd_test RENAME TO perazim_pilot`. `server/.runtime/pilot/api.env` now sets `NODE_ENV=production`, points `DATABASE_URL` at `perazim_pilot`, and no longer sets the synthetic test variables or `ALLOW_MEMORY_STORE`.
- **After:** `perazim-api.service` active, `/api/health` 200, server log reports no pending migrations, `current_database() = perazim_pilot`, 12 accounts.
- **Follow-up for the owner:** the database connection string was printed once in a session transcript while checking the backup. Change the database password for the `postgres` user and update `api.env` before outside access is opened.
