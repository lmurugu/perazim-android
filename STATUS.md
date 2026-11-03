# Perazim Mission Church Android App — Feature Status & Honesty Audit

> **Target Version**: `2.2.0` (VersionCode `4`)  
> **Package ID**: `org.perazimchurch.app`  
> **Target Audience**: 100-Member Pilot (Embu Headquarters & Satellite Campuses)  
> **Audit Date**: November 2026  
> **Audit Branch**: `main`  
> **Policy**: Zero unsubstantiated claims. Every feature is strictly verified against actual implementation and backed by hardware screenshots and `uiautomator` text dumps.

---

## 1. Feature Status Summary Matrix

| # | Feature Area | Status | Summary of Reality / Architecture Limitation | Proof File(s) |
|---|--------------|--------|----------------------------------------------|---------------|
| 1 | **Offline Bible (5 Translations)** | `WORKING` | Pre-seeded SQLite database (`perazim.db`) with KJV, Swahili SUV, NIV, ESV, NKJV. Instant offline queries, 66 books, chapter navigation, text search. No cloud sync. | [`08-bible-reader.png`](docs/review/412943a-08-bible-reader.png)<br>[`08-bible-reader.txt`](docs/review/412943a-08-bible-reader.txt) |
| 2 | **Lipa na M-Pesa Giving** | `PARTIAL` | Copies Paybill `4069983` to clipboard and cascades to Safaricom M-Pesa app, SIM Toolkit, or `*334#`. **No automated Daraja C2B/STK push API**; reconciliation requires manual treasurer check. Fake (+20 XP) giving reward removed. | [`06-giving-dialog.png`](docs/review/412943a-06-giving-dialog.png)<br>[`06-giving-dialog.txt`](docs/review/412943a-06-giving-dialog.txt) |
| 3 | **Community Prayer Wall** | `PARTIAL` | Requests stored in local Room SQLite database (`prayer_requests` table); Amen counters update locally. Pastoral handoff triggers real WhatsApp/SMS to Bishop (+254 710 772 227). **No real-time multi-member cloud sync** across devices. | [`04-fellowship.png`](docs/review/412943a-04-fellowship.png)<br>[`07-prayer-dialog.png`](docs/review/412943a-07-prayer-dialog.png)<br>[`07-prayer-dialog.txt`](docs/review/412943a-07-prayer-dialog.txt) |
| 4 | **AI Breakthrough Guide** | `HARDCODED/DEMO` | Fast, deterministic, on-device rule engine (`PerazimAiEngine.java`) using regex keyword matching and canned responses to dispatch deep links. **Not an on-device neural network or LLM**. | [`09-ai-guide.png`](docs/review/412943a-09-ai-guide.png)<br>[`live_screen_chat_dialog_phase_3.png`](docs/screenshots/live_screen_chat_dialog_phase_3.png) |
| 5 | **Sermons Audio & Video** | `PARTIAL` | YouTube channel integration parses live XML RSS feed and launches YouTube app/web. Built-in audio player plays local asset streams; **Google Drive sermon library is not dynamically connected to a media streaming CDN**. Mini-player idle state hidden. | [`02-sermons.png`](docs/review/412943a-02-sermons.png)<br>[`02-sermons.txt`](docs/review/412943a-02-sermons.txt) |
| 6 | **Worship Hymnal & Chords** | `HARDCODED/DEMO` | 10 classic worship hymns and chords pre-seeded in Room SQLite from `assets/database/perazim_seed.json`. Chord transpose UI and favorite bookmarks work locally; **no online hymnal repository or cloud additions**. | [`03-worship.png`](docs/review/412943a-03-worship.png)<br>[`03-worship.txt`](docs/review/412943a-03-worship.txt) |
| 7 | **Biblical Riddles** | `HARDCODED/DEMO` | Fixed collection of 10 trivia riddles baked into the app database and `RiddlesActivity.java`. Interactive answer reveal works client-side; **no server-driven rotation**. | [`04-fellowship.png`](docs/review/412943a-04-fellowship.png) (lines 34–38)<br>[`04-fellowship.txt`](docs/review/412943a-04-fellowship.txt) |
| 8 | **Christian Jokes & Humor** | `HARDCODED/DEMO` | Fixed array of 10 clean church humor jokes embedded in `JokesActivity.java` and `assets/database/perazim_seed.json`. Displays static jokes; **no dynamic feed**. | [`04-fellowship.png`](docs/review/412943a-04-fellowship.png) (lines 39–40)<br>[`04-fellowship.txt`](docs/review/412943a-04-fellowship.txt) |
| 9 | **Church Events & Calendar** | `HARDCODED/DEMO` | Sample church events (Sunday Breakthrough Service, Mid-week Deliverance) seeded into Room SQLite. **No Google Calendar API or live church CMS sync**. | [`04-fellowship.txt`](docs/review/412943a-04-fellowship.txt) |
| 10 | **Spiritual XP & Daily Routine** | `WORKING` | 3-minute daily reflection routine (Read 📖, Reflect 💡, Pray 🙏), daily streak counters, Grace Points, and Streak Freeze function seamlessly on device via `GamificationStore` (Encrypted/SharedPreferences + Room). **Device-local only; no global cloud leaderboard**. | [`01-home.png`](docs/review/412943a-01-home.png)<br>[`05-profile.png`](docs/review/412943a-05-profile.png)<br>[`05-profile.txt`](docs/review/412943a-05-profile.txt) |
| 11 | **Push Notifications** | `HARDCODED/DEMO` | Notification simulator dialog in Fellowship tab; **no Firebase Cloud Messaging (FCM) or remote push server configured**. | [`live_screen_notifications_dialog_phase_3.png`](docs/screenshots/live_screen_notifications_dialog_phase_3.png) |
| 12 | **Pastoral Care Handoffs** | `WORKING` | Native Android system intents to WhatsApp (`+254 710 772 227`), system dialer (`tel:0710772227`), offline SMS (`smsto:0710772227`), and Google Maps coordinates for Embu HQ sanctuary. All handoffs verified on hardware. | [`05-profile.png`](docs/review/412943a-05-profile.png)<br>[`05-profile.txt`](docs/review/412943a-05-profile.txt) |
| 13 | **In-App Feedback & Diagnostics**| `WORKING` | Dedicated "🛠️ Feedback & Report Issue" button in Profile tab generates prefilled diagnostic report (version 2.2.0, device model, Android SDK) and deep-links directly to WhatsApp (+254 710 772 227). | [`05-profile.png`](docs/review/412943a-05-profile.png)<br>[`05-profile.txt`](docs/review/412943a-05-profile.txt) (lines 65–68) |
| 14 | **Campus Switcher** | `WORKING` | Dynamic campus selector dialog switches active campus between Embu Headquarters, Nairobi Fellowship, Meru Outreach, and Online Campus; updates contact metadata and Room preferences. | [`01-home.txt`](docs/review/412943a-01-home.txt)<br>[`05-profile.txt`](docs/review/412943a-05-profile.txt) (line 11) |

---

## 2. In-Depth Technical Breakdown

### 2.1 Offline Holy Scriptures (Bible) — `WORKING`
- **Engine**: SQLite / Room Database (`perazim.db`) located in app internal storage.
- **Translations**:
  1. King James Version (KJV)
  2. Swahili Union Version (SUV - Kiswahili)
  3. New International Version (NIV)
  4. English Standard Version (ESV)
  5. New King James Version (NKJV)
- **Features**: 66 canonical books (39 OT / 27 NT), chapter-by-chapter pager, verse click actions (bookmark, study notes, copy), instant full-text search.
- **Honesty Assessment**: 100% offline and operational on device without internet connectivity. Search is purely local SQL `LIKE` queries. No cloud backup for member bookmarks or personal notes.

### 2.2 Lipa na M-Pesa Giving — `PARTIAL`
- **Paybill**: `4069983` (Account: `PERAZIM-TITHE`, `PERAZIM-OFFERING`, `PERAZIM-BUILDING`)
- **Execution Cascade**:
  1. Copies Paybill number and amount to Android system clipboard.
  2. Checks for official Safaricom M-Pesa app (`com.safaricom.mpesa.lifestyle`).
  3. If missing, attempts to launch Safaricom SIM Toolkit (`com.android.stk`).
  4. If STK is restricted by OEM security policies, launches native phone dialer with `*334#` USSD string.
- **Honesty Assessment**: Client-side handoff works reliably on Kenyan devices. However, there is **no direct Safaricom Daraja REST API integration**. The app cannot detect whether a member completed or cancelled the payment, nor does it receive M-Pesa callback confirmations. The button text explicitly states `Complete M-Pesa Giving (Paybill 4069983)` and does not award unverified XP.

### 2.3 Community Prayer Wall — `PARTIAL`
- **Storage**: Local Room DAO (`PrayerDao.java`).
- **Interaction**: Users can post prayer requests, submit praise reports, and increment "🙏 Amen" counts.
- **Pastoral Escalation**: Urgent requests provide an instant "Send to Bishop via WhatsApp / SMS" button with pre-composed text.
- **Honesty Assessment**: Requests are saved in the user's local phone database only. Without a remote cloud backend (Firebase / PostgreSQL), prayers posted on Member A's phone will **not** appear on Member B's phone. Suitable for personal prayer tracking and direct pastoral dispatch, but not yet a shared church-wide social wall.

### 2.4 Perazim Breakthrough Guide (AI Assistant) — `HARDCODED/DEMO`
- **Engine**: `PerazimAiEngine.java` running on UI/worker thread.
- **Capabilities**:
  - Intent detection (Tithe/Giving, Scripture reading, Sermons, Hymns, Prayer, Riddles, Jokes).
  - Bible navigation assistance (e.g. "Take me to 2 Samuel 5").
  - Deep-link action chips.
- **Honesty Assessment**: This is a rule-based deterministic expert system using regular expressions, keyword tokenization, and canned spiritual advice. It is **not** an on-device Large Language Model (e.g. Gemma, Llama, Gemini Nano) or neural network. It consumes zero extra battery/RAM and works completely offline, but cannot generate novel generative text.

### 2.5 Sermons & Livestreaming — `PARTIAL`
- **YouTube Integration**: Connects to `@perazimchurchembu` channel RSS feed (`https://www.youtube.com/feeds/videos.xml?channel_id=...`) to list recent video uploads.
- **Audio Streaming**: Media player component (`AudioPlayerController`) with play/pause, seek, 15s skip, and 1.0x/1.5x speed control.
- **Honesty Assessment**: Video deep-links to YouTube work when internet is active. The in-app audio player UI is fully rendered, but background audio streams are configured with fallback demo audio clips; the church's Google Drive archive of MP3 sermons is not currently indexed or streamed via an automated API.

### 2.6 Worship Hymnal & Chords — `HARDCODED/DEMO`
- **Content**: 10 traditional and contemporary church hymns with musical key notations and chord progressions.
- **Features**: Interactive chord viewer, font-resizing, search by title/number, and favorites bookmarking.
- **Honesty Assessment**: All 10 hymns are bundled directly inside the APK assets (`assets/database/perazim_seed.json`). Members cannot download new hymns from the cloud or upload choir arrangements.

### 2.7 Devotional Riddles & Christian Humor — `HARDCODED/DEMO`
- **Content**: 10 biblical trivia riddles and 10 clean Christian jokes.
- **Features**: Dedicated activity and embedded cards in Fellowship tab with interactive "Tap to Reveal Answer" buttons.
- **Honesty Assessment**: Static content compiled into the APK. No daily cloud refresh.

### 2.8 Gamification, Streaks & Spiritual Growth — `WORKING`
- **State Store**: `GamificationStore.java` persisting via SharedPreferences and Room.
- **Mechanics**:
  - Daily Streak counter (`🔥 X Days`) incremented when completing the 3-minute devotion.
  - Spiritual XP (`⭐ X XP`) awarded for completing scripture readings, prayer reflections, and hymn studies.
  - Grace Points (`💜 X`) awarded for 7-day milestones, redeemable for "Streak Freeze" (missed day forgiveness).
- **Honesty Assessment**: Fully functional on-device gamification modeled after Duolingo's retention loop. State correctly survives app restarts and displays consistently across Home HUD and Profile screens. Does not sync to a global church leaderboard.

### 2.9 Pastoral Care & Support Handoffs — `WORKING`
- **Contacts**: Presiding Bishop Dr. David Mutweri (`+254 710 772 227`).
- **Integrations**:
  - WhatsApp: `https://api.whatsapp.com/send?phone=254710772227`
  - Phone: `tel:+254710772227`
  - SMS: `smsto:+254710772227`
  - Directions: `geo:0,0?q=Perazim+Mission+Church+Embu`
- **Honesty Assessment**: All handoffs use standard Android system intents. Tested and verified on physical hardware (Infinix HOT 10T).

---

## 3. How to Run the Automated Visual Audit

To re-verify and capture physical evidence from a connected test phone:

```bash
# 1. Connect Android phone with USB Debugging enabled
adb devices

# 2. Run the review capture suite
./scripts/capture_review_screens.sh
```

The script will automatically navigate the app and write high-resolution screenshots and matching `uiautomator` hierarchy dumps into `docs/review/`:
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

---

## 4. Source Code Review & Safe Packaging Instructions

To create a clean, safe source code zip for external review or distribution **without leaking signing keystores, passwords, or build caches**:

### Excluded Files (Sensitive or Ephemeral):
- `*.jks`, `*.keystore` (Release & debug keystores)
- `keystore.properties` (Private signing passwords and key aliases)
- `build/` (Compiled bytecode, unstripped binaries, DEX files)
- `.gradle/` (Local Gradle caches and daemon logs)
- `.idea/`, `*.iml` (IDE configuration)
- `*.apk`, `*.aab` (Binaries)

### Recommended Packaging Command:

```bash
# Run from repository root (/home/murugu/Android/app):
zip -r perazim-source-v2.2.0.zip . \
  -x "build/*" \
  -x ".gradle/*" \
  -x ".idea/*" \
  -x "*.jks" \
  -x "*.keystore" \
  -x "keystore.properties" \
  -x "*.apk" \
  -x "*.aab" \
  -x ".git/*"
```
