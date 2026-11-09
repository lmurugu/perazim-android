# DATA-01 implementation report

**Status: partially completed.** The registration notice gate, public document access, CSV packaging protection, CSV relocation, and a debug APK are implemented and locally verified. PostgreSQL migration execution, Phase B regression suites, device-level UI verification, single-record administrator entry, legal approval, and the initial real-register import remain outstanding.

## Repository and concurrency

Work is in `/home/murugu/Android/app`, branch `phase-B-community`, based on HEAD `93e24bc1232cf2c5a21b8107459460dc8899fe36`. No commit, push, or merge was made. The audit thread was idle during preflight, but it had uncommitted changes in the shared checkout. Its backend security, membership routes, tests, migrations 0003/0004 and scripts were preserved. The existing untracked `docs/PERAZIM_AI_PROJECT/` specifications were also preserved.

## Documents

The supplied archive was present and passed archive listing. All ten editable DOCX files and its README are retained, unchanged, in `working-drafts/originals/`. The public policy and app notice have Markdown renditions in `public/`; the app notice text is shared byte-for-byte with the server source and Android public asset. Internal procedures and the DPIA remain in the project documentation and are not app assets or public API responses. All supplied content remains marked draft v0.1, with placeholders intact.

The official ODPC framework page, Act, and 2021 regulations were checked. The ODPC FAQ currently lists faith-based or religious institutions among categories not exempt from registration. PMCI's specific position still requires confirmation with ODPC or Kenyan counsel. This implementation does not claim legal compliance or approval.

## Registration privacy gate

The Android account form fetches the current server notice and displays its version and draft status. A person must check an initially empty acknowledgement control; they can open the full notice and public policy before registering. The exact version, SHA-256, and server-issued presentation timestamp are sent to Express. The server rejects missing or stale acknowledgements before account creation and records the presentation and acknowledgement times in the same PostgreSQL transaction as the account. A draft notice cannot be used for production registration; production stays closed until an approved notice is published. No optional consent is bundled with acknowledgement.

## Membership database and member access

The current Phase B source already includes server-side membership records, administrator permissions, imports, corrections, claim review and audit history. The DATA-01 migration records notice versions and acknowledgements. Another migration demotes any legacy contact-list-based verification and tightens the verification constraint. New registrations are always `REGISTERED`; phone matching no longer runs in the registration route, and the legacy contact loader returns no records.

The existing administrative screen supports record review, import and correction. Its import screen is explicitly limited to synthetic test records; there is not yet a dedicated one-person add form. A member's existing authenticated profile remains self-scoped. No full membership register was added to Room, AI prompts, offline knowledge, or app responses.

The new migrations were not applied to any PostgreSQL database. The existing Phase B migration and admin interface changes in the working tree also remain uncommitted and unverified against PostgreSQL. No real-member import or administrator activation occurred.

## Real CSV protection

The CSV was verified as a regular 2,667-byte file without opening or printing its rows. Its SHA-256 was recorded in the owner-only preflight report. At the current DATA-01 instruction, the file was moved byte-for-byte to `/home/murugu/Android/private-member-data/member_contacts.csv`; the directory is mode `700` and file mode `600`, and the hash matched after relocation. The repository asset path is absent and ignored if recreated. At-rest disk encryption was not verified, and no extra CSV backup was made.

The debug APK was opened as an archive. It contains the two public privacy documents and no member CSV or similarly named contact CSV. The Gradle source-set exclusion and explicit merged-asset filter both protect future APK builds.

## Testing and APK

- `npm run test:privacy`: passed 18 assertions covering the current notice, missing/stale acknowledgement, production draft block, presentation/acknowledgement timestamp binding, and unverified account creation using synthetic data and the in-memory store.
- `node test/member_contacts.test.js`: passed all 5 synthetic-only assertions; the legacy loader reads no file and cannot verify an account.
- `npm run test:unit`: passed all 85 assertions across the 19 API contract checks against the suite's transient in-memory store.
- `./gradle-8.5-all/gradle-8.5/bin/gradle testDebugUnitTest assembleDebug --offline --no-daemon`: successful; 67 Android unit tests across 13 suites, 0 failures, 0 errors, 0 skipped.
- APK archive integrity passed; the membership CSV was absent from the final APK.
- `npm run test:migrations` could not run because `MIGRATIONS_TEST_ADMIN_URL` is not configured. No PostgreSQL tests or Phase B security audit suites were run. No Android device or emulator was connected, so the native screen was compiled but not exercised on-device.

APK: `/home/murugu/Android/app/build/outputs/apk/debug/app-debug.apk`
SHA-256: `a3b4c409d516bb933757aca47447c205ce22b8056112d0ea8abd1e0f1f3f4c10`

## Backups

A pre-change source backup is at `/home/murugu/Android/backups/perazim_DATA-01_prechange_20261108_224332_EAT.tar.gz`. Its SHA-256 sidecar and gzip integrity check passed. It excludes the private CSV, Git metadata, local secrets, local databases, and ignored build artifacts.

The prior final archive is `/home/murugu/Android/backups/perazim_DATA-01_final_20261108_231838_EAT.tar.gz`. After Addendum 2 changes, a refreshed archive was created at `/home/murugu/Android/backups/perazim_DATA-01_addendum2_20261108_233830_EAT.tar.gz`; its SHA-256 is `ce14e77a052608d6849f2acce2bc41e062339b0b51c7008668036edf0b74266d`. A final archive was also captured after this report text was saved. Its matching archive, manifest and SHA-256 sidecar are in `/home/murugu/Android/backups/`. The archives exclude the private CSV, Git metadata, secrets, local databases, build output, caches, screenshots and logs. Archive integrity, an isolated restoration of all 829 files, and CSV absence were verified.

## Remaining requirements and next action

Before a controlled pilot, finish the AUD-01/AUD-02 work and run all Phase B suites against a named disposable PostgreSQL database; apply and verify migrations 0006/0007 there; add a single-member administrator form; test the native registration screen on a designated emulator; resolve the notice placeholders and publish an institutionally approved version; confirm ODPC registration, lawful basis, sensitive-data and child-data handling, retention, vendor and cross-border arrangements; and conduct a separate reviewed real-data import. No production deployment or pilot readiness is claimed.

**Next action:** provide the isolated PostgreSQL test admin URL after the security-audit changes are reconciled, so the full migration and permissions suites can be run without touching the live member database.

## Addendum 2 — CC-REV-01 rectifications

This section records implementation evidence separately from institutional decisions and real-data operations. No real register, member photograph, or production database was accessed for these changes. No migration was applied, no approval or publication was recorded, and no production operation was performed.

### Public-policy publication control

**Source implementation:** `GET /api/privacy/notice/current` now returns an explicit 503 unavailable response unless the current notice is published, effective, and not withdrawn. `GET /api/privacy/public-policy` also fails closed, verifies the stored SHA-256 before returning content, and confines file-based publication to `docs/data-protection/public/`. It does not offer an administrative draft preview. Migration 0006 still seeds the app notice as DRAFT.

**Codex test evidence:** synthetic in-memory fixtures cover absent, DRAFT, PUBLISHED, and withdrawn notice and public-policy states. Only the synthetic PUBLISHED effective documents are returned. The exact content hash is checked.

**Limitations:** there is no `privacy_policy_versions` PostgreSQL migration in the current tree. Consequently, PostgreSQL public-policy requests fail closed until an approved publication schema and workflow are supplied. Notice publication is also not workflow-controlled beyond the existing version/status row; no approver, approval date, or institutional publication is invented. The registration route checks publication status only in production; in development/test it can accept the static draft with a matching acknowledgement. That shared route is currently under active work in another session and was not changed here, so this is an open defect.

**Institutional approval:** none. The v0.1 documents remain drafts and have not been published.

### Existing-account re-acknowledgement

**Source implementation:** no version-change re-acknowledgement enforcement, account gate, or recovery flow is implemented. Existing accounts are not marked outdated by a new notice, and the current registration acknowledgement does not satisfy this requirement.

**Codex test evidence:** none for existing accounts or notice-version changes.

**Dependency:** implementation requires coordinated changes across shared authentication, account/profile and Android entry points; those routes are currently being changed in parallel. No account is retrospectively marked as acknowledging a notice.

### Optional consent

**Source implementation:** no optional purpose is configured and no optional-consent API or record exists. Notice acknowledgement remains separate from optional consent. No purpose or wording was invented.

**Institutional approval:** PMCI has not supplied an approved optional purpose, wording, lawful basis, withdrawal effect, or processing path. Consent remains unconfigured pending those decisions.

### Retention, disposal and data-subject requests

**Source implementation:** retention periods, category/purpose rules, review triggers, legal holds, dry-run reporting, controlled disposal, access/correction/erasure request workflow, eligibility checks and request audit are not implemented. No automatic deletion is scheduled. Existing append-only acknowledgement history has no operational disposal process and must not be treated as permanent retention approval.

**Codex test evidence:** none; no retention or DSAR technical behavior is claimed.

**Institutional approval:** retention periods, lawful holds, exceptions, request identity checks and disposal policy require PMCI and legal review before real records can be affected.

### Shared, duplicate, missing and ambiguous contact data

**Source implementation:** migration 0008 adds a restricted staging table and append-only staging events. The importer classifies missing or malformed values, duplicate references, shared phones, possible duplicate people, and conflicting identities. Complete unambiguous rows enter operational records; incomplete or ambiguous rows remain staged. Identical batches are idempotent using a keyed HMAC fingerprint. Non-test environments must configure a stable `DATA01_IMPORT_FINGERPRINT_KEY`; the synthetic test key is available only when `NODE_ENV=test`. A step-up administrator can inspect staging and explicitly promote corrected data after conflict review. Promotion creates an unverified operational record; it does not link or verify an account. Registration continues to create only `REGISTERED` accounts and does not consult contact data.

**Codex test evidence:** `node test/membership_import_classification.test.js` covers name-only staging, malformed optional phone, shared phone, duplicate reference, possible duplicate person, existing conflicts, and deterministic synthetic batch fingerprints. The Phase B workflow test was extended for staging, idempotent re-import, promotion, and legacy reconciliation but could not be run because `PHASE_B_TEST_ADMIN_URL` is not configured. The migration was parsed and the migration sequence was checked for gaps; the migration was not applied.

**Real-data operation:** no CSV or notebook photograph was inspected, parsed, imported, or used for testing.

### Legacy identity reconciliation and phone changes

**Source implementation:** migration 0009 adds separate user-submitted reconciliation requests, immutable proposal revisions/events, identity history, and source metadata. A member can submit or correct a proposal; a step-up pilot administrator can approve, reject, or request correction against an authorized active record. Approval writes only the trusted identity snapshot and history. It does not create or change a membership-verification link. No automatic backfill from mutable display names is performed. No phone-change feature is enabled or implemented; a safe verified-link-aware phone-change lifecycle remains outstanding.

**Codex test evidence:** the synthetic Phase B workflow test now covers a legacy account whose editable display name differs from the proposed identity, neutral applicant response, administrator-only review, successful snapshot reconciliation, and continued `REGISTERED` status. PostgreSQL-backed execution is pending the disposable test admin URL. No phone-change behavior is implemented or claimed.

### Registration error-log redaction

**Source implementation:** registration failure and membership-status persistence events in `server/src/routes/auth.js` now log only a generated request ID and a sanitized error category. Raw request values, SQL messages, stacks, and notice content are omitted from those ordinary logs.

**Codex test evidence:** the synthetic privacy test captures a forced registration failure and verifies that its name marker, phone and PIN do not appear in logs or the generic error response.

### Acknowledgement presentation timestamp source

**Source implementation:** registration no longer reads or validates a client-supplied presentation timestamp. `getCurrentNotice()` stamps the notice in the server process, and `recordAcknowledgement()` persists that server-generated value with the acknowledgement. The legacy Android field may still be sent for compatibility, but it has no authority over stored evidence. The timestamp records server processing and is not cryptographic proof that a person rendered or read the notice.

**Codex test evidence:** the synthetic privacy test submits a non-numeric forged timestamp and verifies registration succeeds without using it; the stored value is server-generated and precedes the acknowledgement time.

### APK and AI-boundary verification

**Independent verification (prior to this addendum's server-only edits):** the debug APK at `/home/murugu/Android/app/build/outputs/apk/debug/app-debug.apk` was inspected as an archive; its SHA-256 was `a3b4c409d516bb933757aca47447c205ce22b8056112d0ea8abd1e0f1f3f4c10`. The archive contained the two public privacy assets and no member CSV or similarly named contact CSV. The prior Gradle build completed with 67 Android unit tests passing, 0 failures, 0 errors, 0 skipped. No Android source or asset-packaging configuration changed in this addendum, so the APK was not rebuilt. No full register was found in Room, AI/offline assets, prompts or caches by the earlier source/package inspection; this does not constitute a comprehensive runtime cache audit.

### Tests run for Addendum 2

- `npm run test:privacy` — passed; synthetic in-memory publication-state and registration acknowledgement cases. The revised suite explicitly verifies absent, DRAFT, PUBLISHED and withdrawn publication states.
- `npm run test:unit` — passed all 19 API contract groups against the transient in-memory store after loopback bind permission was granted. The first restricted attempt failed before test execution with `EPERM` binding `127.0.0.1:3001`.
- `node test/member_contacts.test.js` — passed; synthetic-only disabled contact lookup checks.
- `node test/membership_import_classification.test.js` — passed; synthetic-only importer classification and batch fingerprint checks.
- `node --check src/routes/auth.js`, `node --check src/routes/adminMembership.js`, and `node --check test/phase_b_workflow.test.js` — passed syntax checks.
- `npm run test:unit` — passed all 19 API contract groups against the transient in-memory store after loopback bind permission was granted. The first restricted attempt failed before test execution with `EPERM` binding `127.0.0.1:3001`.
- Migration loader — parsed migrations 0001 through 0009 with no numbering gap. PostgreSQL migration execution and Phase B workflow execution were not run because `MIGRATIONS_TEST_ADMIN_URL` and `PHASE_B_TEST_ADMIN_URL` are unavailable. No real database or member data was used. No emulator/device UI checks were run. The earlier Android build and APK inspection evidence predates this server-only change.

### Security-audit overlap and coordination

The audit session handed off `server/src/routes/auth.js`, `server/src/phase-b/memberContacts.js`, and `server/src/phase-b/security.js` as DONE. The security file changes were preserved; no contact lookup or CSV-based verification was introduced. The audit session still owns `server/test/phase_b_persistence.test.js`; it was not edited. PostgreSQL execution remains pending a uniquely named disposable database URL.

## Addendum 2 completion status

**Implemented in source and covered synthetically:** public API fail-closed publication checks, effective-date and withdrawal checks, public-policy content hash/path checks, synthetic publication-state tests, server-owned acknowledgement timestamps, registration log redaction, and pure importer ambiguity classification.

**Implemented but not PostgreSQL-verified:** importer staging/idempotency/promotion and legacy identity reconciliation. **Not implemented or not verified:** PostgreSQL policy publication storage/history, development/test registration publication enforcement, existing-account re-acknowledgement, optional-consent records and withdrawal, retention dry-run and disposal, DSAR handling, phone-change lifecycle, the Addendum 2 legacy/admin/SEC regression matrix, migrations on PostgreSQL, and Android device behavior. Institutional approval and real-data import remain absent.

## Migration ownership confirmation

- `0006_privacy_notice_acknowledgements.sql` — created by DATA-01; versions the app notice and stores append-only acknowledgement evidence.
- `0007_disable_contact_list_verification.sql` — created by DATA-01; demotes legacy `CONTACT_LIST` verification to `REGISTERED` and restricts verification source to `MANUAL`. Never apply it to a real database without a separate authorization.
- `0008_membership_import_staging.sql` — created by DATA-01; adds idempotency keys, staged import rows, and append-only staging events.
- `0009_legacy_identity_reconciliation.sql` — created by DATA-01 in this turn; adds review requests, proposal revisions/events, and identity history without backfilling profile names.

## Addendum 2 implementation checkpoint — 2026-11-09

This checkpoint records source state and synthetic test evidence. Independent verification is still pending Antigravity, and no pilot acceptance is claimed. No real register, public document publication, institution approval, purpose, or retention period was added by this work.

| Requirement | Source implemented | Test evidence (with command) | Independently verified | Pilot accepted | Awaiting PMCI input |
|---|---|---|---|---|---|
| PostgreSQL migrations and migration 0007 review | PARTIAL — migrations 0010 and 0011 apply in order; 0007 is unchanged | PASS: `npm run test:migrations`; PASS: `node test/data01_governance_postgres.test.js`; synthetic 0007 before/after check described below | NO — pending Antigravity | NO | NO for schema; institutional use still awaits separate decisions |
| Public policy publication storage and delivery | PARTIAL — 0010 stores versioned documents/events; publication and withdrawal endpoints require step-up plus `privacy.document.publish`; public errors have one generic shape and content hash is checked | PASS: `npm run test:privacy` checks missing/draft/withdrawn/hash mismatch; unauthorised publication returns 401. No authorized successful publication or previous-version rollback test was run because no real publication reference or grant was supplied | NO — pending Antigravity | NO | YES — publication authority, real approval reference, reviewed content and effective date |
| Registration and existing-account re-acknowledgement | PARTIAL — registration remains REGISTERED/MANUAL; auth middleware returns 428 with selected version/hash; single-use account-bound receipts use server time; Android prompts at sign-in with acknowledge/sign-out choices | PASS: `npm run test:privacy`; PASS: `node test/phase_b_workflow.test.js` (141 assertions); PASS: Android compile and unit tests (67 tests) | NO — pending Antigravity | NO | YES — final notice and publication/material-change decisions |
| Purpose-specific optional consent | PARTIAL — 0010 has an empty purpose catalogue and append-only events; current grant is checked at point of use; no purpose is seeded | PASS: `node test/data01_governance_postgres.test.js` (empty catalogue, foreign-key rejection, grant/withdraw, append-only, authority/status unchanged) | NO — pending Antigravity | NO | YES — purpose, wording, lawful-basis and withdrawal decisions |
| Retention, legal holds and disposal | PARTIAL — 0010 adds DRAFT-default rules, reviews, holds and disposal evidence; synthetic dry-run is read-only; no scheduled job exists. Protected execution workflow is not wired to an administrator endpoint | PASS: `node test/data01_governance.test.js` (no writes, DRAFT, hold, review expiry, eligibility) | NO — pending Antigravity | NO | YES — categories, triggers, durations, exceptions, review dates and disposal authority |
| Data-subject requests | PARTIAL — 0010 adds restricted request/event/revision storage; request/status routes return neutral, requester-scoped results; narratives are ignored and not logged; complete access export and administrator review/response workflow remain absent | PASS: `node test/data01_governance_postgres.test.js` (requester mismatch returns 404; narrative not stored); PASS: `npm run test:privacy` | NO — pending Antigravity | NO | YES — rights scope, identity checks, exceptions, response process and contact point |
| Membership import ambiguity and replay | PARTIAL — staged records remain separate; changed same-source replay becomes a review candidate; promotion remains administrator-only and does not verify/link accounts | PASS: `node test/membership_import_classification.test.js`; PASS: `node test/phase_b_workflow.test.js` (exact replay, changed replay, staging and promotion) | NO — pending Antigravity | NO | YES — any real-data import authority and source-specific data handling |
| Legacy identity reconciliation | PARTIAL — 0009 is unchanged; 0011 pins approval to record revision; approval writes only the protected identity snapshot and does not verify membership | PASS: `node test/phase_b_workflow.test.js` (stale revision rejected, current revision accepted, still REGISTERED) | NO — pending Antigravity | NO | YES — pilot administrator provisioning and review procedure |
| Phone change | YES — API returns `PHONE_CHANGE_UNAVAILABLE`; profile UI states unavailable; no change flow mutates account phone or membership link | PASS: `npm run test:privacy` checks 501 and unchanged synthetic account phone | NO — pending Antigravity | NO | NO — remains disabled pending a separately reviewed lifecycle |
| Leakage and Android asset packaging | PARTIAL — reviewed touched route/service logs and AI/sync/cache source references; build.gradle excludes `member_contacts.csv`; only top-level asset names were listed | PASS: source checks recorded in this checkpoint; `node test/member_contacts.test.js` confirms disabled lookup | NO — pending Antigravity | NO | NO for the checks performed; no real-data handling is authorized |

### Checkpoints and limits

- **Step 0 — baseline:** captured status, HEAD and hashes under `/tmp/data01-baseline/`; migrations 0001–0009 were present. The baseline `npm run test:privacy`, member-contact, importer classification and API unit suites passed. The baseline Phase B workflow failed because registration fixtures omitted the required notice acknowledgement; the PostgreSQL API test also failed because its fresh schema had no 0006 notice table. The Android Gradle attempt could not start because the local native library was not loadable in the restricted run.
- **Step 1 — PostgreSQL gate:** the migration safety suite passed on three uniquely named `perazim_phaseb_*_test` databases and dropped them. Full migration application through 0011 and exact ledger filenames passed in the governance PostgreSQL test. The Phase B workflow passed 141 assertions on its own disposable database. No real database was used. The migration safety suite exercises its declared foundation rollback/dump-restore behavior; it does not perform full-history dump/restore for 0002–0011.
- **Step 2 — migration review:** migration 0007 changes existing rows in these ways only: a `VERIFIED_MEMBER` row whose source is `CONTACT_LIST` becomes `REGISTERED`, its source becomes `MANUAL`, revision increments once, and one history row records `VERIFIED_MEMBER` → `REGISTERED`; all remaining `CONTACT_LIST` rows have only their source rewritten to `MANUAL`. A three-row synthetic database confirmed the verified contact row demotion, the registered contact source rewrite, and an unchanged manual row. No other migration contains DML that updates or deletes existing membership verification rows. Migrations 0006 and 0007 were not edited. Migration 0010 documents why there is no down migration: it stores append-only consent, request, publication and disposal evidence.
- **Step 3 — public policy:** storage and fail-closed delivery exist; real publication remains unavailable without a real permission grant and supplied approval reference. Successful publication, hash-failure rollback preserving an effective predecessor, and withdrawal were not verified end to end. No policy was published.
- **Step 4 — acknowledgement:** registration still sets only REGISTERED and MANUAL, ignores client role/status, uses no contact lookup and stores a server timestamp. The 428 payload is limited to code, version and hash. Receipt rejection coverage includes wrong account, stale version, wrong hash, expiry, withdrawal and replay, with no extra acknowledgement evidence. Android compile and unit tests passed; no device UI test was run.
- **Step 5 — consent:** production catalogue remains empty. The PostgreSQL integration inserted one temporary synthetic purpose inside the disposable database, verified grant/withdraw point-of-use behavior and dropped that database.
- **Step 6 — retention:** the dry-run function consumes only explicitly synthetic fixtures and makes no writes. The code has a two-actor confirmation helper that records a synthetic disposal event, but there is no protected operational execution route and no real deletion mechanism. No rule is approved or seeded.
- **Step 7 — requests:** request submission stores request type and identifier only; narratives are not accepted into storage or logs. Request status is self-scoped and neutral. Access export, complete correction revision interaction, legal exception review and administrator response are not implemented.
- **Steps 8–10:** exact import replay returns the prior batch; a changed replay stages candidates rather than replacing records. Reconciliation rejects a stale record revision. Phone change stays disabled in the API and UI.
- **Step 11 — leakage:** checked the touched privacy/auth/import/reconciliation source for raw error/request logging, the Android AI, sync and local persistence source references for membership/register values, and public membership response code. No member register value was routed to AI prompts, sync payloads, offline storage or public API output by these changes. `build.gradle` has both an asset exclusion and a merged-asset guard for `member_contacts.csv`; the top-level asset listing did not include that filename. The CSV and notebook photos were never opened or used.
- **Step 12 — regression:** current passing commands are listed below. `server/test/phase_b_persistence.test.js` was not run or edited because the audit session owns it. The baseline PostgreSQL API test failure remains a known harness limitation and was not masked.
- **Step 13 — documentation:** this report and the design-reference status were updated. Independent review and pilot acceptance remain NO.

### Current regression results

- PASS `npm run test:privacy` — generic fail-closed public responses, registration timestamp, receipt lifecycle, neutral DSAR receipt and unavailable phone change.
- PASS `node test/member_contacts.test.js` — contact lookup stays disabled; no CSV is read.
- PASS `node test/membership_import_classification.test.js` — synthetic missing/malformed/shared/duplicate/conflicting data and changed replay classification.
- PASS `node test/data01_governance.test.js` — retention dry-run cases and no writes.
- PASS `npm run test:unit` — all 19 API contract groups.
- PASS `npm run test:migrations` — foundation baseline, failure rollback and dump/restore checks on disposable databases.
- PASS `node test/phase_b_workflow.test.js` — 141 assertions on a disposable database.
- PASS `node test/data01_governance_postgres.test.js` — ordered migration ledger 0001–0011, consent, append-only and requester-scope checks.
- PASS `node test/phase_b_claim_rate_limit.test.js` — SEC-01 acceptance checks.
- PASS `bash scripts/test-sec-02-isolated.sh` — 4 scenarios, 94 assertions; owned database dropped.
- PASS `bash scripts/test-adm-01-isolated.sh` — 8 groups, 148 assertions; two disposable databases and owned cluster removed.
- PASS JavaScript syntax checks on every touched server JavaScript file.
- PASS `./gradle-8.5-all/gradle-8.5/bin/gradle compileDebugJavaWithJavac testDebugUnitTest --offline --no-daemon --quiet` — exit code 0; 67 tests, 0 failures, 0 skipped. No APK was built or installed.
- NOT RE-RUN: `node test/postgres.test.js`; its baseline failure was HTTP 503 because it initialized only the base schema without the privacy migration/notice fixture. It also truncates shared tables, so it was not run against the disposable migrated workflow setup.

### Files and migration ownership for this checkpoint

Created: migrations 0010 and 0011, `server/src/phase-b/data01Governance.js`, `server/test/data01_governance.test.js`, and `server/test/data01_governance_postgres.test.js`.

Modified: `server/src/db.js`, `server/src/middleware/auth.js`, `server/src/routes/auth.js`, `server/src/routes/privacy.js`, `server/src/routes/adminMembership.js`, `server/src/phase-b/membershipImport.js`, `server/test/privacy_privacy_gate.test.js`, `server/test/membership_import_classification.test.js`, `server/test/phase_b_workflow.test.js`, `server/test/migrations.test.js`, `src/main/java/org/perazimchurch/app/data/remote/PrivacyNoticeRemoteSource.java`, `src/main/java/org/perazimchurch/app/presentation/ui/AuthDialog.java`, `src/main/java/org/perazimchurch/app/presentation/ui/ProfileUiBinder.java`, this report, and the design-reference status text.

Deliberately unchanged: migrations 0006, 0007, 0008 and 0009; `server/src/phase-b/memberContacts.js`; `server/src/phase-b/security.js`; `server/test/phase_b_persistence.test.js`; `app/src/main/assets/member_contacts.csv`; and the avatar-feature and audit worktrees. No staging, commit, push, merge, reset, clean, or history rewrite was performed.

Migration ownership: 0008 was created by this DATA-01 work and adds keyed batch idempotency, staged import rows and append-only staging events. Migrations 0010 and 0011 were created by this checkpoint. Migration 0010 adds privacy publication, receipts, empty consent catalogue/events, draft retention structures and DSAR records. Migration 0011 pins reconciliation approval to the exact membership-record revision. Migrations 0006 and 0007 were created earlier by DATA-01 and remain unchanged; 0009 was already created by DATA-01 and remains unchanged.
