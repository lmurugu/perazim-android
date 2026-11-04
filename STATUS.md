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
