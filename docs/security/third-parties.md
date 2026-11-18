# Third-party inventory

Status column: **Live** (used in release), **Planned** (in the plan, not yet used), **Disabled** (present in code but off in release builds). Verified against `build.gradle` build-type flags (`ONLINE_AI_ENABLED`, `REMOTE_SHEET_FEEDS_ENABLED`, `OFFLINE_QWEN_ENABLED` are `false` in release) and the absence of analytics or crash SDKs in `build.gradle`.

| Service | Data sent | Region | Status | Terms or DPA link | Notes |
|---|---|---|---|---|---|
| Cloudflare Workers and KV (AI relay) | Relay requests for the online AI, quota counters | Global edge | Disabled in release (`ONLINE_AI_ENABLED=false`) | [PENDING OWNER INPUT: DPA link] | Relay holds the provider key. The client never holds a relay token. |
| Google Gemini (via relay) | Question text and bounded context | Google regions | Disabled in release | [PENDING OWNER INPUT: Gemini terms and retention setting] | Gate L7 applies |
| Hosting VPS (not chosen) | All server data | PENDING | Planned | PENDING | Plan 5.1; choose a Kenya-region or EU-adequate provider |
| Off-site backup: separate free Google account via rclone (`offsite:` remote) | Encrypted database backups only (`*.dump.enc`); the key is never uploaded here | Google regions | Planned: needs the owner to create the account and run `rclone config` | Google Drive terms | Must not be the account holding the backup key copy. rclone's shared Google Drive client ID is being retired in 2026 ([rclone docs](https://rclone.org/drive/)); create an own client ID before then. Fallback: Backblaze B2 free tier (10 GB). Not Cloudflare R2 (card required). Budget $0 |
| GitHub | Source code, CI logs, release artefacts | US | Live (source) | GitHub terms | No personal data should be committed (see `CONTRIBUTING.md`) |
| Google Play | App distribution, install and crash data (Play-provided) | Global | Planned | Play Developer Distribution Agreement | Data Safety form must match the Privacy Policy |
| Safaricom M-Pesa | Donation performed by the user in the M-Pesa app or USSD | Kenya | Live (handoff only; the app sends nothing) | Safaricom terms | The app opens the M-Pesa app or dialer only |
| WhatsApp and SMS (user's own app) | Message the user writes, sent by the user | Global | Live (handoff only) | WhatsApp terms | The app opens a prefilled message; nothing is sent automatically |
| Google Maps (intent) | Coordinates of the church, requested by the user | Global | Live (handoff only) | Google terms | Opens the user's map app |
| Google Sheets and YouTube feeds | Public content fetches | Global | Disabled in release (`REMOTE_SHEET_FEEDS_ENABLED=false`) | Google terms | |
| Firebase Crashlytics or Sentry | Crash reports | PENDING | Not present | PENDING | Plan 2.8: opt-in only, disclosed in the Privacy Policy |

Any new third party must be added here, to the Privacy Policy and to the Play Data Safety form in the same change (plan C.4 and C.8).
