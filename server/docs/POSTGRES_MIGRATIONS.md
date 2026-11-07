# PostgreSQL migration foundation

The ordered migration directory is `migrations/`. The first file,
`0001_current_schema_baseline.sql`, is a snapshot of the schema currently in
`schema.sql`, including the established campus seed rows for a new database.
Future schema changes belong in a new, ordered SQL file; do not rewrite a file
once its checksum has been recorded.

## Runtime behavior and commands

The API still uses its existing `schema.sql` startup path in this foundation
change. Migration commands are separate from API startup. No migration or
baseline was run against the local application database as part of this work.

With `DATABASE_URL` pointed at the intended database:

```sh
npm run db:migrate -- status
npm run db:migrate -- plan
npm run db:migrate -- migrate --confirm=DATABASE_NAME
npm run db:migrate -- baseline --confirm=DATABASE_NAME
```

`status` and `plan` are read-only, including for untracked databases. `migrate`
only applies pending files to an empty database or a database with a verified
ledger. `baseline` is a separate explicit operation for an untracked existing
database. It compares all public relations, columns and their types/defaults,
constraints, indexes, sequence definitions, triggers, and row security policies
against a temporary schema built from the
baseline migration. It records the baseline checksum only on an exact match; it
does not execute the baseline SQL against user tables and does not alter their
rows. Any extra or missing schema object causes it to stop. The temporary
comparison schema and ledger write run under one transaction and advisory lock.
The adoption check covers public relation/column and behavior metadata used by
this schema; it does not compare unrelated database-wide roles, extensions, or
routines, ownership, or grants. Existing schemas with extra or altered public
tables, columns, views, triggers, policies, indexes, constraints, or sequence
definitions fail the comparison.

Both write commands require the exact database name as a command-line
confirmation and, in this foundation release, accept only names ending in
`_test` over a local connection (loopback or a Unix socket under `/tmp`). They
refuse `NODE_ENV=production`. An operator must take and verify a
backup before any future production migration process is separately approved.
Do not run these commands against the currently configured app database.

The ledger stores migration version, filename, SHA-256 checksum, and apply
time. A checksum/name mismatch, unknown ledger version, history gap, unsafe
untracked schema, or SQL failure stops the process. SQL and ledger changes are
applied in one transaction while holding a PostgreSQL transaction advisory
lock, so a failed file rolls back its partial DDL and ledger changes.

## Backup and restore

Before a future approved migration, take a PostgreSQL custom-format dump and
verify that it can be restored into a separate disposable database. Preserve
uploads separately; this database dump does not contain files. Example commands:

```sh
pg_dump --format=custom --file=perazim-before.dump "$DATABASE_URL"
pg_restore --list perazim-before.dump
pg_restore --no-owner --dbname="$RESTORE_TEST_URL" perazim-before.dump
```

Do not put connection strings, dumps, or member data in the repository.

## Disposable integration test

The migration test intentionally uses a separate `MIGRATIONS_TEST_ADMIN_URL`
targeting the local PostgreSQL maintenance database named `postgres`. It ignores
`DATABASE_URL`, creates unique names ending in `_test`, and drops only those
three names when finished. The admin role needs `CREATEDB` privileges. Never
point it at a remote or production database. The test exercises empty-database
migration, repeated migration, read-only status/plan in a read-only transaction,
strict baseline adoption without changing representative user/community rows,
checksum drift refusal, transaction rollback after partial SQL, and custom dump
restore of the fixture data.

```sh
MIGRATIONS_TEST_ADMIN_URL='postgres://local_role@127.0.0.1:5433/postgres' npm run test:migrations
```
