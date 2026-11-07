# Phase A foundation baseline — 7 November 2026

Initial branch: `phase-B-community`.
Initial commit: `6fe877ccfd06203d0447292833febde414e8d6c6`.
Initial tracked changes: none. The specification directory
`docs/PERAZIM_AI_PROJECT/` was untracked before this work and must remain untouched.

## Existing application and storage

- Single native Java Android application, namespace `org.perazimchurch.app`.
- Android minimum SDK 24, target/compile SDK 34; Java 11 source compatibility.
- Existing debug, release and isolated `.validation` build variants.
- Room runtime schema v6; existing explicit migrations 4→5 and 5→6.
- Schema export initially disabled; destructive fallback initially enabled.
- Historical source also contains v3. Source history alone cannot establish which
  versions are installed on every user's phone. Do not assume v1–v3 upgrade
  support or remove fallback without that inventory and verified paths.
- Current bundled asset is SQLite user_version 5, with Room identity
  `901620a8b52cf9a642e37cd23d3c8564` and zero user rows.
- Asset SHA-256 before Phase A:
  `77d83a182fbb6e8a8fdabe3e83f4c2071ca2308f7a8f5d11b1e2b5571436dcf9`.
- Asset verse counts: KJV/NIV/NKJV/SUV 31,102 each; ESV 31,103.
  These counts establish a preservation baseline, not licensing or textual accuracy.
- Existing Express/PostgreSQL schema is applied from `server/schema.sql` at startup;
  ordered migrations and a version ledger did not exist at this baseline.
- Existing AI runtimes, authentication, community, classroom, media and sync
  features are outside Phase A feature-development scope.

## Test and recovery baseline

Existing tests cover Android pure-Java behavior, API contracts, PostgreSQL,
community authorization, classroom signaling/browser media, Worker history and
database-test safety. Existing tests are not evidence of full migration coverage.
No Android migration instrumentation infrastructure or exported Room schema
history was present at the initial baseline.

Backup commands are documented in `server/deploy/README.md`; documentation alone
does not establish a successful restore. Foundation recovery tests must create
and restore synthetic data in separately named disposable databases, not the
running member database or another task's fixtures.

## Specification filename check

Both `11_TESTING_AND_EVALUATION.md` and `11 TESTING AND EVALUATION.md` already
existed at Phase A start. Both are 43,267 bytes with identical SHA-256:
`1080afd6af04980a35b0b8ccdb2e2a7e4af1164c891dac4397823dd4554075da`.
The master references the canonical underscore filename, which is now present.
No rename/deletion is necessary; both user-provided files are preserved.

## Evidence rules

See the Phase A report for tests actually executed and unresolved limits.
Do not equate generated schema fixtures, successful fresh creation or source
inspection with a proven upgrade of every historical user installation.
