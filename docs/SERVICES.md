# External Services & Cloud Architecture Registry

> **Policy**: Zero cost, zero billing accounts, offline-first fail-soft architecture.

---

## Stage 1 Services

### 1. Google Sheets — Published CSV Content Feed
- **Provider**: Google Workspace / Google Drive (Free Tier).
- **Purpose**: Dynamic church content management without custom servers or administrative databases. Allows church secretary to update weekly bulletins, upcoming events, new hymns, daily trivia riddles, Christian humor, sermon media links, and daily cornerstone scripture.
- **Cost / Billing**: \$0.00 / completely free. Requires no Google Cloud Project, no service account credentials, and no billing method.
- **Protocol**: HTTP GET via standard `HttpURLConnection` with UTF-8 encoding and RFC 4180 parsing.
- **Authentication**: None (Public read-only CSV published via Google Sheets "Publish to the Web").
- **Rate Limits & Caching**:
  - Google edge caches published CSVs (~5-minute TTL).
  - App client throttles automatic background sync to once every 6 hours on app startup.
  - On-demand manual sync button in Profile tab ("🔄 Refresh Content").
  - Connect & read timeouts strictly capped at 10,000ms (10 seconds).
- **Offline & Error Fallback**:
  - Pre-seeded local Room SQLite database (`perazim_database.db` version 5).
  - Atomic upsert by primary key (`id` or `date`).
  - Corrupted, missing, or empty downloads never overwrite or clear existing local data.
  - Fail-soft indicator displayed on UI ("Last updated: <time>" or "Not synced yet (using local seed data)").
- **Verification Proof**:
  - Unit tests: `org.perazimchurch.app.sync.SheetSyncValidationTest` (7/7 passing).
  - Hardware device verification: Live Infinix HOT 10T tap update (`docs/review/dc15a79-05-profile.txt`, lines 40–43).

---

## Upcoming Stage Services (Pending Levi Approval)

### Stage 2: Generative AI Assistant Relay
- **Provider**: Cloudflare Workers (Free tier: 100,000 requests/day) + Groq / Google Gemini Free Tier.
- **Status**: Reserved for Stage 2.

### Stage 3: Community Fellowship Backend
- **Provider**: Supabase Free Tier (PostgreSQL + PostgREST + Row Level Security).
- **Status**: Reserved for Stage 3.

### Stage 4: Church Admin Portal
- **Provider**: Cloudflare Pages / GitHub Pages (Static Web Hosting, Free).
- **Status**: Reserved for Stage 4.
