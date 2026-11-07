# PostgreSQL migration foundation test report

**Run date:** 2026-11-07 (Africa/Nairobi)
**Scope:** PostgreSQL migration infrastructure only. Tests used temporary local PostgreSQL clusters under `/tmp` and uniquely named databases ending in `_test`. No configured app database, `perazim_test`, `perazim_local`, or running API was used or changed.

## Focused migration safety run

- Test source: [`test/migrations.test.js`](../test/migrations.test.js)
- Runner source: [`src/migrations.js`](../src/migrations.js)
- Command: `npm run test:migrations`
- Captured output: `/tmp/perazim-migration-foundation-final-test.log`
- Result: **passed**. The final run created and dropped only `perazim_migration_source_125423_5a442ae59d_test`, `perazim_migration_restore_125423_5a442ae59d_test`, and `perazim_migration_rollback_125423_5a442ae59d_test`.

The focused suite covers nine scenario groups:

1. CLI write guards reject non-`_test` targets, nonlocal hosts, and production mode before connecting.
2. `status` and `plan` succeed inside a PostgreSQL read-only transaction.
3. Empty-database ordered migration applies once and the second run is a no-op.
4. Representative fixture rows cover members with zero and nonzero grace balances, prayers and amens, messages, connections, sync events, revoked tokens, community posts and comments, groups and memberships, classes, media ownership, event RSVPs, notifications, and challenge records.
5. A schema mismatch with an extra table makes baseline adoption fail while leaving the extra row intact.
6. Exact baseline adoption preserves fixture rows, columns, types/defaults, generated/identity metadata, relation metadata, constraints, indexes, sequence definitions, triggers, and row-security policies; a second baseline attempt is refused without changing rows.
7. An edited ledger checksum is detected and rejected.
8. A deliberately failing second migration rolls back its partial DDL and ledger creation.
9. `pg_dump`/`pg_restore` into a separate disposable database preserves the rows, migration ledger, and structural metadata.

## Existing backend regression runs

- `npm test` passed on a temporary PostgreSQL cluster and a new unique `_test` database: **19** server API contract checks, the PostgreSQL API contract suite, and **11** community API checks all passed.
- `npm run test:restart` passed on a separate unique `_test` database after the current schema was loaded into that disposable database. It verified account, prayer, amen, and testimony persistence across two Node process lifetimes.
- The first restart attempt used a blank disposable database and failed before starting the server because that test truncates its expected tables before startup (`relation "users" does not exist`). No app data was involved. I then initialized `schema.sql` in a new disposable database and reran the restart test successfully.

The regression-suite outputs were observed in the run session; only the final focused migration output was retained as a file under `/tmp`. These checks did not restart the configured local backend. `server/src/db.js` and `server/schema.sql` remain unchanged, so existing startup behavior is preserved.
