# Phase E functional readiness record

This record describes local source evidence only. No production endpoint, database, provider credential, external spreadsheet URL, member record or live administrator was used.

| Workstream | Status | Current evidence and remaining dependency |
|---|---|---|
| Online Gemini | PARTIALLY RESOLVED | Express `/api/ai/online` now derives identity from the authenticated session, checks current verified membership and an explicit active permission, rejects client identity/context substitution, and has a mockable signed Worker provider. The Worker source now accepts short-lived Express grants and minimal question data. All runtime enablement gates remain off. There is no AI permission in the catalogue, no atomic Express quota service, no one-time grant replay protection, no privacy-reviewed pastoral-data safeguard, no approved source retrieval, and no verified deployed Worker/configuration or provider model/terms. |
| Offline Qwen | BLOCKED — DEPENDENCY REQUIRED | Model execution and download remain behind `OFFLINE_QWEN_ENABLED=false`. Local seed and Bible assets exist, but no approved signed public knowledge package, trusted package-signing public key, withdrawal/revision manifest, or completed evidence retrieval path was found. The model's current pinned size/hash does not establish its approval or license. |
| Church information and feeds | PARTIALLY RESOLVED | Local seed assets cover announcements, events, hymns, riddles, jokes, sermons, reflections and Bible verses. They contain no approval/publication metadata establishing institutional approval. All eight remote feeds remain disabled; their content/ACL has not been retrieved or verified. |
| Administrator | RESOLVED — OPERATIONAL VERIFICATION PENDING | Existing synthetic-only enrollment and recovery remain restricted to synthetic disposable environments. The runbook preserves the approved capability set. No real account mapping, production database, secret manager or controlled enrollment environment exists here; no real account was activated. |
| Registration and privacy | RESOLVED — OPERATIONAL VERIFICATION PENDING | Notice 0.1 remains the active DRAFT database entry; notice 0.2 is a review draft. Public routes and registration fail closed unless a current approved publication is established. Server binds presentation time; acknowledgement text/version/hash contract tests are present. `POST /api/privacy/requests` accepts access, correction and erasure intake and status lookup, but does not complete fulfillment; objection, restriction and portability are not implemented as exercisable request types. The retention tables have no approved populated schedule and there is no scheduled disposal or backup-expiry verification. Publication and public registration remain institutionally gated. |
| Migration 0007 | PARTIALLY RESOLVED | The primary migration API, CLI migrate command and owned isolated Phase B/security/admin runners now reject a plan containing 0007 before connection or cluster creation. A no-database independent test mode is provided. PostgreSQL suites whose full plan includes 0007 were not run. The Antigravity audit worktree has an active untracked audit runner/test set and was not edited; its ownership-specific runner requires a coordinated preflight update before that workspace is safe to run. No database received 0007 during this task. |
| Android runtime | BLOCKED — DEPENDENCY REQUIRED | Android source still gates both AI paths off. No emulator/device runtime test was possible in this environment; previous ADB access failed with `Operation not permitted`. No live service was contacted. |

## Migration test omission

The default migration plan contains `0007_disable_contact_list_verification.sql`, so the full Phase B persistence, workflow, membership, admin, security and migration integrations were not executed. `src/migrations.migrate()` and the owned shell runners refuse that plan before connection or database/cluster creation. Database-free unit/security/contract suites may run through `scripts/test-phase-b-isolated.sh --independent`. The independent `test:migrations` harness uses a separately generated migration directory and is not full project migration coverage.

The migration prohibition remains absolute: do not run the full migration suite or any runner that selects the default plan until a safe plan without 0007 is separately established. Do not edit or execute the audit-owned `phase_b_persistence.test.js` directly.

## Local content and eight feed categories

The existing runtime gates for all eight remote feeds remain off. No spreadsheet URL was opened and no sheet content was fetched.

| Feed category | Local equivalent found | Readiness |
|---|---|---|
| Announcements | `src/main/assets/seed/announcements.json` | Local seed exists; no approval/version metadata was found. |
| Events | `src/main/assets/seed/events.json` | Local seed exists; no approval/version metadata was found. |
| Hymns | `src/main/assets/seed/hymns.json` | Local seed exists; no approval/version metadata was found. |
| Riddles | `src/main/assets/seed/riddles.json` | Local seed exists; no approval/version metadata was found. |
| Jokes | `src/main/assets/seed/jokes.json` | Local seed exists; no approval/version metadata was found. |
| Sermon catalogue | `src/main/assets/seed/sermons.json` | Local seed exists; no approval/version metadata was found. |
| Bible/daily verses | `src/main/assets/bible/starter_verses.json`, `kjv_verses.json`, and `kjv_manifest.json` | Local material exists; translation licensing and approval provenance still need verification. |
| App configuration | `docs/sheet-template/config.csv` is a template only | No approved runtime configuration feed replacement was identified. |

The remote feed code already returns an unavailable message and keeps local data usable. Do not treat seed presence as evidence of institutional approval.

## Verification evidence

- `npm run test:phase-e-independent` — exit 0; eight database-free test files passed: privacy publication gate, acknowledgement contract, synthetic admin enrollment-file handling, database destination/startup guard, migration 0007 policy/API guard, migration runner preflight, online AI request/eligibility contract, and Worker signed-service-auth contract. No PostgreSQL connection was used; no migration was applied.
- `npm run test:migration-0007-guard` — exit 0; the direct migration API test proves refusal before `pool.connect()`, and the runner regression confirms all three owned isolated runners invoke the guard before `mktemp`, `initdb` or `createdb`. A direct runner probe passed earlier; nested process startup became intermittent under this environment, so the retained test checks ordering deterministically.
- `npm run test:migrations` was reviewed as safe: it applies only a generated directory containing baseline 0001 and a synthetic rollback fixture, with no 0007. An owned PostgreSQL 18.6 cluster was initialized under `/tmp` with a Unix socket and no TCP listener, but `pg_ctl` could not start it, so the suite itself did not run. The disposable cluster directory was removed and no PostgreSQL process remained; no migration was executed.
- `npm run test:unit` was attempted after source inspection established it uses only a synthetic memory store, but exited 1 because this environment denies binding `127.0.0.1:3001` (`listen EPERM`). It did not reach a database. This is not a pass.
- The offline Android unit command `gradle --no-daemon testDebugUnitTest --offline --console=plain` exited 1 before the build began because Gradle could not load `libnative-platform.so`. No Android unit tests ran.
- APK inspection: `build/outputs/apk/release/app-release.apk`, package `org.perazimchurch.app`, release variant, version 2.2.0 (code 4), SHA-256 `23c2b3255a8e5ab0a6d5591d81c70f4f27cad2b5c32fa3a808800b15d40f5769`. Signature and manifest checks passed; 20 default avatar assets matched source; credential/configuration markers were absent; one bundled SQLite database was checked; privacy assets remained labelled DRAFT. This is an INTERNAL QA artifact, not a functional RC.
- `adb devices -l` was attempted once; the local ADB daemon could not bind port 5037 (`Operation not permitted`). No device/emulator runtime test or installation occurred.

No migration 0007 was applied in this task. The earlier disclosed migration violation remains a historical incident and is not represented as undone by this guard.
