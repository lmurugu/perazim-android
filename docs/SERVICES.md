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
  - Edge Cache TTL: Published CSVs are edge-cached by Google and automatically refreshed ~5 minutes after spreadsheet edits (*Source*: Google Docs Editors Help, "Publish files from Google Docs, Sheets, Slides, & Forms", `https://support.google.com/docs/answer/183965`, verified November 3, 2026).
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
  - Unit tests: `org.perazimchurch.app.sync.SheetSyncValidationTest` (15/15 passing), `org.perazimchurch.app.sync.CsvParserTest` (6/6 passing).
  - End-to-end sync: Awaiting entry of the 8 published URLs from Levi.

---

## Upcoming Stage Services (Pending Levi Approval)

### Stage 2: Generative AI Assistant Relay
- **Provider**: Cloudflare Workers (Free Tier) + Google Gemini API Free Tier (via Google AI Studio).
- **Status**: **ACTIVE** (Deployed live on Cloudflare Worker `perazim-ai-relay.perazim-mission.workers.dev`).
- **Purpose**: Zero-cost serverless reverse-proxy gateway protecting the Google Gemini API key, enforcing church safety guardrails, crisis routing, and daily quotas per device.
- **Documented Free Limits & Sources**:
  - Cloudflare Workers Free Tier:
    - 100,000 requests/day (resets daily at 00:00 UTC)
    - 10ms CPU execution time per invocation
    - 128 MB memory allocation per isolate
    - Cloudflare KV: 100,000 reads/day, 1,000 writes/day
    - *Source*: Cloudflare Workers Documentation, "Limits - Free Plan" (`https://developers.cloudflare.com/workers/platform/limits/`), verified November 3, 2026.
  - Google Gemini API Free Tier (Google AI Studio):
    - Active Model: `gemini-3.5-flash-lite` (verified operational; legacy `gemini-2.0-flash` is retired)
    - Rate Limits: 15 RPM (requests per minute), 1,500 RPD (requests per day), 1,000,000 TPM (tokens per minute)
    - *Source*: Google AI Studio Documentation (`https://ai.google.dev/pricing`), verified November 3, 2026.
  - Device Daily Quota: 3 questions/day/member enforced at proxy layer (Cloudflare KV) and mirrored client-side (`AiPreferences`). Reset at midnight Africa/Nairobi timezone (UTC+3).
- **Safety & Privacy Features**:
  - 500-character query limit.
  - Immediate crisis phrase regex detection routing directly to Pastoral Care hotline (`+254 710 772 227`).
  - Strict church grounding system prompt: 2 Samuel 5:20, Micah 2:13, under 120 words, strictly Bible-grounded, no medical/financial advice, polite deflection of off-topic secular questions.
  - Zero logging of member query text.
  - Seamless offline fail-soft to `PerazimAiEngine` when quota exhausted or offline.

### Stage 3: Community Fellowship Backend
- **Provider**: Firebase Spark Plan (Google Cloud). No Supabase.
- **Purpose**: Community prayer requests, member amen counters, and shared praise reports.
- **Documented Free Limits & Sources**:
  - Cloud Firestore Spark Plan:
    - 1 GiB total stored data
    - 50,000 document reads/day
    - 20,000 document writes/day
    - 20,000 document deletes/day
    - 10 GiB/month network egress
    - 100 maximum simultaneous connections
    - *Source*: Firebase Pricing Documentation (`https://firebase.google.com/pricing`), verified November 3, 2026.
  - Firebase Authentication Spark Plan:
    - Standard Sign-in Methods (Email/Password, Anonymous, Federated Social): Free up to 50,000 Monthly Active Users (MAUs) (Identity Platform cap: 3,000 DAUs on Spark plan)
    - SAML / OIDC: Free up to 50 MAUs (2 DAUs daily cap)
    - Phone Auth (SMS): Requires Blaze plan upgrade (billed per SMS sent); unverified/excluded to ensure zero billing risk on pilot
    - *Source*: Firebase Pricing Documentation (`https://firebase.google.com/pricing`), verified November 3, 2026.
- **Status**: Reserved for Stage 3 (pending approval).

### Stage 4: Church Admin Portal
- **Scope**: Design Document only.
- **Purpose**: Architectural specification and user flows for a future administrative web portal (curating hymns, bulletins, sermon media links, and member moderation) without implementing or deploying services during this pilot.
- **Status**: Reserved for Stage 4 (pending approval).
