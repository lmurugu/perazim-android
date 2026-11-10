# Perazim $0-First Media Hub + Reliability Implementation Blueprint
**Date:** 2026-11-10  
**Base:** `phase-B-community`  
**Status:** DESIGN / IMPLEMENTATION PLAN ONLY. No runtime feature, build, external integration, or device test is claimed complete.  
**Pilot sizing:** 100–200 registered; 50–100 DAU; 20–30 heavy daily users. Closed, known-member pilot; no public registration.

## 0. Executive decision
Build a **provider-backed media terminal**, not a church-operated streaming CDN. Keep Android Java + Room offline-first, existing Express/JWT/PostgreSQL community API, existing Google Sheets content pipeline where enabled and verified, and church-owned media hosted on YouTube and an authorized podcast/audio provider. Treat Facebook and Spotify as optional embedded experiences with a reliable external fallback. Store **IDs, metadata, progress and consented user preferences**, not copies of third-party media streams.

**$0 means no mandatory recurring infrastructure subscription under actual quota and account conditions; it does NOT guarantee 24/7 availability, backups, free domains, unrestricted provider APIs, or zero member mobile-data costs.** Do not launch to 200 members before proving hosting uptime and restore.

## 1. Repository reality (inspection, not device proof)
- `src/main/java/org/perazimchurch/app/presentation/ui/SermonsUiBinder.java`: existing sermon UI, `MediaPlayer`, audio controls, video/live affordances, `YouTubeFeedRepository` reference. Reuse, do not duplicate playback systems.
- `src/main/java/org/perazimchurch/app/SermonsActivity.java`: older separate activity with external YouTube intents. Audit navigation; consolidate rather than maintain two competing sermon experiences.
- `src/main/java/org/perazimchurch/app/data/local/entity/SermonEntity.java`, DAO, repository, model, ViewModel: existing Room catalogue.
- `src/main/java/org/perazimchurch/app/sync/SheetSyncManager.java`: CSV-based sync, 6-hour automatic cadence, fail-soft caching; existing `docs/sheet-template/sermons.csv`.
- `build.gradle`: `REMOTE_SHEET_FEEDS_ENABLED=false` and empty default API URL; validation/release API base points to a Fedora Tailscale host. Do NOT assume these are reachable from members' phones or turn remote feeds on blindly.
- `server/package.json`: Express/Postgres/JWT existing. No Socket.IO dependency; no need to add it for media discovery.
- `docs/LOCAL_BIBLE_AUDIO.md`: member-selected audio on phone is already supported; never delete a personal source file.
- `STATUS.md` contains historical claims and evidence across branches/dates; confirm current code + actual device behavior before calling any feature working.
- Template CSV contains example-looking `video.perazim.org` and `audio.perazim.org` URLs; verify reachability and ownership before using. Do not treat examples as functioning production hosts.
- **Security:** repository is public; never commit member phone lists, credentials, secret tokens, API keys, signing keystores, or unpublished private content.

## 2. Feasible features and honest constraints
| Feature | $0-first path | Gate / limitation |
|---|---|---|
| YouTube sermons | Official YouTube iframe embedded player; catalogue stores video ID | Embedding enabled, video available, compliant WebView player identity/referrer, fallback external |
| YouTube live | Published scheduled/live video ID; live badge from trusted status | No fabricated LIVE status; prefer manually managed schedule before quota-sensitive API automation |
| YouTube playlists | Playlist URL/ID and curated episode IDs | Do not scrape or download streams; API key optional and server-side if API used |
| Sermon search/filter | Room indexed metadata: speaker, series, date, scripture | Works offline against last synced catalogue |
| Continue listening | Local bookmark position for authorized direct audio | Embedded YouTube/Spotify may not expose reliable resume state |
| Audio sermons | Church-owned podcast RSS + permitted direct audio in Media3 | Verify host's hotlink/bandwidth policies; Spotify embed only as secondary |
| Spotify podcasts | Official embed when supported | Account, playback, and WebView restrictions; external fallback mandatory |
| Facebook Live | Official public embed if supported | Login/embed/visibility restrictions; external fallback mandatory |
| Audio background playback | Media3 + foreground MediaSessionService for authorized direct audio | Notification, audio focus, interruption, Android 11 device test |
| Downloads | Only church-owned/licensed direct audio with explicit permission | No YouTube/Spotify/Facebook stream extraction or offline capture |
| Live schedule | Existing events/announcements or curated Sheet feed | Cached offline; 'scheduled' vs 'live' never confused |
| Favorites/history | Room locally; optional opt-in account sync | Respect privacy; no unnecessary tracking of religious listening behavior |
| Share sermon | Share provider canonical URL | Avoid embedding credentials and internal private links |
| Accessibility | Captions supplied by host, readable controls, TalkBack labels | Embedded providers control caption availability |
| Bandwidth saver | Default no autoplay, thumbnails, user-initiated stream, Wi-Fi-only direct-audio download | Providers choose actual adaptive video bitrate |
| Resilience | Cached catalogue, timeout, retry, provider fallback, stop-on-navigation, health telemetry without PII | No false offline streaming claim |
| Community media | Separate private bucket, 30-day expiry, app cache cleanup | Not a substitute for podcast/video hosting; never delete Gallery originals |
| Notifications | In-app schedule reminders initially | Push requires separately configured and tested service |
| Admin publishing | Google Sheets approved catalogue with validated schema | Public Sheet contains PUBLIC metadata only, never member records |
| Provider outage fallback | Alternative church-approved recording/provider link | No guarantee a mirror exists; show honest unavailability |
| Cloud data backup | Encrypted PostgreSQL export + independent storage + restore test | Supabase DB is not itself a backup; free-plan backups/pausing vary |

## 3. Target architecture
```
[Church admin publishes on YouTube / podcast host / Facebook]
                      |
     [Curated public catalogue: Google Sheets CSV or API]
                      |
 [Android fetch -> validate -> Room transaction -> cached index]
                      |
   [ProviderAdapter -> official embed / direct Media3 / fallback]
                      |
  [User phone downloads stream directly from original provider]

[Android Room + existing sync queue] <-> [existing Express API]
                                           |
                                      [PostgreSQL]
                                           |
                                [independent encrypted backup]
```
**Never proxy or re-host YouTube/Facebook/Spotify video through Express or Supabase Storage.** A persistent cloud API is still necessary for online multi-user community functions; putting Postgres on Supabase does not keep Fedora Express running when the PC is off.

## 4. Minimal catalogue contract and validation
Keep existing sermon fields intact. Introduce additive, backward-compatible fields only after inspecting Room migrations:
```text
id,title,speaker,date,series,scripture,provider,provider_id,canonical_url,
audio_url,thumbnail_url,kind,live_state,scheduled_start_utc,language,
duration_seconds,active,featured,allow_download,last_verified_at
```
- `provider`: `YOUTUBE`, `DIRECT_AUDIO`, `SPOTIFY_EMBED`, `FACEBOOK_EMBED`.
- `kind`: `VIDEO`, `AUDIO`, `LIVE`; `live_state`: `UNKNOWN`, `SCHEDULED`, `LIVE`, `ENDED`. Never infer LIVE solely from the calendar.
- `provider_id`: provider-native identifier, **not arbitrary HTML**. `canonical_url`: HTTPS, allowlisted provider host. `audio_url`: HTTPS from approved church-controlled/licensed audio host only.
- Reject javascript/file/content/intent schemes, untrusted iframe HTML, redirects to unknown hosts, malformed IDs, duplicate IDs, invalid dates, impossible sizes, and oversized CSV rows.
- Preserve previously cached catalogue when fetch fails or returns malformed/empty results; explicit administrative deletion needs tombstones/versioned deactivation, not silent blanket overwrite.
- Prefer typed adapter classes `MediaSourceResolver`, `YoutubeEmbedAdapter`, `PodcastAudioAdapter`, `ExternalFallbackLauncher`, `PlaybackCoordinator` and `MediaAvailability`; no WebView per list row.
- Use allowlisted provider URLs; never inject untrusted remote JavaScript into WebView. JavaScript enabled only for trusted official embeds; disable file access, mixed content, arbitrary navigation and unnecessary JS bridges.

## 5. Detailed execution order (each step requires one killer test and evidence)
### Step 0 — Freeze baseline and prepare proof
1. Record current `phase-B-community` commit SHA and working tree. Create feature branch `feature/media-hub-zero-cost` for code (this document is on a separate docs branch).
2. Inspect `SermonsUiBinder`, `SermonsActivity`, `YouTubeFeedRepository`, sermon Room schema, `SheetSyncManager`, existing playback cleanup and Gradle variants.
3. Record existing sermon playback bug reproduction, current app version, and build variant on Infinix Android 11.
**Killer test:** current APK opens sermons and leaves playback screen; record actual behavior. **Evidence:** APK hash, commit SHA, device screenshot + logcat, pass/fail; do not claim repaired yet.

### Step 1 — Confirm church-controlled sources
1. Obtain official church YouTube channel, video/playlist URLs, Facebook Page/public Live URL, and podcast/RSS host.
2. Verify ownership/permission, embed-enabled public videos, HTTPS, no age/region restriction, captions where available, and public vs members-only content.
3. Select one working video, one actual authorized audio file/feed, one scheduled stream; Facebook/Spotify optional.
**Killer test:** open each exact source on Infinix mobile data in a browser. **Gate:** no unknown/demo URLs in release catalogue.

### Step 2 — Add validated metadata without breaking Room
1. Decide whether to extend existing SermonEntity or create separate `MediaEntryEntity` with stable ID and provider fields.
2. Write explicit Room migration preserving bookmarks, prayers, user identity, and favorites. Never destructive migration.
3. Add parser tests for valid/invalid CSV, duplicate rows, malicious URLs, empty/partial response, and offline fallback.
4. Keep published Google Sheet PUBLIC metadata only; never member identity or secret API keys.
**Killer test:** install over previous APK, verify existing data and offline sermon catalogue. **Evidence:** migration test, screenshot, log.

### Step 3 — Build unified media screen
1. Reuse SermonsUiBinder/ViewModel, not a second independent navigation surface. Tabs: Sermons, Live, Audio, Saved.
2. Search by title, speaker, series, scripture; show cached thumbnails, provider badges, dates and source attribution.
3. One prominent play action; explicit data-use hint; loading, no-connection, unavailable, and retry states.
4. English-only UI, Perazim #681A7D purple, #E17D2F orange accents, #2A0F33 deep background; preserve existing navigation.
**Killer test:** airplane mode -> browse saved catalogue -> tap online-only item -> honest offline message without crash.

### Step 4 — YouTube embed adapter
1. Use official YouTube iframe embed player; test Android WebView support and current provider requirements for identity/referrer/origin. Do not fake HTTP headers or circumvent player restrictions.
2. Use a single trusted playback container, fullscreen callback, lifecycle pause/destroy, hardware back handling and rotation.
3. No autoplay by default; display YouTube attribution and official controls. Provide 'Open in YouTube/browser' fallback.
4. Test video disabled embedding, deleted/private video, slow network, switching tabs, phone call interruption.
**Killer test:** play a church video, leave Sermons, verify sound stops; return and play another; no overlapping audio. **Evidence:** screen recording/logcat.

### Step 5 — Native authorized audio
1. Verify direct audio ownership and host hotlink/bandwidth policy. Use Media3 ExoPlayer + MediaSession for streaming church-owned MP3/HLS only; pin a compatible stable version and license.
2. Build a single playback coordinator; pause/resume/seek/speed/next; audio focus, noisy headset, calls, notification control, background service only when user initiated playback.
3. Retain `docs/LOCAL_BIBLE_AUDIO.md` behavior and never delete the original selected file.
4. Optional offline download only for authorized direct audio, Wi-Fi-only toggle, app-owned storage and explicit removal.
**Killer test:** start audio -> lock phone -> control from notification -> unplug headphones -> playback pauses appropriately -> stop and confirm service ends.

### Step 6 — Live broadcasts
1. Store a curated schedule in events/Sheet and one active provider video URL/ID; don't claim live based on schedule alone.
2. Add YouTube live embed; provide countdown only for confirmed scheduled time, last checked timestamp, 'Watch externally' fallback.
3. Support Facebook public embed behind provider compatibility check; when login/embedding fails, open external browser/app with an explanatory message.
4. Never scrape protected Facebook video or circumvent login.
**Killer test:** no live stream -> show 'No live broadcast now'; live available -> opens verified player; unavailable -> fallback works.

### Step 7 — Spotify/podcast optional
1. Publish church audio through a provider permitting RSS and embedded/direct playback; verify free quota, ownership and redistribution policy.
2. Parse RSS carefully (safe XML parser, size limits, HTTPS, no external entities), map episodes to stable IDs and cache metadata.
3. Offer official Spotify embed only where supported; do not promise full audio, login-free playback, background controls or offline downloads.
**Killer test:** podcast direct audio plays in Media3; Spotify unavailable -> external fallback, no crash.

### Step 8 — Reliability and privacy layer
1. Offline cached index, ETag/If-Modified-Since where supported, 10s timeout, exponential backoff + jitter, network constraints, bounded retries, dedupe by stable ID.
2. Health states per provider: `AVAILABLE`, `UNAVAILABLE`, `UNKNOWN`; cache timestamps, never silently substitute another sermon.
3. Central playback lifecycle manager prevents multiple simultaneous audio sources. Release WebView and MediaPlayer resources on lifecycle transitions.
4. Never log tokens, personal phone numbers, full private URLs, member watch histories, or prayer content. Collect opt-in coarse counters only if necessary.
5. Privacy: do not treat listening to religious content as ordinary analytics; no third-party tracking SDK just for engagement.
6. Security review: WebView URL allowlist, Android intent safety, server auth, group media authorization, backup encryption and restoration.
**Killer test:** force network outage midstream, rotate/back/return, restore internet; no crash, duplicate player, lost Room content or leaked session.

### Step 9 — Media retention
1. Community-uploaded media only: private storage, `expires_at = accepted_at + 30 days`, server scheduled deletion, idempotent cleanup and expired placeholder.
2. Android WorkManager purges **app-owned cache only**, plus expiry check on access; don't remove user's Gallery or file-picker originals.
3. 'Save to device' explicit action writes user-chosen location; saved copy is outside automatic Perazim deletion.
4. Backups have separate retention; deletion from live bucket does not guarantee deletion from older backup copies or recipient downloads.
**Killer test:** test clock/fixture expired object -> private cloud object removed + local cache inaccessible + Gallery original untouched.

### Step 10 — Backup + operations
1. Decide on reliable 24/7 API hosting; Fedora-only or Quick Tunnel cannot guarantee 24/7. Existing Tailscale URL may not be reachable by ordinary pilot members.
2. Keep one authoritative PostgreSQL and one existing auth system. Avoid dual Supabase Auth + Express JWT unless separately migrated/tested.
3. Automated encrypted database dumps to **independent** storage, verify checksums, 7–14 day retention policy, restore into disposable database monthly. Never commit dumps.
4. Add monitoring: API health, recent backup age, restore test date, media source failures, queue backlog; alert owner without disclosing member data.
5. Test provider limits with 100 concurrent catalogue reads and 30 concurrent authorized media launches; streams should be direct provider-to-phone.
6. Free tier guardrails: storage/egress budget alerts, max upload size, no video community uploads initially, no assumed free automatic DB backups.
**Killer test:** shut Fedora PC off -> cloud API still accessible if claiming 24/7; restore backup in isolated environment; otherwise explicitly label 'scheduled pilot only'.

### Step 11 — Closed pilot release
1. Run unit/instrumentation tests, backend tests, privacy gates, Android build and release APK inspection.
2. Install APK on Infinix HOT 10 Android 11, test Wi-Fi and Safaricom/Airtel mobile data (where available), offline and reconnect, Android back and audio focus.
3. Record APK SHA-256, Git commit, timestamp, device version, screenshots/video/logcat, test result and known limitations in STATUS.md.
4. Invite 10 known testers, then 25, 50, and only then 100–200 based on proof and support capacity. No public signups; do not reintroduce Campus or deferred moderation features.
**Killer test:** one tester can discover and play verified church media while another uses community; Fedora-off check and privacy gate pass.

## 6. Reliability acceptance matrix
| Gate | Required proof |
|---|---|
| Offline-first | App opens and cached catalogue works in airplane mode; playback clearly says internet needed |
| Provider resilience | Broken YouTube/Facebook/Spotify link offers a safe fallback; no blank WebView |
| Audio lifecycle | Stop/pause/seek, tab switching, screen lock, incoming call and headphone disconnect verified |
| Data migration | In-place upgrade preserves Room user data; schema migration automated test passes |
| Security | No private register/secret in APK or repo; allowlist and authenticated media access tested |
| Hosting | 24h monitoring or documented scheduled-pilot limitation; API independently reachable if PC off |
| Recovery | Encrypted backup created and **successfully restored**, with date and proof |
| Cost | Zero mandatory paid services; free quotas checked monthly; media bandwidth delegated to provider |
| User experience | English-only; no fake LIVE badges; Perazim colors; provider attribution |
| Device proof | Commit + APK hash + Infinix screenshot/recording + logcat + PASS/FAIL recorded |

## 7. Cost and provider-risk decision table
- **YouTube:** no charge to embed ordinary public, embeddable videos, subject to policies and ads/provider restrictions. YouTube Data API quota is separate; curated IDs avoid API dependence. No guaranteed uninterrupted ad-free experience.
- **Podcast RSS/direct audio:** may be free at low traffic, but free hosting/bandwidth terms vary. Do not rely on a made-up free host. Confirm a specific provider before committing.
- **Spotify:** podcast embed can be useful, but playback restrictions are platform/browser dependent. Do not rely on it as sole audio delivery.
- **Facebook:** optional public live embed with mandatory external fallback; not a reliable primary player.
- **Google Sheets:** suitable for PUBLIC approved metadata, existing code supports it, but current Gradle disables remote feed; enabling requires validated sources and build-specific checks.
- **Supabase Free:** optional Postgres/private community media if project active and quotas fit; do not assume auto-backups or 24/7 SLA.
- **Cloudflare:** Workers/Pages may help with public catalogue caching under current quotas; not a drop-in always-on Express/Postgres replacement.
- **GitHub Actions:** useful for CI within applicable plan quotas; never store database dumps or member data in public workflow artifacts.
- **Domain:** $0 only if an existing suitable domain/subdomain is available or provider hostname accepted.
- **Member data:** streaming uses members' internet bundles, not zero-cost to members.

## 8. Recommended initial release cut
**Must-have:** verified YouTube video embed + external fallback; cached searchable sermons; manually curated Live schedule; authorized direct podcast audio in native player; stop-on-navigation; offline messaging; privacy and device proof.

**Nice-to-have after proof:** Spotify embed, Facebook embed, background audio, favorites, playlists, opt-in progress sync, RSS automation, service reminders, downloadable church-owned audio.

**Explicitly deferred:** video rehosting, YouTube/Spotify downloaders, proprietary stream extraction, unbounded community video, paid transcoding/CDN, public signup, intrusive watch analytics, public bucket for private attachments, a second auth stack.

## 9. Build agent work order / handoff
Implement one step at a time on a new code branch; keep the app functional and offline-first. Each step must produce: changed files, tests, build result, physical Infinix device proof or 'NOT VERIFIED', APK hash, risks, and `STATUS.md` update. No completion claim without proof. Stop at any failed acceptance gate. Preserve Paybill 4069983, existing church contacts, English-only UI, brand palette, closed pilot, privacy exclusions, and no Campus feature expansion.

## 10. Source-of-truth documentation to recheck before implementation
- YouTube iframe player: https://developers.google.com/youtube/iframe_api_reference
- YouTube embedded player requirements: https://developers.google.com/youtube/terms/required-minimum-functionality
- YouTube API quota: https://developers.google.com/youtube/v3/determine_quota_cost
- Spotify embeds: https://developer.spotify.com/documentation/embeds
- Meta embedded video player: https://developers.facebook.com/docs/plugins/embedded-video-player/
- Android Media3: https://developer.android.com/media/media3
- Android WebView security: https://developer.android.com/privacy-and-security/risks/webview-unsafe-file-inclusion
- Android WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
- Supabase storage quotas: https://supabase.com/docs/guides/platform/manage-your-usage/storage-size
- Supabase private buckets: https://supabase.com/docs/guides/storage/buckets/fundamentals

**Next implementation decision:** confirm actual church media URLs and whether unattended 24/7 community API operation is required while Fedora is off. Those facts determine viable $0 hosting and provider adapters.
