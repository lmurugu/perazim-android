# Perazim AI Breakthrough Guide — Cloudflare Worker Relay

Reverse-proxy gateway connecting the Perazim Android app to Google Gemini API (`gemini-3.5-flash-lite`).

---

## Architecture & Security Boundary

1. **Zero Key Exposure**:
   * The Gemini API key is stored exclusively as an encrypted secret in Cloudflare (`wrangler secret put GEMINI_API_KEY`).
   * The Android app never possesses or transmits the Gemini API key.
2. **App Token Speed Bump**:
   * The app transmits an `X-App-Token` header.
   * *Notice*: As documented in the architecture spec, this shared token is only an operational speed bump to filter unauthenticated internet scrapers; it is not public-key client attestation.
3. **Zero PII Logging**:
   * The Worker strictly refrains from logging user question strings.
4. **Rate Limiting & Daily Quotas**:
   * Enforces 3 questions per day per device ID via Cloudflare Workers KV (`PERAZIM_KV`).
   * Resets at 00:00 Africa/Nairobi time.
5. **Pastoral Crisis Intervention**:
   * Intercepts questions mentioning self-harm or danger and immediately delivers pastoral support referral to Bishop Dr. David Mutweri (`+254 710 772 227`) without consuming AI quota.

---

## Free Tier Quotas & Limits

* **Cloudflare Workers Free Plan**:
  * 100,000 requests/day
  * 10ms CPU time / request
  * Free KV namespace: 100,000 reads/day, 1,000 writes/day
* **Google Gemini API Free Tier**:
  * Model: `gemini-3.5-flash-lite`
  * Quota: 15 Requests Per Minute (RPM), 1,500 Requests Per Day (RPD)
  * Cost: $0.00 / completely free, no credit card required.

---

## Deployment & Management

```bash
# Navigate to relay directory
cd relay

# Deploy to Cloudflare
npx wrangler deploy

# Set secrets
npx wrangler secret put GEMINI_API_KEY
npx wrangler secret put APP_TOKEN
```
