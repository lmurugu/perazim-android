# APP-CLAIM-03-R security remediation evidence

Status: implementation and synthetic regression evidence only. This note does
not certify a deployment, legal compliance, encryption at rest, or pilot
acceptance.

## Database test destination guard

Phase B synthetic startup now parses and validates the complete PostgreSQL URL
before constructing a `pg.Pool`. Query strings and fragments are refused;
destination host must be an explicit loopback IP, port must be explicit, the
database must use the generated `perazim_phaseb_<random>_test` name, and the
user must be `postgres`. The pool receives explicit host/port/database/user
options, so `PGHOST`, `PGHOSTADDR`, `PGPORT`, `PGDATABASE`, `PGSERVICE`, and
`PGOPTIONS` cannot redirect this connection. The runner requires an owned
temporary cluster and refuses every externally supplied database URL before
connecting. After a read-only identity query, startup compares the database,
loopback address, port, and exact cluster data directory before applying
`schema.sql`.

This is a test-harness boundary, not a general production database allowlist.
The process that creates the disposable cluster is trusted to supply its
expected directory and port. PostgreSQL owners and superusers remain outside
the protection offered by application triggers and test guards.

## Android transport policy

The Android API client requires HTTPS for every `/api/auth` route, administrator
and membership route, permission lookup, and every request that carries a
bearer or privileged-session token. Community media bearer downloads and the
AI relay app-token request also reject non-HTTPS destinations. HTTP redirects
are disabled for these requests. Cleartext loopback is accepted only in the
`validation` build type; other HTTP destinations are rejected.

The Android manifest still declares `usesCleartextTraffic="true"`. The manifest
is being edited in the separate avatar worktree and was not changed in this
remediation. That platform setting permits cleartext for code paths that do not
apply this client policy. Review and resolve that ownership conflict before
making a broader app-wide cleartext claim.

The API process itself does not terminate TLS or reject plaintext requests.
The Caddy configuration in `server/deploy/` is prepared to terminate HTTPS and
proxy to the API, but `server/deploy/README.md` says it is not deployed. No live
certificate, public endpoint, redirect behavior, or mobile-data reachability
was verified here. HTTPS enforcement is therefore evidenced at the updated
Android client boundary only, not at the server or deployment boundary.

## Verification history protection

Migration `0012_membership_verification_history_append_only.sql` adds triggers
that reject ordinary `UPDATE`, `DELETE`, and `TRUNCATE` operations on
`membership_verification_history`; inserts remain allowed. It does not rewrite
or delete historical rows. The persistence test checks the exact migration,
function, and trigger catalogs and verifies all three rejected operations
against a synthetic row. A database owner or superuser can alter or disable a
trigger, so this is an integrity control, not permanent retention or proof of
complete audit coverage.

## Storage and backup claim limits

- Android session tokens are encrypted with AES-GCM using an Android Keystore
  key and are bound to account and server origin. Backup rules exclude the
  token preference files.
- The Room database is a standard Room database; no SQLCipher or equivalent
  database encryption is configured. The backup rules do not exclude the Room
  database.
- The prepared Compose stack declares a PostgreSQL volume but no database or
  host-disk encryption setting. No hosting evidence was available.
- The deployment README describes operator backup steps and recommends
  encrypted off-host copies and restore checks. No scheduled backup job,
  archive encryption, access-control evidence, retention evidence, or restore
  evidence was verified in this task.

Do not generalize the Keystore-protected session token to a claim that all
Android data, PostgreSQL data, uploaded files, or backups are encrypted.

## Synthetic verification performed

The isolated Phase B runner created and removed its own temporary loopback
PostgreSQL cluster and generated `_test` databases. The full runner exercised
SEC-01 cross-process rate limiting, migrations and persistence, membership and
administrator workflows, current-authority checks, legacy API contracts,
community API contracts, and process-restart persistence. The Android
`testDebugUnitTest` task passed after testing the transport policy through a
JVM-only helper. These results are test evidence only; they do not establish
production hosting, TLS, backup, encryption-at-rest, or institutional
approval.
