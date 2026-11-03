# Perazim Mission Church Android App — Feature Status & Honesty Audit

> **Target Version**: `2.2.0` (VersionCode `4`)  
> **Package ID**: `org.perazimchurch.app`  
> **Target Audience**: 100-Member Pilot (Embu Headquarters & Satellite Campuses)  
> **Audit Date**: November 3, 2026  
> **Audit Branch**: `feature/dynamic-community` (Stage 1: Google Sheet Dynamic Content)  
> **Audit Commit**: `1d621ccd0cb55f214cfc832d86e07943e5080b14`  
> **Policy**: Zero unsubstantiated claims. Every feature is strictly verified against actual implementation and backed by hardware screenshots and `uiautomator` text dumps.

---

## 1. Feature Status Summary Matrix

| # | Feature Area | Status | Summary of Reality / Architecture Limitation | Proof File(s) |
|---|--------------|--------|----------------------------------------------|---------------|
| 1 | **Offline Bible (5 Translations)** | `WORKING` | Pre-seeded SQLite database (`perazim_database.db`) with KJV, Swahili SUV, NIV, ESV, NKJV. Instant offline queries, 66 books, chapter navigation, text search. No cloud sync. | [`08-bible-reader.png`](docs/review/dc15a79-08-bible-reader.png)<br>[`08-bible-reader.txt`](docs/review/dc15a79-08-bible-reader.txt) |
| 2 | **Lipa na M-Pesa Giving** | `PARTIAL` | Copies Paybill `4069983` to clipboard and cascades to Safaricom M-Pesa app, SIM Toolkit, or `*334#`. **No automated Daraja C2B/STK push API**; reconciliation requires manual treasurer check. Fake (+20 XP) giving reward removed. | [`06-giving-dialog.png`](docs/review/dc15a79-06-giving-dialog.png)<br>[`06-giving-dialog.txt`](docs/review/dc15a79-06-giving-dialog.txt) |
| 3 | **Community Prayer Wall** | `PARTIAL` | Requests stored in local Room SQLite database (`prayers` table); Amen counters update locally. Pastoral handoff triggers real WhatsApp/SMS to Bishop (+254 710 772 227). **No real-time multi-member cloud sync** across devices (Stage 3). | [`04-fellowship.png`](docs/review/dc15a79-04-fellowship.png)<br>[`07-prayer-dialog.png`](docs/review/dc15a79-07-prayer-dialog.png)<br>[`07-prayer-dialog.txt`](docs/review/dc15a79-07-prayer-dialog.txt) |
| 4 | **AI Breakthrough Guide** | `HARDCODED/DEMO` | Fast, deterministic, on-device rule engine (`PerazimAiEngine.java`) using regex keyword matching and canned responses to dispatch deep links. **Generative AI cloud relay is scheduled for Stage 2**. | [`09-ai-guide.png`](docs/review/dc15a79-09-ai-guide.png)<br>[`09-ai-guide.txt`](docs/review/dc15a79-09-ai-guide.txt) |
| 5 | **Sermons Audio & Video** | `PARTIAL` | YouTube channel integration parses live XML RSS feed and launches YouTube app/web. Built-in audio player plays local asset streams; sermon metadata and media URLs dynamically updatable via Google Sheets `sermons.csv`. | [`02-sermons.png`](docs/review/dc15a79-02-sermons.png)<br>[`02-sermons.txt`](docs/review/dc15a79-02-sermons.txt) |
| 6 | **Worship Hymnal & Chords** | `UNVERIFIED` | 10 classic worship hymns pre-seeded in Room. Code written for dynamic CSV sync (`hymns.csv`); favorites and chord transpositions preserved locally. **Unverified against a live Google Sheet** until real published URLs are connected. | [`03-worship.png`](docs/review/dc15a79-03-worship.png)<br>[`03-worship.txt`](docs/review/dc15a79-03-worship.txt) |
| 7 | **Biblical Riddles** | `UNVERIFIED` | Biblical trivia riddles in Room SQLite. Dynamic rotation and CSV sync logic implemented (`riddles.csv`). **Unverified against a live Google Sheet** until real published URLs are connected. | [`10-riddles.png`](docs/review/dc15a79-10-riddles.png)<br>[`10-riddles.txt`](docs/review/dc15a79-10-riddles.txt) |
| 8 | **Christian Jokes & Humor** | `UNVERIFIED` | Clean church humor in Room SQLite. Sync logic implemented (`jokes.csv`). **Unverified against a live Google Sheet** until real published URLs are connected. | [`11-jokes.png`](docs/review/dc15a79-11-jokes.png)<br>[`11-jokes.txt`](docs/review/dc15a79-11-jokes.txt) |
| 9 | **Church Events & Announcements** | `UNVERIFIED` | Church bulletins and upcoming events in Room SQLite. Filtering by `active = 1` and CSV sync implemented (`events.csv`, `announcements.csv`). **Unverified against a live Google Sheet** until real published URLs are connected. | [`01-home.png`](docs/review/dc15a79-01-home.png)<br>[`01-home.txt`](docs/review/dc15a79-01-home.txt) |
| 10 | **Daily Cornerstone Scripture** | `UNVERIFIED` | Date-mapped daily scriptures (`daily_verses` table) with fallback to 2 Samuel 5:20. CSV sync implemented (`verses.csv`). **Unverified against a live Google Sheet** until real published URLs are connected. | [`01-home.png`](docs/review/dc15a79-01-home.png)<br>[`01-home.txt`](docs/review/dc15a79-01-home.txt) |
| 11 | **Google Sheets Content Sync Engine** | `UNVERIFIED` | Background sync engine (`SheetSyncManager.java`, `CsvParser.java`, `SheetConfig.java`) with RFC 4180 parser, 10s timeout, Room upsert, local-only field preservation, and Profile UI wired. **Engine unit tests pass (9/9), but end-to-end sync is UNVERIFIED until real Google Sheet URLs are entered**. | [`05-profile.png`](docs/review/dc15a79-05-profile.png)<br>[`05-profile.txt`](docs/review/dc15a79-05-profile.txt) |
| 12 | **Spiritual XP & Daily Routine** | `WORKING` | 3-minute daily reflection routine (Read 📖, Reflect 💡, Pray 🙏), daily streak counters, Grace Points, and Streak Freeze function seamlessly on device via `GamificationStore`. | [`01-home.png`](docs/review/dc15a79-01-home.png)<br>[`05-profile.png`](docs/review/dc15a79-05-profile.png) |
| 13 | **Push Notifications** | `HARDCODED/DEMO` | Local notification simulator dialog; **no remote push server or FCM active**. | [`live_screen_notifications_dialog_phase_3.png`](docs/screenshots/live_screen_notifications_dialog_phase_3.png) |
| 14 | **Pastoral Care Handoffs** | `WORKING` | Native Android system intents to WhatsApp (`+254 710 772 227`), system dialer (`tel:0710772227`), offline SMS (`smsto:0710772227`), and Google Maps coordinates for Embu HQ sanctuary. All handoffs verified on hardware. | [`05-profile.png`](docs/review/dc15a79-05-profile.png)<br>[`05-profile.txt`](docs/review/dc15a79-05-profile.txt) |
| 15 | **Campus Switcher** | `WORKING` | Dynamic campus selector dialog switches active campus between Embu Headquarters, Nairobi Fellowship, Meru Outreach, and Online Campus; updates contact metadata and Room preferences. | [`01-home.txt`](docs/review/dc15a79-01-home.txt)<br>[`05-profile.txt`](docs/review/dc15a79-05-profile.txt) |

---

## 2. In-Depth Technical Breakdown (Stage 1 Architecture)

### 2.1 Google Sheet Dynamic Content Synchronization — `UNVERIFIED` (Awaiting Real Google Sheet URLs)
- **Status**: Implementation & unit tests complete (9/9 unit tests passing); full end-to-end verification pending Levi providing the 8 published Google Sheet CSV URLs.
- **Engine**: `SheetSyncManager.java`, `CsvParser.java`, and `SheetConfig.java` in package `org.perazimchurch.app.sync`.
- **Database Schema**: Room Database `version = 5` with `MIGRATION_4_5` preserving existing member data, reflections, bookmarks, and gamification state.
- **Tables Synchronized**:
  1. `announcements` (`AnnouncementEntity`) — Weekly announcements with priority and expiration.
  2. `events` (`EventEntity`) — Upcoming church gatherings and campus events.
  3. `hymns` (`HymnEntity`) — Worship songs with multiline lyrics, chord charts, musical key, and BPM.
  4. `riddles` (`RiddleEntity`) — Biblical trivia riddles with answers and difficulty categories.
  5. `jokes` (`JokeEntity`) — Family-safe Christian humor.
  6. `sermons` (`SermonEntity`) — Sermon series metadata and YouTube/audio links.
  7. `daily_verses` (`DailyVerseEntity`) — Date-mapped scriptures (`YYYY-MM-DD`).
  8. `app_config` (`AppConfigEntity`) — Remote church configuration parameters.
- **Fail-Soft Principles**:
  - Connect and read timeouts capped strictly at 10 seconds.
  - If network is unavailable or URLs return errors, the app falls back to cached local Room data.
  - Required header validation prevents corrupted CSV uploads from clearing local content.
  - If a row is malformed or missing required IDs, it is cleanly skipped while valid rows are saved.
- **Secretary Workflow**: Managed via Google Sheets "Publish to the web" with zero server maintenance. Documented in `docs/CONTENT_GUIDE.md` and `docs/SETUP_FOR_LEVI.md`.

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
