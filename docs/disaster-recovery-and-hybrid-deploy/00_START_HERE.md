# Perazim — Hybrid Deployment and Full Reconstruction Documentation

**Status:** REVIEW DRAFT — documentation plus scoped CI/release-build and API-endpoint configuration amendments; no migration, deployment, account change, data import, or mobile release authorized.
**Date:** 2026-11-10
**Baseline:** phase-B-community at cad3758e5469534c01142bbdeadc62371fa4e7f6
**Decision:** Path 3 hybrid selected for design; operational production approval remains separate.

## Start here — read in order

1. [HYBRID_DEPLOY_CHECKLIST.md](HYBRID_DEPLOY_CHECKLIST.md) — phased 14-day acceptance plan, detailed steps, evidence gates, and capacity.
2. [SCHEMA_SYNC_AND_FEATURE_CONTRACTS.md](SCHEMA_SYNC_AND_FEATURE_CONTRACTS.md) — proposed Room/PostgreSQL/API contracts for AI, giving, notifications, and safe sync.
3. [DISASTER_RECOVERY_RUNBOOK.md](DISASTER_RECOVERY_RUNBOOK.md) — independent backups, from-zero restoration, signing identities, Android/iOS reconstruction, and drills.
4. [reconstruction-manifest.json](reconstruction-manifest.json) — machine-readable observed toolchain, floating-version gaps, and API build properties.
5. Existing authority documents: [AI Decisions Register](../PERAZIM_AI_PROJECT/DECISIONS_REGISTER.md), [AI API contracts](../PERAZIM_AI_PROJECT/16_API_AND_DATABASE_CONTRACTS.md), [Privacy and retention](../PERAZIM_AI_PROJECT/15_DATA_RETENTION_AND_PRIVACY.md), [Room migration evidence](../foundation/ROOM_MIGRATIONS.md), [Phase B release gates](../phase-b/RELEASE_CANDIDATE_OPERATIONAL_CHECKLIST.md), [Server deployment preparation](../../server/deploy/README.md), and [Phase B status](../../STATUS.md).
6. Related but separate media-hub proposal: PR #1, https://github.com/lmurugu/perazim-android/pull/1. Its draft is NOT merged into this branch.

## One-page system map

~~~text
Android (existing Java/Room)             iOS (NOT YET BUILT; separate client)
  local Bible, media metadata              native Swift/SwiftUI + local SQLite
  Room cached feeds and pending ops        equivalent offline repository/queue
  offline AI + explicit online request     offline guide parity requires validation
            \                                /
             \ HTTPS / versioned API contract
              v                            v
                  Express API (AUTHORITY)
       membership, permission, sync, AI entitlement,
       giving reconciliation, notification records
                         |
                    PostgreSQL
                ONE authoritative database
                         |
             +-----------+-----------+
             |           |           |
         backups       FCM/APNs    external services
       independent     alert only  Gemini Worker gated;
       encrypted                 Safaricom C2B if approved;
                                 YouTube/audio embeds
~~~

**Important:** Device Room caches, cloud replication/sync, GitHub, and encrypted backups solve different problems. A named Cloudflare Tunnel gives a stable route to an origin; it does not make a powered-off Fedora PC available. A Supabase project is not a backup unless independent verified encrypted exports and restoration exist. A GitHub documentation copy is not an independent source-code backup.

## The three paths — decision matrix

| Path | Topology | Best use | Limitation / decision |
|---|---|---|---|
| 1 — local Fedora | Android Room -> local Fedora Express + PostgreSQL, optionally named Cloudflare Tunnel | Isolated internal validation, 10–20 testers after security review | Origin availability depends on Fedora power/internet; public exposure is NOT approved merely by creating a tunnel |
| 2 — Supabase-centric rewrite | Clients -> Supabase Auth/DB/Storage/Realtime | Separate future architectural evaluation | Would duplicate/replace established Express/JWT authority and require security and migration redesign; NOT selected |
| 3 — approved hybrid | Android Room offline-first -> Express -> one PostgreSQL; optional managed cloud DB/storage; independent backups | Closed pilot and controlled growth | The authoritative API must be reachable for online features; cloud backup alone does not provide failover; choose one production DB and one writer |

**Selected implementation sequence:** Preserve Express/JWT, existing PostgreSQL migrations, Room v6 and offline Bible; test locally first; then separately approve a stable publicly reachable API origin and backup target. Supabase can be evaluated as a managed PostgreSQL/storage provider without adopting parallel Supabase Auth or making it a second authoritative database. The previously discussed Supabase project is not assumed active, connected, or configured.

## Actual code evidence vs proposals

| Area | Inspected evidence | Classification |
|---|---|---|
| Android app | Native Java, package org.perazimchurch.app; minSdk 24, targetSdk 34; Room schema v6 | Exists; do not claim iOS parity |
| Local sync | sync_queue Room entity and DAO; Express /api/sync endpoint | Exists; cross-feature conflict and recovery gates require new tests |
| Offline AI | Existing local AI components and specifications; build gates disable Qwen and online AI in inspected build.gradle | Specified/partly implemented, NOT proof of enabled release |
| Online AI | Express /api/ai/online with verified-member/permission checks; fail-closed gates; Gemini Worker design | Disabled pending review; do not switch to OpenRouter silently |
| Giving | Paybill 4069983 external handoff; STATUS.md says no automatic Daraja C2B | Manual reconciliation today; proposed financial ledger is NOT implemented |
| Push | STATUS.md labels notifications HARDCODED/DEMO | FCM/APNs is a future opt-in integration, NOT deployed |
| Server | Express, PostgreSQL 15 Compose, local Fedora helper, separate un-deployed VPS/Caddy stack | No verified production public origin |
| iOS | No iOS project in inspected tree | Future new client; native Java APK cannot run on iOS |
| Disaster recovery | Room migration tests and some deployment documentation exist | Complete independent encrypted backup + full restoration drill NOT evidenced |

Repository truth supersedes an outdated screenshot, README statement, or proposed feature. In particular, STATUS.md documents prior device evidence but is not proof that the same features are enabled on the current branch/build.

## CI release identity and endpoint separation

- The workflow is `.github/workflows/build.yml`; root `build.gradle` owns `verifyReleaseApk`. Without a signing key, CI inspects `build/outputs/apk/release/app-release-unsigned.apk` as **UNSIGNED / NOT FOR DISTRIBUTION**. Only a keystore-backed signed `app-release.apk` may pass signed-release inspection. Never rename unsigned to imply signing.
- API origins are injected at build time: `debug` / local uses `-PperazimLocalApiBaseUrl`, `validation` uses `-PperazimValidationApiBaseUrl`, and signed `release` / pilot-production uses `-PperazimPilotApiBaseUrl`. Omission leaves the API origin empty for offline-only CI builds; signed release verification rejects an empty pilot origin.
- Validation may use an injected HTTPS Tailscale hostname for isolated VPN testing. Pilot production requires an owner-approved stable public HTTPS origin (e.g. a named Cloudflare Tunnel hostname), never Quick Tunnel, loopback, or Tailscale. Build URL checks do not prove reachability or owner authorization.
- Post-pilot Room changes require tested, non-destructive numbered migrations and exported schema fixtures. Existing destructive fallback is a legacy risk, not an acceptable migration strategy.

## Non-negotiable operating rules

- Closed enrollment only; never make registration or account self-claims confer membership, admin role, online AI entitlement, or finance authority.
- No real member import, real administrator provisioning, production tunnel exposure, C2B callback registration, FCM project activation, AI provider activation, release signing, device replacement, or server restart on this documentation branch.
- Financial confirmation is server-authoritative and independently reconciled; offline user intent cannot become VERIFIED.
- AI pastoral data is sensitive. Local conversation history is opt-in and private; default cloud conversation synchronization is prohibited pending approved purpose, consent, retention, and safeguards.
- FCM/APNs carries opaque identifiers or generic alerts, not private prayer, direct-message text, giving details, phone numbers, or AI prompts. Fetch authoritative content after authenticated app open.
- Preserve Android Room migrations, existing device data, privacy gates, server authority, and historical app identities. No destructive migration without a separate fleet-level decision.
- Keep all credentials, personal records, database dumps, device captures, APKs, signing material, and tunnel tokens OUT of the public repository.
- Evidence is classified as DESIGNED, STATICALLY INSPECTED, UNIT-TESTED, DEVICE-TESTED, STAGING-TESTED, or PRODUCTION-VERIFIED. Do not promote status without a dated artifact.

## Android and iOS strategy

**Pilot:** Keep existing native Android. **iOS:** Implement a separate Swift/SwiftUI client against a versioned, tested Express OpenAPI contract, with a native SQLite repository and equivalent offline mutation semantics. Share API contracts, schemas, test fixtures, branding (#E17D2F, #681A7D, #2A0F33), content, and acceptance criteria; do not assume Room/Java binaries are portable. The iOS offline AI runtime, media embed behavior, APNs integration, Xcode/macOS access, and Apple distribution costs require separate validation and owner approval. A PWA can be assessed as a limited-access bridge, NOT claimed equivalent to the native offline app.

## Review decisions before any implementation

1. Which single PostgreSQL deployment is authoritative for the pilot: local Fedora (internal only) or an approved hosted origin?
2. Which independent off-host backup destination, encryption key custodian, backup frequency, and recovery targets are approved?
3. Is the church authorized for Daraja C2B production callbacks for Paybill 4069983, or must the pilot use manual treasurer reconciliation?
4. Should AI history be saved locally at all? If yes, confirm opt-in, auto-delete period, export/delete controls, and no automatic server upload.
5. Does the church approve FCM + Apple APNs processing and associated privacy notice changes?
6. Who is the accountable privacy/pastoral escalation officer and finance reconciliation operator?
7. Is iOS a later native SwiftUI workstream, with macOS/Xcode and distribution arrangements, or is limited web access acceptable temporarily?
8. Who signs the 10–20 member pilot gate and the later 100–200 expansion gate?

## Acceptance boundary

This branch creates a professional **plan and reproducibility specification** plus scoped CI/build-configuration hardening, not a rebuilt or deployed app. Release approval requires verified disposable-database restore, replay-safe sync, Android physical device evidence, security/privacy signoff, payment reconciliation proof, notification delivery tests, public HTTPS reachability where applicable, and independent iOS acceptance when built.
