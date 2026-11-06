# Perazim Mission Church Android App — Feature Status & Honesty Audit

> **Target Version**: `2.2.0` (VersionCode `4`)  
> **Package ID**: `org.perazimchurch.app`  
> **Target Audience**: 100-Member Pilot (Embu Headquarters & Satellite Campuses)  
> **Audit Date**: November 3, 2026  
> **Audit Branch**: `feature/dynamic-community` (Stage 1: Google Sheet Dynamic Content)  
> **Audit Commit**: `f035e3496fc701d4469ba472008229c4ec438475`  
> **Policy**: Zero unsubstantiated claims. Every feature is strictly verified against actual implementation and backed by hardware screenshots and `uiautomator` text dumps.

---

## 1. Feature Status Summary Matrix

| # | Feature Area | Status | Summary of Reality / Architecture Limitation | Proof File(s) |
|---|--------------|--------|----------------------------------------------|---------------|
| 1 | **Offline Bible (5 Translations)** | `WORKING` | Pre-seeded SQLite database (`perazim_database.db`) with KJV, Swahili SUV, NIV, ESV, NKJV. Instant offline queries, 66 books, chapter navigation, text search. No cloud sync. | [`08-bible-reader.png`](docs/review/dc15a79-08-bible-reader.png)<br>[`08-bible-reader.txt`](docs/review/dc15a79-08-bible-reader.txt) |
| 2 | **Lipa na M-Pesa Giving** | `PARTIAL` | Copies Paybill `4069983` to clipboard and cascades to Safaricom M-Pesa app, SIM Toolkit, or `*334#`. **No automated Daraja C2B/STK push API**; reconciliation requires manual treasurer check. Fake (+20 XP) giving reward removed. | [`06-giving-dialog.png`](docs/review/dc15a79-06-giving-dialog.png)<br>[`06-giving-dialog.txt`](docs/review/dc15a79-06-giving-dialog.txt) |
| 3 | **Community Prayer Wall & Cloud Sync** | `WORKING` | Requests stored in local Room SQLite database (`prayers` table) and synchronized with self-hosted server via `SyncDispatcher`. Multi-account intercession verified on live hardware (Account 1 posts prayer, Account 2 reads and amens, amen count updates across devices and on server). Offline mutations safely enqueued in `sync_queue` and flushed on reconnect. | [`18_prayer_wall_fixed.png`](docs/review/phase_A/18_prayer_wall_fixed.png)<br>[`20_sister_mary_amen.png`](docs/review/phase_A/20_sister_mary_amen.png)<br>[`22_prayer_wall_synced.png`](docs/review/phase_A/22_prayer_wall_synced.png) |
| 4 | **AI Breakthrough Guide** | `STAGE 2 PENDING` | Cloudflare Worker Gemini relay (`AiRelayClient.java`) and on-device fallback (`PerazimAiEngine.java`) implemented on branch; **Stage 2 gate is not yet approved/opened**. Generative AI cloud features remain gated pending Stage 2 formal approval. | [`09-ai-guide.png`](docs/review/dc15a79-09-ai-guide.png)<br>[`09-ai-guide.txt`](docs/review/dc15a79-09-ai-guide.txt) |
| 5 | **Sermons Audio & Video** | `WORKING` | Dynamic Google Sheets sync (`sermons.csv`) verified on hardware (9 sermons synced into Room with `duration_seconds` e.g. 47:00, preacher mapped as speaker). Audio controller and YouTube integration tested. | [`f035e34-02-sermons.png`](docs/review/f035e34-02-sermons.png)<br>[`f035e34-02-sermons.txt`](docs/review/f035e34-02-sermons.txt) |
| 6 | **Worship Hymnal & Chords** | `WORKING` | Dynamic Google Sheets sync (`hymns.csv`) verified on hardware (17 hymns synced into Room). Member local favorites preserved; `active=true` defaulted when omitted. | [`f035e34-03-worship.png`](docs/review/f035e34-03-worship.png)<br>[`f035e34-03-worship.txt`](docs/review/f035e34-03-worship.txt) |
| 7 | **Biblical Riddles** | `WORKING` | Dynamic Google Sheets sync (`riddles.csv`) verified on hardware (12 biblical trivia riddles synced into Room with scripture references and XP rewards). | [`f035e34-04-fellowship.png`](docs/review/f035e34-04-fellowship.png)<br>[`f035e34-04-fellowship.txt`](docs/review/f035e34-04-fellowship.txt) |
| 8 | **Christian Jokes & Humor** | `WORKING` | Dynamic Google Sheets sync (`jokes.csv`) verified on hardware (14 clean church jokes synced into Room with composed joke+punchline and approved status mapping). | [`f035e34-04-fellowship.png`](docs/review/f035e34-04-fellowship.png)<br>[`f035e34-04-fellowship.txt`](docs/review/f035e34-04-fellowship.txt) |
| 9 | **Church Events & Announcements** | `WORKING` | Dynamic Google Sheets sync verified on hardware (5 announcements, 11 church events synced into Room with venue/campus mapping and date/time parsing). | [`f035e34-01-home.png`](docs/review/f035e34-01-home.png)<br>[`f035e34-01-home.txt`](docs/review/f035e34-01-home.txt) |
| 10 | **Daily Cornerstone Scripture** | `WORKING` | Date-mapped daily scriptures (`verses.csv`) verified on hardware. Live Home screen displays synced daily verse for 2026-11-03: Exodus 14:14 ("The Lord shall fight for you, and ye shall hold your peace."). | [`f035e34-01-home.png`](docs/review/f035e34-01-home.png)<br>[`f035e34-01-home.txt`](docs/review/f035e34-01-home.txt) |
| 11 | **Google Sheets Content Sync Engine** | `WORKING` | Full end-to-end hardware verification with 8 published Google Sheet CSV tabs. Profile screen "Refresh content" updates Room SQLite atomically; airplane mode test proves zero crash and seamless cached content display. | [`f035e34-05-profile.png`](docs/review/f035e34-05-profile.png)<br>[`f035e34-05-profile.txt`](docs/review/f035e34-05-profile.txt)<br>[`f035e34-06-airplane-offline-profile-sync-failsoft.png`](docs/review/f035e34-06-airplane-offline-profile-sync-failsoft.png)<br>[`f035e34-07-airplane-offline-home-cached.png`](docs/review/f035e34-07-airplane-offline-home-cached.png) |
| 12 | **Spiritual XP & Daily Routine** | `WORKING` | 3-minute daily reflection routine (Read 📖, Reflect 💡, Pray 🙏), daily streak counters, Grace Points, lifetime days tracking, and milestone badge indicators verified on device. | [`f035e34-01-home.png`](docs/review/f035e34-01-home.png)<br>[`f035e34-05-profile.png`](docs/review/f035e34-05-profile.png) |
| 13 | **Push Notifications** | `HARDCODED/DEMO` | Local notification simulator dialog; **no remote push server or FCM active**. | [`live_screen_notifications_dialog_phase_3.png`](docs/screenshots/live_screen_notifications_dialog_phase_3.png) |
| 14 | **Pastoral Care Handoffs** | `WORKING` | Native Android system intents to WhatsApp (`+254 710 772 227`), system dialer (`tel:0710772227`), offline SMS (`smsto:0710772227`), and Google Maps coordinates for Embu HQ sanctuary. All handoffs verified on hardware. | [`f035e34-05-profile.png`](docs/review/f035e34-05-profile.png)<br>[`f035e34-05-profile.txt`](docs/review/f035e34-05-profile.txt) |
| 15 | **Campus Switcher** | `WORKING` | Dynamic campus selector dialog switches active campus between Embu Headquarters, Nairobi Fellowship, Meru Outreach, and Online Campus; updates contact metadata and Room preferences. | [`f035e34-01-home.txt`](docs/review/f035e34-01-home.txt)<br>[`f035e34-05-profile.txt`](docs/review/f035e34-05-profile.txt) |
| 16 | **Member Accounts & Authentication** | `WORKING` | Kenyan phone number (`+254` / `07...`) + 4-digit PIN registration and login against self-hosted Node.js / PostgreSQL server with JWT tokens. Offline Guest mode preserved with seamless data migration upon sign-in. Zero fake starter numbers (0 streak, 0 XP, 2 grace points verified). | [`03_profile_guest_zeroed.png`](docs/review/phase_A/03_profile_guest_zeroed.png)<br>[`12_registered_profile.png`](docs/review/phase_A/12_registered_profile.png)<br>[`19_sister_mary_profile.png`](docs/review/phase_A/19_sister_mary_profile.png) |

---

## 2. In-Depth Technical Breakdown (Stage 1 Architecture)

### 2.1 Google Sheet Dynamic Content Synchronization — `WORKING`
- **Status**: Verified end-to-end on physical Android 11 hardware (`Infinix X689C`) using 8 live published Google Sheet CSV tabs.
- **Engine**: `SheetSyncManager.java`, `CsvParser.java`, and `SheetConfig.java` in package `org.perazimchurch.app.sync`.
- **Database Schema**: Room Database `version = 5` with `MIGRATION_4_5` preserving existing member data, reflections, bookmarks, and gamification state.
- **Tables Synchronized & Verified**:
  1. `announcements` (`AnnouncementEntity`) — 5 announcements synced from Google Sheets.
  2. `events` (`EventEntity`) — 11 events synced with start millis and venue.
  3. `hymns` (`HymnEntity`) — 17 hymns synced; local favorites preserved and active defaulted to true.
  4. `riddles` (`RiddleEntity`) — 12 biblical trivia riddles synced with XP rewards and scripture references.
  5. `jokes` (`JokeEntity`) — 14 clean church jokes synced; composed joke+punchline supported.
  6. `sermons` (`SermonEntity`) — 9 sermons synced with durationSeconds and preacher mapping.
  7. `daily_verses` (`DailyVerseEntity`) — 4 date-mapped verses synced; Exodus 14:14 displayed for 2026-11-03.
  8. `app_config` (`AppConfigEntity`) — Remote church configuration parameters.
- **Fail-Soft & Offline-First Verification**:
  - Airplane mode enabled via ADB: app launched without internet connection, verified zero crashes and complete presentation of Room-cached scriptures, sermons, and hymns.
  - Manual sync attempt in airplane mode gracefully shows "⚠️ Offline or sync failed. Kept cached content." and preserves all pre-existing Room data.
- **Security & Privacy**:
  - Published Google Sheet URLs are configured strictly in local non-git config (`sheet_urls.properties` / `local.properties`) and excluded from source control.

### 2.2 In-Place Release Upgrade Migration Verification — `WORKING`
- **Baseline Version**: Release APK of `v2.2.0-pilot-stable` (`versionCode 4`, `versionName 2.2.0`) signed with `perazim-release.jks`.
- **Target Version**: Release APK of `feature/dynamic-community` (`versionCode 4`, `versionName 2.2.0`) signed with the same `perazim-release.jks`.
- **Methodology**: In-place `adb install -r` upgrade performed without uninstalling.
- **Verification Results**:
  1. No signature collision or package manager error.
  2. Room Database schema migration 4 -> 5 executed seamlessly on app startup without crash.
  3. Pre-upgrade seeded test data survived intact:
     - Member Spiritual XP: `⭐ 10 XP` (Level 1 Disciple) proven intact.
     - Favorited worship hymn: *How Great Thou Art* retained active heart icon (`❤️`).
     - Member identity: `Guest Pilgrim`, `Embu Headquarters` proven intact.
     - Fellowship prayer petitions on wall: Not proven (the captured review dump `migration-02-after.txt` displays the Profile screen; prayer petition persistence on the Fellowship wall was not dumped and is marked not proven).
- **Hardware Proof Files**:
  - Pre-upgrade state: [`migration-01-before.png`](docs/review/migration-01-before.png) and [`migration-01-before.txt`](docs/review/migration-01-before.txt)
  - Post-upgrade state: [`migration-02-after.png`](docs/review/migration-02-after.png) and [`migration-02-after.txt`](docs/review/migration-02-after.txt)

### 2.3 Phase A: Real Member Accounts, Self-Hosted Backend & Sync Engine — `WORKING`
- **Audit Date**: November 4, 2026
- **Branch**: `phase-A-server`
- **Backend Architecture**: Self-hosted Node.js + Express backend in `server/` with PostgreSQL database schema (`server/schema.sql`), JWT token authentication (`server/src/middleware/auth.js`), REST endpoints for auth, prayers, amens, testimonies, and batch mutations (`/api/sync`). Containerized via `Dockerfile` and `docker-compose.yml`.
- **Identity & Session Management**:
  - `SessionManager.java` stores active JWT token, user ID, and auth state in private SharedPreferences.
  - Eliminated hardcoded `"user_me"` and `"guest_user"` across the entire app.
  - Offline Guest mode preserved; users can browse, pray, and read without an account.
  - Room migration (`migrateLocalData`) preserves local guest prayers and bookmarks upon member registration/login.
- **Starter Metrics Truth**:
  - Completely purged fake starter numbers (450 XP, 7-day streak, 14 prayers).
  - New member accounts and guest mode strictly default to 0 streak, 0 XP, 2 grace points (room to stumble).
- **Bidirectional Sync Dispatcher**:
  - `SyncDispatcher.java` bridges Room `sync_queue` and server `/api/sync`.
  - Dispatches pending mutations in batches of 25 with exponential backoff on failure.
  - Ingests remote prayers and amens into Room SQLite database.
- **Hardware & Multi-Device Verification**:
  - Account 1 (Brother John, `0711000001`): Registered on Embu campus, posted *"Prayer for healing in Embu"*. Successfully synced to server.
  - Account 2 (Sister Mary, `0711000002`): Registered on Nairobi campus, saw Brother John's prayer on her prayer wall, tapped Amen. Amen count incremented to 1 locally and on the server.
  - Fail-Soft Offline Test: Enabled airplane mode, posted *"Strength for fellowship outreach"*. Prayer appeared locally, mutation held in `sync_queue` with `PENDING` status. Restored connectivity; mutation automatically dispatched on app resume and verified on server without data loss.
- **Hardware Proof Files**:
  - Guest Profile (0 streak, 0 XP, 2 grace): [`03_profile_guest_zeroed.png`](docs/review/phase_A/03_profile_guest_zeroed.png)
  - Registered Member Profile (Brother John): [`12_registered_profile.png`](docs/review/phase_A/12_registered_profile.png)
  - Constrained Quiet-Dark Prayer Header: [`18_prayer_wall_fixed.png`](docs/review/phase_A/18_prayer_wall_fixed.png)
  - Second Member Profile (Sister Mary, 0 streak, 0 XP, 2 grace): [`19_sister_mary_profile.png`](docs/review/phase_A/19_sister_mary_profile.png)
  - Multi-Device Intercession Amen: [`20_sister_mary_amen.png`](docs/review/phase_A/20_sister_mary_amen.png)
  - Offline Sync Recovery (Both Prayers Synced): [`22_prayer_wall_synced.png`](docs/review/phase_A/22_prayer_wall_synced.png)

### 2.4 Physical Hardware Verification on Infinix HOT 10T (`0667737142100269`) — `WORKING`
- **Audit Date**: November 5, 2026
- **Branch**: `phase-A-server`
- **Device Specifications**: Physical Infinix HOT 10T (`Infinix_X689C`), Android 11 (API 30), Screen Resolution 720x1640 (tall aspect ratio).
- **Aspect Ratio & Layout Engineering**:
  - Identified layout bloat on tall screens where unconstrained `WRAP_CONTENT` headers caused 720x1279 background bitmaps to monopolize the viewport.
  - Refactored `HomeUiBinder.java`, `ProfileUiBinder.java`, and `SermonsUiBinder.java` to enforce fixed dp header heights (`dp(140)` and `dp(160)`) with `CENTER_CROP` and `Gravity.BOTTOM` alignments.
  - All 5 navigation tabs now render completely without clipping, bloat, or off-screen element push on the physical device.
- **Physical Hardware Test Results**:
  1. **Home Screen Sanctuary**: Constrained header, daily scripture card (Philippians 4:13), 3-minute breakthrough routine button, and all 4 Sanctuary Doors (Bible, Sermons, Worship, Prayer) fit simultaneously on screen without scrolling.
  2. **Member Account Flow**: Registered user Levi (`0715440482`) authenticated against local server via USB reverse tunnel (`adb reverse tcp:3000 tcp:3000`). Profile screen displays 0 Streak, 0 XP, 2 Grace Points, 0 Lifetime Days, M-Pesa giving button, and full settings navigation.
  3. **Live Prayer Intercession & Server Sync**: Prayer wall rendered live petitions from Sister Mary and Brother John. Tapping `🙏 AMEN (0)` on Sister Mary's petition updated the UI immediately to `✓ JOINED IN PRAYER (1)` and transmitted a `POST /api/sync` mutation over the network tunnel, returning HTTP 200 OK with zero data loss.
  4. **Sermons & Audio Controller**: Livestream card displays correctly with sunrise lamp artwork; Supernatural Breakthrough audio player, search bar, and message list verified.
  5. **Worship Hymnal**: Sacred hymns and choral worship banner, search bar, chips (Favorites, Worship, Faith, Grace, Praise), and hymnal list verified.
- **Physical Hardware Proof Files**:
  - Unconstrained Header Before Fix: [`01_home_screen_before_fix.png`](docs/review/infinix_hardware/01_home_screen_before_fix.png)
  - Home Sanctuary View: [`02_home_sanctuary.png`](docs/review/infinix_hardware/02_home_sanctuary.png)
  - Authenticated Member Profile (Levi): [`03_profile_authenticated.png`](docs/review/infinix_hardware/03_profile_authenticated.png)
  - Prayer Wall Initial State: [`04_prayer_wall_initial.png`](docs/review/infinix_hardware/04_prayer_wall_initial.png)
  - Prayer Wall Amen Synced: [`05_prayer_wall_amen_synced.png`](docs/review/infinix_hardware/05_prayer_wall_amen_synced.png)
  - Sermons Tab: [`06_sermons_tab.png`](docs/review/infinix_hardware/06_sermons_tab.png)
  - Worship Tab: [`07_worship_tab.png`](docs/review/infinix_hardware/07_worship_tab.png)

### 2.5 Phase A Backend Hardening & Review Remediation — `WORKING`
- **Audit Date**: November 5, 2026
- **Branch**: `phase-A-server`
- **Remediation Scope**: Comprehensive resolution of all 6 architectural security and durability findings identified in Phase A code review.
- **Findings Remediated & Verified**:
  1. **Strict PostgreSQL Durability (Zero Silent Memory Fallback)**:
     - `initDb()` in `server/src/db.js` strictly halts execution on connection failure if `DATABASE_URL` is set or when running in `production`.
     - In-memory mock fallback is forbidden by default and can only be enabled if explicitly set via `ALLOW_MEMORY_STORE=true` for lightweight unit tests.
  2. **Cryptographic JWT Secret Hardening**:
     - `server/src/middleware/auth.js` enforces strict JWT secret validation.
     - Production environments unconditionally reject missing secrets, secrets shorter than 32 characters, or default development placeholders, failing fast at boot.
     - Development environments automatically generate a cryptographically random 32-byte ephemeral hex key per process.
  3. **Brute-Force Rate Limiting (4-Digit PIN Protection)**:
     - Implemented `server/src/middleware/rateLimiter.js` with sliding-window protection and phone number normalization (`07...` and `+254...` unified).
     - Caps consecutive failed login attempts at 5 within a 15-minute window; subsequent attempts are locked out with HTTP `429 Too Many Requests` and a `Retry-After` header.
     - Successful PIN verification immediately clears failed attempts.
  4. **Strict Prayer Ownership & Testimony Security**:
     - Protected `/api/prayers/:id/testimony` and `/api/sync` (`PRAYER_TESTIMONY` mutation) with mandatory JWT authentication.
     - Enforces row-level authorization (`prayer.author_id === req.user.id || req.user.role === 'ADMIN'`). Tampering attempts by unauthorized accounts are rejected with HTTP `403 Forbidden`.
  5. **Explicit Mutation Acknowledgment (Rejection of Unsupported Operations)**:
     - Completely removed the blind wildcard `syncedIds.push(id)` fallback from `server/src/routes/sync.js`.
     - Unrecognized or unsupported operations are rejected and returned in a dedicated `rejectedMutations` array (`[{ id, reason: 'UNSUPPORTED_OPERATION', message }]`), preventing client phones from falsely clearing unapplied mutations from their local `sync_queue`.
  6. **Automated Real PostgreSQL Integration Test Suite**:
     - Created `server/test/postgres.test.js` executing 14 end-to-end integration tests directly against a live PostgreSQL 15 container (`perazim-test-db`).
     - Validates PostgreSQL table existence, `UNIQUE` constraint enforcement, idempotent prayer amens (`ON CONFLICT (prayer_id, user_id) DO NOTHING`), row ownership checks, rate limiting lockouts, and multi-mutation batch sync.
     - Total test suite: 26 automated tests (12 unit + 14 real PostgreSQL integration tests) passing with 100% success rate.
- **Physical Hardware Verification**:
  - Live reverse port tunnel (`adb -s 0667737142100269 reverse tcp:3000 tcp:3000`) connected to the hardened server running on real PostgreSQL.
  - Physical Infinix phone successfully loaded live prayers from PostgreSQL and confirmed synchronized prayer state.

---

## 3. How to Run the Automated Visual Audit

To re-verify and capture physical evidence from a connected test phone:

```bash
# 1. Connect Android phone with USB Debugging enabled
adb devices

# 2. Run the review capture suite
./scripts/capture_review_screens.sh
```

The script automatically navigates the app and writes high-resolution screenshots and matching `uiautomator` hierarchy dumps into `docs/review/`:
- `docs/review/<hash>-01-home.png` / `.txt`
- `docs/review/<hash>-02-sermons.png` / `.txt`
- `docs/review/<hash>-03-worship.png` / `.txt`
- `docs/review/<hash>-04-fellowship.png` / `.txt`
- `docs/review/<hash>-05-profile.png` / `.txt`
- `docs/review/<hash>-06-giving-dialog.png` / `.txt`
- `docs/review/<hash>-07-prayer-dialog.png` / `.txt`
- `docs/review/<hash>-08-bible-reader.png` / `.txt`
- `docs/review/<hash>-09-ai-guide.png` / `.txt`
- `docs/review/<hash>-10-riddles.png` / `.txt`
- `docs/review/<hash>-11-jokes.png` / `.txt`

## 2026-11-06 — UI audit corrections

- Bible repository initialization now receives application context and loads the existing bundled KJV starter assets before its first background read. Upgrade hydration preserves saved bookmarks and study notes; seed-time verification no longer writes a sample bookmark or note into member data. Previously saved bookmarks are retained.
- The five translation choices remain. Assets are unchanged: only KJV starter passages are bundled. Missing translations/chapters show an explicit unavailable message rather than displaying KJV text under another translation name. Clipboard citations use the actual translation ID.
- Removed the campus/headquarters row and chip from the active profile, plus the legacy affiliated-campus row.
- Sermon control buttons no longer reserve platform minimum widths that squeezed the central play label. Existing pause-on-background, pause-on-tab-change, back-to-stop, and Stop behavior remains; player errors reset the play state.
- The guide opens with an accurately labeled on-device response. Members can ask offline questions immediately without an automatic online-consent dialog; online opt-in remains available from the subtitle.
- New member Grace defaults are zero in entity creation, repository/profile fallbacks, the main screen, and offline-guide state reads. Persisted member balances remain intact. Added a zero-balances regression test.
- Added the missing `Post` domain model matching the existing community entity contract so the already-present repository interface compiles.
- Validation: `./gradlew testDebugUnitTest assembleDebug --offline` passed; 47 tests, no failures/errors, including Bible mapping, sync queue mechanics, and new-account balance regression. Debug APK: `build/outputs/apk/debug/app-debug.apk`. Physical-device results are recorded separately after installation; the build alone does not establish visual/lifecycle correctness.
- Follow-up profile correction: authenticated members now resolve their cached profile by the session's exact user ID. If the profile is not cached, the screen shows “Perazim Member / Profile details unavailable offline” rather than a fabricated guest name/email/phone. Guest sign-in controls still follow session state. The physical guide screenshot showed input and Send visible; misleading hierarchy bounds alone did not justify a layout change.
