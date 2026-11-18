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
