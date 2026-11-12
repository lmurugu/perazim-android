# Phase F production readiness — 2026-11-09

**Scope:** primary checkout at `cad3758e5469534c01142bbdeadc62371fa4e7f6`, branch `phase-B-community`. This is an evidence-based snapshot of the complete intended app, not a claim of pilot or production acceptance. No app or backend code was changed in this checkpoint; the evidence and scorecard are recorded here.

## Baseline and worktree preservation

- The Git repository is `/home/murugu/Android/app`; `/home/murugu/Android` is its parent/container. Before this note was added, the checkout had no tracked or untracked changes. HEAD was `cad3758` (`Configure side-by-side pilot app for HTTPS backend`), following the version 2.2.1 and PIN-length changes. No changes were staged, committed, pushed, merged, or rebased.
- Worktrees inspected read-only: `/home/murugu/.codex/worktrees/audit-58f593b/app` (clean, detached at `58f593b`); `/home/murugu/Android/worktrees/audit-db1dce3` (clean, detached at `db1dce3`); `/home/murugu/Android/worktrees/antigravity-audit` (untracked audit-owned runner, source and tests); and `/home/murugu/Android/worktrees/avatar-feature` (uncommitted Community backend and avatar Android work). The Antigravity and avatar changes were left untouched and were not executed or incorporated. No other active sub-agents were present.
- Primary implementation uses Java/Android, Express and PostgreSQL. Migrations `0001` through `0011` are present. The primary migration API rejects any default plan containing `0007` before acquiring a client; owned test runners statically check this guard precedes cluster creation.
- Existing APK artifacts predate this work session. Their presence and inspection do not make them fresh builds. The signed release APK is version 2.2.1; see release details below.

## Weighted production score

The denominator is 100 points across the complete intended product. Weights reflect user-facing scope and release risk, including the two separate AI modes, privacy, device acceptance, and operations; no weights were changed to improve the result. Maturity credit is 0 for absent, 0.25 for implemented, 0.45 for integrated, 0.65 for tested, 0.80 for operationally verified, and 1.0 for production accepted. Partial evidence is capped at the strongest stage actually established. A disabled or unapproved capability receives no operational or acceptance credit.

The “before” value is the reconstructed state of this same clean checkout at preflight, not a historical Phase E percentage. Because no production capability changed during this task, its after value remains the same.

| Product area | Weight | Evidence maturity | Earned |
|---|---:|---:|---:|
| Android foundation and reliability | 10 | 0.45 — integrated; existing signed APK inspected, no build/test/device acceptance today | 4.50 |
| Backend, database and API integration | 10 | 0.45 — integrated source/contracts; no database execution | 4.50 |
| Accounts, authentication and membership | 10 | 0.45 — integrated fail-closed flows; synthetic enrollment check passed, PG workflow not run | 4.50 |
| Community and shared interactions | 12 | 0.45 — most flows integrated to API; connections remain local and no cross-account run was possible | 5.40 |
| Online AI Guide | 7 | 0.25 — guarded implementation and mock contract tests; entitlement/operations incomplete | 1.75 |
| Offline AI Guide | 5 | 0.10 — runtime/download gates off; model hash alone is not approval | 0.50 |
| Bible and approved knowledge | 6 | 0.30 — offline resources exist; approval, licensing and signed corpus not established | 1.80 |
| Daily routines, streaks and discipleship | 4 | 0.20 — UI/local progress implementation; shared and operational acceptance absent | 0.80 |
| Giving and M-Pesa handoff | 5 | 0.15 — giving UI exists; no verified payment-provider handoff | 0.75 |
| Sermons, media and content feeds | 5 | 0.30 — local catalogue/media paths; institutional feed approval and shared operations absent | 1.50 |
| Events, hymns and church resources | 5 | 0.30 — local resources and API paths exist; approval and operational verification absent | 1.50 |
| Notifications | 3 | 0.25 — implementation present; delivery acceptance not established | 0.75 |
| Privacy, security and administration | 8 | 0.40 — important controls and database-free contracts tested; publication and PG/admin acceptance blocked | 3.20 |
| Device acceptance | 3 | 0.20 — older evidence exists, but no device was accessible in this run | 0.60 |
| Release packaging and operations | 7 | 0.45 — existing release artifact passes inspection; fresh build and deployment gates incomplete | 3.15 |
| **Total** | **100** |  | **35.20%** |

**Before: 35.20%. After: 35.20%. Gap to 40%: 4.80 percentage points.** This is below the requested threshold. The blocked items are actual service, device, institutional and release acceptance; source presence is not being counted as operational completion. The score is a conservative product-wide maturity measure, not a statistical estimate.

## Community and persistence

| Capability | Current primary-checkout evidence | Classification |
|---|---|---|
| Feed/posts, Amen, comments | Android Community UI calls `/api/community`; Express handlers persist `community_posts`, reactions and comments in PostgreSQL. Account/server-scoped cache and drafts are local conveniences, not the delivery store. | RESOLVED — OPERATIONAL VERIFICATION PENDING |
| Prayer requests and Amen | Express prayer APIs and `/api/sync` persistence exist; Android has remote calls and queued synchronization. | RESOLVED — OPERATIONAL VERIFICATION PENDING |
| Groups, classes, direct messages, event RSVP, notifications and media | Corresponding Express/PostgreSQL routes and Android Community flows exist, with authenticated membership/ownership checks in source. No cross-account execution was run in this assignment. | RESOLVED — OPERATIONAL VERIFICATION PENDING |
| QR connections/friend requests | The reachable Android dialog calls `ConnectionRepository`, whose current implementation is backed by Room. No corresponding connected Express route is present in the primary checkout. | PARTIALLY RESOLVED |

Thus, Community as a whole is not “local-only,” but it is also not fully accepted as shared Community: several major flows are server-backed in source, and connections remain device-local. No two-account/device persistence claim is made. The avatar worktree has separate uncommitted changes and was not used as evidence for this checkout.

## AI, membership, content and privacy

- **Online AI:** Express derives identity from the authenticated session, requires current `VERIFIED_MEMBER` plus an explicit permission from the environment-selected catalogue code, rejects client identity fields, marks responses `no-store`, applies a limited identifier filter, and uses a signed short-lived grant to a mockable Worker provider. Offline contract tests passed for the mock path and Worker service authentication. Runtime gates remain off. `ai.online.use` is proposed but absent from the catalogue; do not add or grant it without the project decision. Atomic quota enforcement, one-time grant replay protection, reviewed pastoral-data safeguards, approved retrieval, deployed Worker/configuration, provider terms/model and live service evidence remain missing.
- **Offline AI:** `OFFLINE_QWEN_ENABLED=false`; inference and download fail closed. No approved/licensed/versioned/signed knowledge package or trusted signing key was established. The pinned Qwen download size/hash establishes file integrity only, not content approval or licensing. No internet fallback is enabled as an offline answer.
- **Membership/admin:** Registration, claims, verification and permissions are separate in source. Ordinary applicants receive neutral outcomes; only the designated pilot admin path can approve a synthetic membership link. The independent enrollment-file test passed. No PG-backed verification workflow or real administrator was activated. Production provisioning needs an approved account mapping, controlled database and secret-delivery/enrollment environment; the exact synthetic-only procedure is in `ADMIN_PROVISIONING_PROCEDURE.md`.
- **Bible and content:** The source SQLite asset and the inspected APK contain the same database byte-for-byte. Schema-only/count checks found full verse rows for ESV (31,103), KJV (31,102), NIV (31,102), NKJV (31,102) and SUV (31,102); no scripture text was printed. Android offers those five translations and queries the selected translation. This establishes packaged data and an offline retrieval path in source, but no device airplane-mode run was possible. The database's `versionInfo`/`licenseInfo` fields contain version/attribution strings (KJV and SUV say “Public Domain”; NIV/ESV/NKJV name publishers); those strings are not evidence of rights approval, licensing terms or an institutional sign-off. The APK therefore has an unresolved content-rights release gate. The local seeds and eight remote spreadsheet feeds do not establish approval. Feed gates remain disabled; no sheet was opened or fetched. No approved Qwen corpus or trusted approval/version/signature chain was demonstrated.
- **Privacy/security:** Privacy notices remain drafts and registration is publication-gated. No legal identity, contact, lawful basis, retention schedule, vendor/cross-border terms, ODPC status or institutional approval was invented. Request intake is not full request fulfillment. No real membership register CSV was opened, read, tested, packaged, logged, or transmitted. Release inspection checked the packaged database for restricted records without printing content.

## Commands and verification results

Environment observed: Node `v26.7.0`, npm `11.19.0`, Java `21.0.12.1`, installed Gradle distribution `8.5`, ADB `37.0.1-15733141`. `psql` and `pg_ctl` are not on `PATH`.

| Command | Exit | Result |
|---|---:|---|
| `npm run test:phase-e-independent` (in `server/`) | 0 | Eight database-free test files passed: privacy publication/acknowledgement (draft/unpublished/published/withdrawn, registration binding, unverified membership), enrollment secret-file protections, DB destination/startup guard, migration 0007 API refusal, runner guard ordering, online AI request/eligibility/provider contract, and Worker signed-auth contract. One expected warning stated `JWT_SECRET` was unset and an ephemeral key was used for this test session. |
| `python3 scripts/inspect_release_apk.py build/outputs/apk/release/app-release.apk` | 0 | Release inspection passed: prohibited credential/configuration markers absent; cleartext transport disabled and network policy present; APK signature verified; 20 default-avatar assets matched source SHA-256; privacy documents remain marked DRAFT; one bundled SQLite database checked for restricted records. |
| `apkanalyzer manifest application-id …`; `version-name …`; `version-code …`; `apksigner verify --print-certs …` | 0 | `org.perazimchurch.app`, `2.2.1`, code `5`; signature verification passed. Signing certificate SHA-256: `e41e654796700f948860dd0a6c4c4362b2addbe8fc7b55bbb43c070afde049a2`. |
| `sha256sum build/outputs/apk/release/app-release.apk` | 0 | `8692d41792cdac154efcb30499a855f392e8420b12b2ee40aee47230106f7ef5` |
| `./gradlew --no-daemon testDebugUnitTest assembleDebug --offline --console=plain` | 1 | Wrapper stopped before Gradle started: it could not create its `.zip.lck` in the read-only shared Gradle cache. No Android tests/build ran. |
| `GRADLE_USER_HOME=/tmp/perazim-gradle-user /home/murugu/.gradle/wrapper/dists/gradle-8.5-bin/5t9huq95ubn472n8rpzujfbqh/gradle-8.5/bin/gradle --no-daemon testDebugUnitTest assembleDebug --offline --console=plain` | 1 | Temporary writable Gradle home got past wrapper startup, but the single-use daemon could not bind its local socket (`java.net.SocketException: Operation not permitted`). No Android tasks ran. No system permissions or caches were changed. |
| `adb devices -l` | 1 | ADB `37.0.1` could not start its loopback listener on port 5037 (`Operation not permitted`). No device enumeration, install, or device test occurred. |
| `git diff --check` | 0 | No whitespace errors in the pre-documentation checkout. |

Database-dependent API, Community, membership/admin, migrations and PostgreSQL integration suites were not run. The safe runner and migration test sources were inspected first. The full migration plan contains prohibited `0007`; no runner capable of applying it was invoked. Independent tests used no PostgreSQL connection. **Migration `0007` was not applied by this assignment.** No database cluster was started. The `npm test` aggregate was not run because it launches database-backed suites; `npm run test:migrations` was not run because no PostgreSQL test service exists here. No server socket tests were repeated after the documented loopback `EPERM` failure.

## Release classification and remaining dependencies

The inspected APK is a pre-existing **internal QA release artifact**, not a fresh build and not a production release. Path: `build/outputs/apk/release/app-release.apk`; 25,687,673 bytes; artifact timestamp 2026-11-09 18:27 EAT (before the current HEAD commit at 18:32); package/version as above. No fresh APK was produced. Manifest/signature/asset scan passed, but that does not establish a tested backend, app-device acceptance, approved privacy state, content rights, or production operations. Release classification: **BLOCKED — APPROVAL REQUIRED** for translation/content rights, and otherwise **PARTIALLY RESOLVED**.

| Dependency / owner | Exact action needed |
|---|---|
| Project owner / permission-catalogue decision | Decide whether to add `ai.online.use`, its grant authority and scope; do not grant it automatically. |
| Backend/security owner | Implement and independently test atomic quota and one-use replay protection; complete privacy/safety review and approved retrieval; verify Worker deployment, secrets/configuration, provider model and terms in a controlled environment. |
| Church leadership and privacy counsel | Supply approved notice/controller identity and contact; determine lawful basis, retention, ODPC and child/sensitive-data handling, vendors/cross-border terms, and approve publication/registration state. |
| Content/licensing approver | Approve the exact offline Bible/knowledge corpus, translation rights, version and withdrawal policy; establish signing custody and the app’s trusted public key. Do not activate Qwen before this. |
| Pilot operations owner | Provide a controlled PostgreSQL test service and designated pilot device with loopback/ADB permission, plus the approved administrator account mapping and secret-enrollment channel. Keep all records synthetic until real-data work is separately approved. |
| Database/migration owner | Establish an approved test plan that excludes execution of 0007 and provide a disposable database. Do not use the current default full migration runner under this assignment’s restriction. |

**Realistic next step toward a functional pilot:** run the primary Android client and Express API against a controlled disposable PostgreSQL environment with synthetic accounts, using a reviewed migration/test plan that cannot execute `0007`; demonstrate post/comment/Amen/group/message changes across two accounts; complete the authorized device acceptance; then address privacy publication and administrator provisioning before enabling registration. Keep online and offline AI disabled until their separate gates are satisfied.

## Classifications

- Community server/API source integration: **RESOLVED — OPERATIONAL VERIFICATION PENDING**.
- QR connections across accounts/devices: **PARTIALLY RESOLVED** (Room-local in the primary checkout).
- Online AI: **PARTIALLY RESOLVED** (mock contracts pass; operational gates are off).
- Offline AI: **BLOCKED — DEPENDENCY REQUIRED** (approved corpus and signing trust absent).
- Bible translation distribution: **BLOCKED — APPROVAL REQUIRED** (the QA APK packages five complete translation datasets; attribution metadata does not establish distribution permission).
- Synthetic administrator workflow: **RESOLVED — OPERATIONAL VERIFICATION PENDING** (database workflow not exercised).
- Privacy publication and public registration: **BLOCKED — APPROVAL REQUIRED**.
- Fresh build/device acceptance: **BLOCKED — DEPENDENCY REQUIRED** (Gradle/ADB local socket permissions unavailable).
- Migration 0007: **RESOLVED AND VERIFIED** as a refusal guard; explicitly not applied.
- Production readiness overall: **PARTIALLY RESOLVED**; score remains 35.20%, below target.
