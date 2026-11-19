# Store readiness checklist: Google Play and the Apple App Store

Status key: **DONE** (in the code and tested), **CODE** (needs code work), **OWNER** (needs Levi, an account, money or a decision), **VERIFY** (needs a device or a store console).

Last reviewed: 2026-11-19. Branch: `claude/project-thread-xf5etn`.

## 1. App behaviour that stores check

| Requirement | Google Play | Apple App Store | Status |
|---|---|---|---|
| Users can report objectionable content and members | Required for user-generated content | Guideline 1.2 | **DONE** Report on posts, comments and members, plus a Report option in conversations. Server: `POST /api/community/reports`. Android and web. Tested: `server/test/content_safety.test.js`, and a browser run of report then block. |
| Users can block abusive members | Required for user-generated content | Guideline 1.2 | **DONE** Blocks hide content both ways and stop messages. Undo under Blocked members. |
| Admins can act on reports | Expected | Expected | **DONE** API: `GET /api/community/reports`, `POST /api/community/reports/:id/decision` (admin role). **OWNER**: a written procedure for who reviews reports and how fast. |
| In-app account deletion | Required when accounts exist | Guideline 5.1.1(v) | **DONE** Profile → Delete my account, which runs the erasure in `server/src/privacy/erasure.js`. |
| Web link for account deletion | Required | Required (App Store Connect field) | **DONE** `https://lmurugu.github.io/perazim-mission-church/delete-account/`. **OWNER**: confirm the church's own domain if it differs. |
| Privacy policy at a public URL | Required | Required | **OWNER** The policy is draft v0.2 and has placeholders. Publish only after owner approval. |
| Data safety form (Play) and privacy labels (Apple) | Required | Required | **OWNER** Fill in from the data inventory (`docs/security/data-inventory.md`). The inventory must cover posts, messages, photos, prayer requests, AI questions and sign-in data. |
| Reviewer demo account | Required (App access) | Required (App Review notes) | **OWNER** Create one approved reviewer account on the production server, and give its credentials only to the store consoles. |
| Sign-in does not block review | Guest mode works offline | Guest browsing works | **DONE** Guest mode reads the Bible, sermons and public discussions without an account. |
| Content rating | Questionnaire | Age rating | **OWNER** Answer in the consoles. The app contains user-generated content, so answer the questions truthfully. |
| Religious and sensitive content | Declared in the data safety form | Declared in privacy labels | **OWNER** Confirm the declarations match the policy. |

## 2. Android build and permissions

| Item | Status | Notes |
|---|---|---|
| targetSdk 36, compileSdk 36, minSdk 24 | **DONE** | Gradle is the source of truth (`build.gradle`). |
| Debug build and unit tests | **DONE** | CI job "Unit tests and debug build" passes on PR #35. |
| Database instrumentation and background sync emulator tests | **VERIFY** | Running in CI on PR #35 at the time of writing. |
| Release build | **OWNER** | Needs the signing key and `perazimPilotApiBaseUrl`, an approved public HTTPS origin. |
| `INTERNET` | Used | Required. |
| `CAMERA` | Used for profile photos and classroom video | Play requires a declaration of why it is needed. Prompt only when the user asks. |
| `RECORD_AUDIO` | Used by the classroom call page (`src/main/assets/classroom.html`) | **VERIFY** Confirm the classroom call is shipped to members. If it is not, remove the permission. Otherwise keep the declaration. |
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (≤ API 32) | Used to find Bible audio files on the phone | **VERIFY** Play may question broad audio access. Prefer the system file picker if it is enough. |
| `usesCleartextTraffic=false` | **DONE** | Release uses HTTPS only. |
| `allowBackup=false` | **DONE** | Member data is not copied to cloud backups. |

## 3. Web client (`web/`) and the Capacitor wrappers

| Item | Status | Notes |
|---|---|---|
| Installable PWA with manifest and icons | **DONE** | Church icons reused. |
| Responsive layout at phone width | **DONE** | Tested at 390 px. Also needs a check at 320 px and in landscape. **VERIFY**. |
| Safe-area insets for notched iPhones | **DONE** | `env(safe-area-inset-*)` in `web/styles.css`. |
| Service worker never caches API responses | **DONE** | `web/sw.js`. |
| Content Security Policy | **DONE** | Set in `web/index.html`. |
| Capacitor project (`android/`, `ios/`) | **OWNER** | Not generated. Needs Levi's decision on the Android project (Java app versus Capacitor) and a Mac for iOS. |
| Apple "minimum functionality" (guideline 4.2) | **CODE** | A web app in a wrapper can be rejected. Native value is needed: push notifications, offline storage and photo access. Not built. |
| iOS privacy manifest (`PrivacyInfo.xcprivacy`) | **OWNER** | Needed once the iOS project exists. |

## 4. Owner actions

1. Choose the iOS approach and the Android build that ships.
2. Create the Google Play account ($25) and the Apple Developer account ($99 a year).
3. Confirm the signing key and its backup.
4. Choose and approve public hosting for the API, and set `perazimPilotApiBaseUrl` and `ALLOWED_ORIGINS`.
5. Approve the privacy policy and fill in the legal name and privacy contact.
6. Create the reviewer demo account.
7. Complete the data safety form, the privacy labels and the age ratings.
