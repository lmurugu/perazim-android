# External Services & Cloud Architecture Registry

> **Policy**: Zero cost, zero billing accounts, offline-first fail-soft architecture.

---

## Stage 1 Services

### 1. Google Sheets — Published CSV Content Feed
- **Provider**: Google Workspace / Google Drive (Free Tier).
- **Purpose**: Dynamic church content management without custom servers or administrative databases. Allows church secretariat to update weekly bulletins, upcoming events, new hymns, daily trivia riddles, Christian humor, sermon media links, and daily cornerstone scripture.
- **Cost / Billing**: $0.00 / completely free. Requires no Google Cloud Project, no service account credentials, and no billing method.
- **Protocol**: HTTP GET via standard `HttpURLConnection` with UTF-8 encoding and RFC 4180 parsing.
- **Authentication**: None (Public read-only CSV published via Google Sheets "Publish to the Web").
- **Rate Limits & Caching**:
  - Edge Cache TTL: Published CSVs are edge-cached by Google and automatically refreshed ~5 minutes after spreadsheet edits (*Source*: Google Docs Editors Help, "Publish files from Google Docs, Sheets, Slides, & Forms", verified Nov 2024).
  - Client Sync Interval: App throttles automatic background sync to once every 6 hours on app startup.
  - Manual Sync: On-demand sync via Profile tab ("🔄 Refresh Content").
  - Timeouts: Connect & read timeouts strictly capped at 10,000ms (10 seconds).
- **Offline & Error Fallback**:
  - Pre-seeded local Room SQLite database (`perazim_database.db` version 5).
  - Atomic upsert by primary key (`id` or `date`).
  - Preserves local-only user state: `HymnEntity.isFavorite`, `SermonEntity.isDownloaded`, and `SermonEntity.localAudioPath`.
  - Corrupted, missing, or empty downloads never overwrite or clear existing local data.
  - Fail-soft indicator displayed on UI ("Last updated: <time>" or "Not synced yet (using local seed data)").
- **Verification Proof**:
  - Unit tests: `org.perazimchurch.app.sync.SheetSyncValidationTest` (9/9 passing).
  - End-to-end sync: Awaiting entry of the 8 published URLs from Levi.

---

## Upcoming Stage Services (Pending Levi Approval)

### Stage 2: Generative AI Assistant Relay
- **Provider**: Cloudflare Workers (Free tier) + Google Gemini API Free Tier (via Google AI Studio). No Groq.
- **Purpose**: Serverless reverse-proxy gateway protecting API keys and enforcing church safety guardrails and daily quotas per device.
- **Documented Free Limits & Sources**:
  - Cloudflare Workers Free Tier: 100,000 requests/day, 10ms CPU time per invocation (*Source*: Cloudflare Workers Documentation, "Limits - Free Plan", verified Oct 2024).
  - Google Gemini API Free Tier (Gemini 1.5/2.0 Flash): 15 RPM (requests per minute), 1,500 RPD (requests per day), 1,000,000 TPM (tokens per minute) (*Source*: Google AI Studio Pricing Documentation, `ai.google.dev/pricing`, verified Nov 2024).
  - Device Daily Quota: 3 questions/day/member enforced at proxy layer.
- **Status**: Reserved for Stage 2 (pending approval).

### Stage 3: Community Fellowship Backend
- **Provider**: Firebase Spark Plan (Google Cloud). No Supabase.
- **Purpose**: Community prayer requests, member amen counters, and shared praise reports.
- **Documented Free Limits & Sources**:
  - Cloud Firestore Spark Plan: 1 GiB stored data, 50,000 document reads/day, 20,000 document writes/day, 20,000 document deletes/day, 100 maximum simultaneous connections (*Source*: Firebase Pricing Documentation, `firebase.google.com/pricing`, verified Nov 2024).
  - Firebase Authentication Spark Plan: Unlimited email/password & anonymous auth (*Source*: Firebase Auth Documentation, verified Nov 2024); phone SMS authentication is unverified/excluded to avoid SMS verification billing risks.
- **Status**: Reserved for Stage 3 (pending approval).

### Stage 4: Church Admin Portal
- **Scope**: Design Document only.
- **Purpose**: Architectural specification and user flows for a future administrative web portal (curating hymns, bulletins, sermon media links, and member moderation) without implementing or deploying services during this pilot.
- **Status**: Reserved for Stage 4 (pending approval).
