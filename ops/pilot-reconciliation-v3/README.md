# Pilot reconciliation v3 (operator scripts and scratch tests)

Review material for migrations 0022 and 0023 on perazim_pilot. Nothing here has been run on the pilot. Do not run it against the pilot without the owner's explicit approval, and only in the order below.

## Contents
- `pre-flight.sql`: read-only checks (server version, notice rows, acknowledgements per version, ledger).
- `backup-pilot.sh`: fresh custom-format dump before any change, verified with `pg_restore --list`, with a SHA-256 file.
- `reconcile-pilot-privacy-notice.sql`: the reconciliation step. No BEGIN/COMMIT. It only runs inside `reconcile-and-migrate.js`, or with `psql -1`. It sets `is_active = FALSE` on version `0.1-TEST` and changes nothing else.
- `reconcile-and-migrate.js`: one transaction holding the runner's advisory lock (key checked against `server/src/migrations.js`). It calls the runner's own `verifyHistory` before any write, requires the ledger to be exactly 0001–0021, requires the pending set to be exactly 0022 and 0023 with git HEAD checksums, then runs the reconciliation, the tracked 0022 and 0023, the ledger rows and post-checks. COMMIT only if all pass; otherwise ROLLBACK.
- `recover-from-backup.sh`: restores the pre-change dump into a NEW database. It never overwrites the damaged one.
- `scratch-env.js`: shared settings for the tests (see below).
- Scratch tests (need a throwaway Postgres; they never touch the pilot): `test-atomic.js` (rollback, commit, recovery), `test-ledger-drift.js` (tampered ledger rows refused, database unchanged), `test-disk-drift.js` (changed, renamed, missing or extra tracked files refused before any write), `http-check.js` (notice endpoint and gate SQL behaviour).

## Running the scratch tests
Requirements: Node with `pg` and `express` installed in `server/` (`npm ci --omit=dev`, or a `node_modules` link), and a throwaway Postgres 16 on `SCRATCH_PG` (default `postgresql://postgres@127.0.0.1:55555`). Set `PRE_CHANGE_DUMP_FOR_TESTS` to a dump made by `backup-pilot.sh` for the ledger and disk tests. Each test creates and drops its own `perazim_*` databases on the scratch server only.

```
node ops/pilot-reconciliation-v3/test-ledger-drift.js
node ops/pilot-reconciliation-v3/test-disk-drift.js
node ops/pilot-reconciliation-v3/test-atomic.js V1
```

## Pilot procedure (after the owner's approval)
See the commands in the review (REVIEW-v3.md, attached to the thread). In short: pre-flight, fresh backup, stop the API, `reconcile-and-migrate.js`, post pre-flight. Recovery: `recover-from-backup.sh <dump> <new database>`, then decide which database the API uses.

## Limits
- Tests use the tracked migration files from this branch. The pilot's real state is not known to these scripts until `pre-flight.sql` has been run there.
- The pilot's server major version must be checked with `pre-flight.sql` (`SHOW server_version`).
- Acknowledgement and registration flows are not exercised by the tests. The gate is checked by its exact SQL.
- Whether 0023 is recorded in other environments is still unknown.
