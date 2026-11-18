# ADR 0004: Production migrations are applied only through the backed-up CLI

Status: Accepted (2026-11-18)

## Context
The server applied pending migrations at every boot, including production. A migration that changes live member data could then run with no backup. The CLI (`scripts/db-migrate.js`) already takes a `pg_dump` and requires confirmation, but it was bypassed on the live pilot path.

## Decision
- **Production boot never changes data.** When `NODE_ENV=production`, the server checks the migration plan (read-only) and refuses to start if anything is pending or the history is not clean (`assertNoPendingMigrations`, `server/src/migrations.js`).
- **The CLI is the only way to apply migrations to a real database.** It requires `--confirm=<database>` and `MIGRATE_CONFIRM=<database>` for any database whose name does not end in `_test`, and it writes a `pg_dump` before any change.
- **Development and disposable test databases** still apply migrations at boot, so local work and the isolated test suites are unchanged.

## Consequences
- A new migration on a live system needs the CLI step before the new code starts. That is deliberate.
- The pilot database's name ends in `_test`, so the CLI's extra confirmation does not apply to it. Renaming the pilot database or treating it as production is an owner decision and is not changed here.
- Tests: `server/test/migrations.test.js` covers the CLI confirmation and the boot check.
