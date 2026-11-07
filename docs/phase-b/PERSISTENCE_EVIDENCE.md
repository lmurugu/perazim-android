# B2 persistence evidence

## Scope and status

B2 adds the membership and permission persistence layer in migration
`server/migrations/0002_membership_permissions.sql`. It is additive: the
pre-existing application tables are not altered. The isolated persistence test
source is `server/test/phase_b_persistence.test.js`, with synthetic existing
application rows from `server/test/phase_b_fixture.js`.

B2 persistence was run successfully on PostgreSQL 18 and PostgreSQL 15. Each
run used a local isolated cluster and generated `perazim_phaseb_*_test`
databases only. The source, restore, and rollback databases were dropped by the
test. No configured app database, member database, or backend service was used.

## Exact commands and results

PostgreSQL 18 used the repository's extracted runtime tools and an owned
throwaway cluster on loopback port 55472:

```sh
PHASE_B_TEST_ADMIN_URL='postgres://postgres@127.0.0.1:55472/postgres' \
  node server/test/phase_b_persistence.test.js
```

**Result:** passed. The run reported:

```text
Phase B persistence passed on isolated databases
perazim_phaseb_source_169536_b15b6401be_test,
perazim_phaseb_restore_169536_b15b6401be_test,
perazim_phaseb_failure_169536_b15b6401be_test.
```

The temporary PostgreSQL 18 cluster was stopped and its temporary directory
removed after the run.

PostgreSQL 15 used the separate isolated local test cluster and matching
PostgreSQL 15 dump/restore wrappers. The admin URL is intentionally not recorded
here because it contained the test role's synthetic password.

```sh
PHASE_B_TEST_ADMIN_URL='<local isolated PostgreSQL 15 test admin URL>' \
PHASE_B_PG_BIN='/tmp/perazim-phaseb-pg15-tools' \
  node server/test/phase_b_persistence.test.js
```

**Result:** passed. The run reported:

```text
Phase B persistence passed on isolated databases
perazim_phaseb_source_169623_b923a9717a_test,
perazim_phaseb_restore_169623_b923a9717a_test,
perazim_phaseb_failure_169623_b923a9717a_test.
```

The root-owned PostgreSQL 15 cluster remains under its owner’s cleanup control.
The test dropped its three generated databases.

## Coverage exercised by the B2 test

- Applies the 0001 baseline and then 0002 against a synthetic application
  database containing representative existing users, balances, prayer, message,
  connection, sync, revoked-token, group, post, class, media, notification, and
  challenge rows.
- Compares every pre-existing public table’s rows and catalog metadata before
  and after B2; checks the two synthetic user balances remain 0 and 9.
- Confirms exactly the approved B2 tables and canonical permission catalogue
  exist, and that the migration creates no user permission grants.
- Checks canonical verification states and defaults, a positive revision,
  verified-record assignment uniqueness, valid scopes, and grant expiry order.
- Confirms audit entries reject UPDATE, DELETE, and TRUNCATE while retaining the
  original row.
- Re-runs migrations and checks the stored 0002 checksum remains unchanged.
- Attempts B2 followed by a deliberately failing synthetic 0003; verifies the
  B2 tables and failed migration DDL roll back while the earlier 0001 ledger
  entry and pre-existing user data remain intact.
- Restores a custom-format dump into a separate disposable database and compares
  table rows and catalog metadata, including the append-only audit trigger.

## PostgreSQL 15 dump/restore compatibility note

An earlier cross-version attempt used PostgreSQL 18 `pg_restore` against a
PostgreSQL 15 server. The restore stopped because the newer client emitted
`SET transaction_timeout`, which PostgreSQL 15 does not recognize. This was a
client/server version mismatch, not a schema or data failure. Re-running B2
with PostgreSQL 15 matching `pg_dump` and `pg_restore` wrappers passed, as
recorded above.

## Reproducible Phase B runner

`scripts/test-phase-b-isolated.sh` owns a new temporary PostgreSQL 18 cluster by
default, chooses a free loopback port, and creates separate generated test
databases for the legacy suite and process-restart suite. The restart database
is initialized from the existing `schema.sql` before its persistence test. The
runner also supports an explicitly supplied local `PHASE_B_TEST_ADMIN_URL` and
matching `PHASE_B_PG_BIN` for isolated cross-version checks. It ignores any
inherited `DATABASE_URL`, `PHASE_B_SYNTHETIC`, and `PHASE_B_ADMIN_KEY`; B3 test
children configure their own synthetic fixtures. It sets the extracted
PostgreSQL library/search paths and does not read local backend environment
files. It runs the B3 security and authority suites as separate steps.

The runner has not been executed in this report: Phase B acceptance requires
the B5 gate. Once B5 is cleared and `server/test/phase_b_security.test.js`
exists, run:

```sh
scripts/test-phase-b-isolated.sh
```

The default requires both B3 suites. `--allow-missing-b3` is available only
for preparatory validation before they are present.
