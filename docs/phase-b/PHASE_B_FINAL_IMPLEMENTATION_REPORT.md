# Perazim AI Phase B implementation report

**Date:** 7 November 2026 (Nairobi)
**Branch:** `phase-B-community`
**Phase A baseline:** `01d9e3e`
**Scope:** B1–B7 in local development with synthetic accounts and disposable databases.

## Implementation

- The membership workflow starts with a registered account and accepts one open claim per account. It returns a neutral receipt before private matching. Exact, missing and duplicate matches have the same user-facing pending state. A pilot administrator reviews the claim, and the server records verification, rejection, correction and revocation. The account receives a neutral update in its existing notifications inbox.
- PostgreSQL migration `0002_membership_permissions.sql` adds membership, history, scoped permissions, audit and administrator security records. Migration `0001`, the production schema bootstrap and the Android Room schema are unchanged.
- Express resolves current account authority from the database for authenticated requests. The membership and administrator routes check current, enabled, unexpired grants and exact resource scope. The administrator must be the sole bootstrap identity and have a fresh privileged session. The server audits protected reads and administrative changes.
- The bootstrap and recovery command requires the exact account and database plus an approval reference. The reference records the operator's attestation. Test routes and bootstrap commands are gated to explicit test mode and an isolated, uniquely named local PostgreSQL database.
- Android adds Church Membership in the profile. It displays server status or an account- and server-scoped last-known status. Administrator tools fetch fresh grants and require PIN and authenticator step-up. Privileged operations send directly to the server, and the session is cleared when the dialog closes. Session tokens use Android Keystore encryption bound to the account and server origin. Credential preferences are excluded from Android backup and transfer.

See [contracts and access inventory](CONTRACTS.md) for the permission catalogue and endpoint map, and [persistence evidence](PERSISTENCE_EVIDENCE.md) for migration and restore procedures.

## Acceptance evidence

| Check | Environment and result |
|---|---|
| Migration, rollback and backup/restore | Phase B persistence and Phase A migration suites passed in disposable PostgreSQL 18.6 and 15 databases. The restore checks compared synthetic rows and migration records. |
| Current authority and permissions | Isolated runner passed the current-authority and security suites. They covered forged or stale claims, inactive accounts, token revocation, self-grant revocation, route compatibility and response caching policy. |
| Membership and administrator workflow | 62 assertions passed on PostgreSQL 18.6 and 15. Coverage includes uniform claim results, administrator review, revocation, notification isolation, scoped/disabled/expired permissions, TOTP replay, audit and synthetic recovery. |
| Backend compatibility | Node v26.7.0. All 19 API contract checks, PostgreSQL integration, all 11 community checks and process restart/persistence checks passed on the isolated runner. |
| Classroom call regression | Existing classroom browser test passed on Chromium with fake media and a disposable PostgreSQL 15 backend. Two authenticated peers negotiated audio/video, decoded a 640×480 frame, tested mic/camera controls and left the call. An unauthorised caller was denied. |
| Focused regressions | Classroom signalling, proxy configuration and database safety passed. Relay conversation tests passed with mocked Gemini fetch. |
| Android build and unit tests | 67 tests passed. Debug and validation builds and validation instrumentation APK built offline. |
| Android instrumentation | Infinix X689C, Android 11. The corrected validation run passed 11 tests with zero failures or errors. The separate live-fixture test was skipped in this run by design. Coverage includes Room migration, credential storage and tamper handling, guest UI and snapshot isolation. |
| Phone-to-backend workflow | One targeted test passed on the Infinix X689C against a disposable PostgreSQL 15 fixture. Two synthetic logins, administrator step-up, neutral claim, protected record correction, approval, access and revocation all executed through the Android client. The USB port mapping was removed after the run. |

Backend reproduction uses `bash server/scripts/test-phase-b-isolated.sh`. Android reproduction uses `./gradlew testDebugUnitTest assembleDebug connectedValidationAndroidTest --offline --no-daemon`. The separate phone-to-backend test uses `server/test/phase_b_device_fixture.js` and `PhaseBBackendDeviceTest`. Its default instrumentation run skips the test until explicitly given the synthetic fixture arguments. Test credentials are temporary.

The first PostgreSQL 15 restore attempt used PostgreSQL 18 client tools and failed on an unsupported setting. The PostgreSQL 15 rerun passed with matching tools. An initial runner attempt also stopped at its PostgreSQL version parser, which was corrected before the successful run. The first connected Android run found one guest-dialog test failure because the active accessibility window did not expose the dialog's labels. The rerun passed after the test inspected the displayed dialog directly. The first standalone fixture launch could not find its instrumentation package after Gradle cleanup. It passed after reinstalling the two validation APKs.

## Scope and remaining limits

No real membership records, real administrator activation, production database migration, provider changes, deployment or push occurred. The specification hash inventory and Phase A SQL baseline matched. The real phone application was left in place. The validation package is the only Android package used for installation during the tests.

The phone workflow uses a local USB bridge, and browser media is simulated. Full manual coverage of every legacy screen, real-device classroom media, production rollout, legacy Room installations and the Node 24 container pairing remain unverified. Existing legacy access paths and AI-provider integration were outside this phase's implementation scope. Approval and policy decisions for a real pilot remain separate.

## Change inventory and history

- `c449595` records Phase B contracts and access boundaries.
- `6accced` adds migration 0002, fixture data and persistence checks.
- `4a08396` enforces current account authority and adds its tests.
- `461dfe2` records migration evidence and provides the isolated runner.
- `7ffb489` adds membership and administrator workflows, security helpers and API tests.
- The final Phase B commit adds the Android integration, validation harness and this report. Use `git diff --name-status 01d9e3e HEAD` for the exact file inventory.

The pre-existing `docs/PERAZIM_AI_PROJECT/` directory is intentionally untracked. The final response records the commit containing this report and the observed working-tree status.
