# HYBRID DEPLOY CHECKLIST — Path 3, Controlled Pilot and Capacity Gate

**Document state:** APPROVED ARCHITECTURE / IMPLEMENTATION PLAN ONLY.
**Prepared:** 2026-11-10. **Source baseline:** phase-B-community, cad3758e5469534c01142bbdeadc62371fa4e7f6.
**Scope:** 10–20-member controlled Android pilot first; 100–200 registered / 50–100 daily active / 20–30 heavy daily users only after acceptance. iOS is a separately gated new client, not an existing build.

## 0. Decisions and safety gates

- [ ] Name accountable owner, backend operator, church privacy officer, finance verifier, and two independent recovery custodians.
- [ ] Obtain written approval before publishing any API hostname/tunnel, changing provider configuration, using real member data, registering a Daraja callback, enabling AI/FCM, or distributing an APK.
- [ ] Select ONE authoritative PostgreSQL instance and document hostname, database name, role, migration version, and approved backup destination in a private operations inventory.
- [ ] Record source commit SHA, GitHub branch, JDK/Gradle/Android SDK versions, Node version, PostgreSQL version, test fixture IDs, and build SHA-256.
- [ ] Do not alter existing main app data, replace a signed main package, or use a production member database for tests. Follow root AGENTS.md and the Phase B release checklist.
- [ ] Never put secrets, real donor records, member contact files, C2B receipts, APK signing keys, or database dumps in Git.

## 1. Capacity model and budget boundaries

| Parameter | Phase 1 | Phase 2 design target | Acceptance evidence |
|---|---:|---:|---|
| Known registered participants | 10–20 | 100–200 | Approved enrollment roster; no public signup |
| Daily active | 5–15 planning assumption | 50–100 | Server event counts, not guesses |
| Heavy daily active | 2–5 planning assumption | 20–30 | Distinct authenticated users and API load |
| Concurrent API clients | 2–5 test | 10–30 test scenario | Synthetic load test and 95th percentile |
| Media | Metadata + externally hosted church sermons; limited private temporary attachments | Same, quotas monitored | Storage and egress reports |
| Infrastructure spend | Target $0 internal testing | $0 is NOT guaranteed for reliable public uptime, domain, iOS distribution, or provider overages | Owner-approved recurring cost ledger |

**Illustrative load envelope, not a measured forecast:** if 100 DAU make 10 ordinary mutations each and 30 heavy users make 50 additional mutations each, that is 2,500 mutations/day and 75,000/month, excluding feed reads, media, and retries. Test read amplification, burst concurrency, 429s, DB connection pool, upload caps, storage, outbound traffic, and queue replay. Do not infer 200-user readiness from member count alone.

**Provisional operational targets for approval:** RPO <= 24 hours for ordinary server data using nightly independent backups; RTO <= 8 hours after replacement infrastructure is available. These are design targets, NOT tested guarantees. Giving records require external M-Pesa/treasurer reconciliation for any gap, even after a successful restore. If the church requires lower RPO, evaluate continuous WAL archiving and additional hosting costs before promising it.

## 2. Milestone A — Days 1–5: baseline and isolated Fedora validation

### Day 1 — source and toolchain inventory

- [ ] Clone the exact approved branch and record its commit:
~~~bash
git clone https://github.com/lmurugu/perazim-android.git
cd perazim-android
git fetch --all --tags
git checkout phase-B-community
git rev-parse HEAD
git status --porcelain
~~~
- [ ] Verify JDK 17, Android SDK platform 34/build tools 34, Gradle wrapper, Node/npm, PostgreSQL 15-compatible tools, and test device model/OS. Pin versions and checksums in the recovery inventory.
- [ ] Inspect build.gradle: current build sets REMOTE_SHEET_FEEDS_ENABLED=false, ONLINE_AI_ENABLED=false, OFFLINE_QWEN_ENABLED=false; do not claim remote AI or feeds enabled in a released binary.
- [ ] Read existing docs: docs/foundation/ROOM_MIGRATIONS.md, docs/phase-b/RELEASE_CANDIDATE_OPERATIONAL_CHECKLIST.md, server/README.md, server/docs/POSTGRES_MIGRATIONS.md, docs/PERAZIM_AI_PROJECT/DECISIONS_REGISTER.md.

### Day 2 — baseline tests (safe by default)

- [ ] Android CI equivalent, in a configured isolated workstation/runner:
~~~bash
./gradlew testDebugUnitTest assembleDebug --stacktrace
git diff --check
~~~
- [ ] Backend tests not requiring a live DB:
~~~bash
cd server
npm ci
npm run test:unit
~~~
- [ ] For full PostgreSQL-backed tests, first create a uniquely named disposable database with disposable credentials; verify each test script's connection guard and never point it to the running member database. Then run the selected test targets from server/package.json (test:pg, test:community, test:migrations, test:privacy, test:phase-e-independent). Record exact commands, database fixture, pass/fail, and test report paths.
- [ ] Review pre-existing GitHub CI run 42645689094: Android unit tests and debug build passed; assembleRelease failed at verifyReleaseApk because the expected unsigned/no-keystore release artifact was absent. Treat this as a known baseline blocker, not a regression introduced by documentation. Do not bypass release inspection or insert signing secrets into public CI.

### Day 3 — local backend, synthetic only

- [ ] Use a separate test instance or the existing local runtime WITHOUT deleting .runtime or restarting a member-facing service.
- [ ] On a clean, authorized Fedora workstation, provision PostgreSQL 15 and Node as documented; the existing server/scripts/local-backend.sh relies on a pre-extracted private PostgreSQL runtime under server/.runtime/pgsql/usr and is NOT a universal fresh-machine installer.
- [ ] Alternatively use Docker/Compose with test-only environment values and volumes, never real member data. Keep DB bound privately; server/docker-compose.yml publishes only API loopback 127.0.0.1:3000.
- [ ] Check localhost health on the isolated instance:
~~~bash
curl --fail --silent http://127.0.0.1:3000/api/health
~~~
- [ ] Test enrollment authorization, privacy acknowledgement, token revocation, rate limits, private group visibility, and synthetic sync operations.

### Day 4 — connectivity and stable hostname decision

- [ ] Internal USB testing may use adb reverse for the isolated validation app only. Never switch an existing member's server URL or clear their Room data silently.
- [ ] A Quick Tunnel is a temporary test URL, NOT a permanent endpoint.
- [ ] A named Cloudflare Tunnel requires an authorized Cloudflare account, a domain managed in Cloudflare, tunnel credentials, DNS routing, and an approved stable origin. Record a private recovery procedure for tunnel credentials, DNS, host firewall, service startup, and health monitoring.
- [ ] Only AFTER explicit exposure approval: create a named tunnel, restrict origin to loopback, configure DNS hostname, and enforce TLS/authorization/rate limits. Do not expose PostgreSQL or admin endpoints; test via a non-Wi-Fi mobile connection.
- [ ] Record outage scenario: Fedora powered off -> tunnel unavailable -> Room remains usable offline -> pending operations wait -> server restore/reconnect replays safely. Named tunnel is not failover.

### Day 5 — physical device smoke test

- [ ] Use Infinix HOT 10 / Android 11 or a newly approved equivalent and the isolated .validation application ID; do not install over the main signed app.
- [ ] Test: offline Bible and hymnal; online login with synthetic account; prayer create/amen; airplane-mode mutation; reconnect and replay; app process death; 2-user privacy isolation; media metadata fallback; account change; server error handling.
- [ ] Save redacted dated evidence: device ID class/model (not hardware identifiers), OS, APK SHA-256, backend commit SHA, test fixture, pass/fail, screenshots or test XML without member data.

**Gate A:** 10-person internal test begins only after authentication/privacy tests and isolated connectivity pass. This is NOT yet a production member launch.

## 3. Milestone B — Days 6–10: durable queue, features, independent recovery

### Days 6–7 — offline sync and AI

- [ ] Preserve Room v6 and its migration history; new tables require a numbered v6->v7 migration, exported schema, upgrade fixtures, and rollback policy. Never use destructive migration to introduce new tables.
- [ ] Audit SyncQueueEntity/SyncQueueDao and server/src/routes/sync.js; record supported mutation types, idempotency, reject reasons, retry caps, timestamps, and authority boundaries.
- [ ] Add WorkManager for network-constrained background retries only after design approval; WorkManager is best-effort, not an exact real-time schedule. Foreground/manual refresh remains available.
- [ ] Test repeated mutation IDs, simultaneous client updates, duplicate Amens, server restart, 401/403/429/5xx, network interruption after server commit but before client acknowledgement, and account switching.
- [ ] AI: use existing offline engine and approved local knowledge. Proposed ai_sessions/ai_messages must be opt-in private local Room storage, with deletion and retention; NO default server sync. A queued online prompt requires explicit per-prompt user action, expiry, and renewed server authorization. Keep current Express/Gemini Worker gates OFF until privacy, entitlement, quota and provider review pass.
- [ ] Prove no AI prompt, pastoral disclosure, PIN, or model key appears in crash reports, push payloads, or generic sync batches.

### Days 8–9 — giving, notifications, backup

- [ ] Giving: current M-Pesa handoff remains operational. Proposed local giving intent is unverified until server-side evidence arrives; no offline VERIFIED state. Use immutable receipt ID, idempotent provider-event ingestion, unique receipt constraints, server-authoritative status, and treasurer reconciliation. No real Daraja C2B registration until merchant access and church finance approval are confirmed.
- [ ] Test synthetic C2B callback duplicates, out-of-order delivery, wrong Paybill, wrong account reference, wrong amount, forged payload, missing callback, provider outage, refund/dispute, and reconciliation audit. Never use real money in the test environment.
- [ ] FCM: document Firebase project/account owner, Android app registration, notification permission (Android 13+), per-device token lifecycle, consent, server-only send credentials, generic payloads, opt-out, and logout revocation. iOS requires APNs setup and separate client testing. FCM is a best-effort wake-up signal, not a source of truth.
- [ ] Encrypted backup: PostgreSQL custom-format dump, uploads/object metadata and media where church-owned, versioned infrastructure config, release signing material, tunnel credentials, and separate recovery keys. Store off-host in a separately controlled location. Verify a restore into a disposable DB and validate migration history/record counts without exporting private data to CI.

### Day 10 — recovery drill

- [ ] Simulate Fedora loss by rebuilding a new disposable backend from repository commit, secrets escrow, and independent encrypted backup. Do not touch production.
- [ ] Verify restored member/account fixtures, role and consent state, one-message/one-Amen replay, unique payment receipt and audit log, and absence of duplicated AI requests or notification sends.
- [ ] Record actual measured RPO/RTO, backup integrity checksum, key custodian, restore operator, evidence location, and incident escalation contact.

**Gate B (Step 8 — conflict resolution):** no duplicate financial confirmations; no unauthorized private data; no lost acknowledged writes; deterministic conflict resolution; durable queue after crash/restart; 0 unexplained schema/data-loss events; independently verified restore. Any failure blocks expansion.

## 4. Milestone C — Days 11–14: 10–20-member closed pilot

- [ ] Obtain privacy notice, operator, and release approvals; keep closed enrollment. Do not enable public signup or self-awarded permissions.
- [ ] Day 11: 10 known participants on approved deployment; record mobile-data HTTPS access (no USB, adb reverse, Wi-Fi-only path, or laptop proxy).
- [ ] Day 12: expand to 10–20 after no severe privacy/sync failures. Monitor error rates, queue backlog age, DB size, backup job health, latency, and on-call response.
- [ ] Day 13: test offline/online handoff and push alert timing on Infinix with battery optimization conditions; FCM failure must not lose authoritative notifications.
- [ ] Day 14: review defect log, restore evidence, privacy incidents, member feedback, measured performance and cost. Sign or reject gate for further growth.

**Phase 2 growth:** 25 -> 50 -> 100 -> 200 registered members, pausing at each threshold for synthetic 10–30 concurrent-user load, 50–100 DAU traffic observations, restore drill, security review, notification opt-out tests, and cost/egress checks. A failed Step 8 means NO scale-up.

## 5. Cross-platform delivery gate (separate from 14-day Android pilot)

- [ ] Freeze versioned OpenAPI specification, authentication/privacy errors, cursor-based sync, idempotency semantics, media URL lifetimes, AI permission contract, payment receipt status, and push registration endpoints.
- [ ] Build native iOS Swift/SwiftUI app against same Express API with local SQLite/GRDB (or approved equivalent). Android Room schema is NOT iOS binary-compatible.
- [ ] Verify iOS offline Bible content rights, native local AI feasibility, media embeds, background sync limitations, Keychain, APNs/FCM token flow, notification privacy, data deletion, and store distribution.
- [ ] Arrange macOS/Xcode and Apple developer distribution capability; no assertion of zero-cost native App Store distribution. PWA may serve as a reduced-capability interim path only after explicit product decision.
- [ ] Execute equivalent Android+iOS contract tests and independently test device/account migration and disaster restore.

## 6. Acceptance evidence and stop conditions

Every checked item requires **operator + date + commit + environment + command + actual output + artifact reference**. Record FAIL, BLOCKED, or NOT TESTED explicitly; do not mark planned features DONE.

Stop immediately for: privacy leakage, lost/duplicated giving confirmation, accidental member data import, production database touched by tests, unreviewed AI external processing, missing independent backup, unverified signing key custody, uncontrolled public exposure, or inability to restore a synthetic environment.

### External technical references (verify again before deployment)

- Cloudflare named tunnel setup: https://developers.cloudflare.com/tunnel/get-started/
- Firebase Cloud Messaging pricing: https://firebase.google.com/pricing/
- Safaricom Daraja C2B API: https://developer.safaricom.co.ke/apis/CustomerToBusiness
- Android WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
- Existing project release gate: ../phase-b/RELEASE_CANDIDATE_OPERATIONAL_CHECKLIST.md
