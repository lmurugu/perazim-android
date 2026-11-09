# Online AI Worker integration handoff

## Current implementation

The repository contains `relay/worker.js` and `relay/wrangler.toml`. The Worker source previously accepted the reusable `X-App-Token`, client `deviceId`, name, history and context; its KV quota was fail-soft and non-atomic. The source has been changed locally to require a short-lived HS256 grant from Express, bind its request ID to the grant, reject extra client fields, use a pseudonymous subject for its defensive KV counter, and avoid sending names, history, streaks or client context to Gemini. Provider failures return generic responses and do not log question or provider response text.

This source change is not deployed or operationally verified. The Worker stays disabled unless its `AI_ENABLED` setting is exactly `true`. The Android release flag remains false. The existing Worker quota is only a defensive counter: KV read/increment/write is not atomic and cannot be the authoritative quota control. A signed grant is bound to the request ID and expires quickly, but one-time replay protection is not implemented; an attacker replaying the same grant during its validity period is not prevented by the Worker alone.

## Express contract implemented locally

- Android submits an authenticated Perazim bearer session to `POST /api/ai/online` with only `{ "question": "..." }`.
- Express derives the user from the validated session, then checks current `membership_verifications.status`, the configured active permission grant, and expiry/revocation fields. The request body cannot supply identity, role, status, permissions, history, device ID or context.
- Only `VERIFIED_MEMBER` plus an effective explicit permission is eligible. Registration or administrator status alone does not grant access.
- Express makes a 60-second HS256 grant with issuer `perazim-express`, audience `perazim-ai-worker`, a unique request ID and an HMAC-pseudonymized subject. It sends only `{requestId, question}` to the Worker.
- The endpoint is fail-closed by default. It requires independent server-side enablement, privacy review, authoritative quota enforcement and a verified `signed-v1` Worker contract, as well as an entitlement code and Worker configuration. None is enabled by this change.
- Common direct identifiers and credential patterns are rejected before dispatch. This deterministic filter is incomplete; it is not a reliable detector of sensitive pastoral information. The privacy gate must remain off until a reviewed safeguard can meet the pilot restriction.
- No approved Bible/document retrieval is attached to this Worker request yet. The existing model identifier and provider processing terms are unverified, so it cannot be treated as a grounded or operational church-assistant response path.

## Permission contract still missing

The database permission catalogue does not contain an online AI permission, and no user has such a grant. The specifications approve online AI for verified members subject to a distinct permission but leave the final catalogue identifier unresolved. Proposed minimal identifier for owner review: `ai.online.use` (active catalogue permission; assign only by an authorized operator to eligible verified-member accounts; revocable and expiring using the existing `user_permissions` fields). Do not insert or grant it automatically. The runtime reads `PERAZIM_AI_ONLINE_PERMISSION_CODE`; it does not assume or seed a value.

## Required service configuration

Configure these only in the approved secret/configuration systems after the contract is verified; never paste values into source or chat:

- Worker secret: `PERAZIM_AI_WORKER_SHARED_SECRET` (at least 32 random bytes, shared only by Express and the Worker).
- Worker secret: `GEMINI_API_KEY` (provider credential; never sent to Express clients).
- Worker setting: `AI_ENABLED` (must remain disabled until activation acceptance).
- Express settings: `PERAZIM_AI_ONLINE_ENABLED`, `PERAZIM_AI_ONLINE_PRIVACY_REVIEWED`, `PERAZIM_AI_ONLINE_QUOTA_ENFORCED`, `PERAZIM_AI_WORKER_CONTRACT`, `PERAZIM_AI_ONLINE_PERMISSION_CODE`, `PERAZIM_AI_WORKER_URL`, `PERAZIM_AI_WORKER_SHARED_SECRET`, and `PERAZIM_AI_SUBJECT_PSEUDONYM_KEY`.

Do not configure live credentials in this repository or Android resources.

## Quota, retention, and missing deployment evidence

No authoritative atomic AI quota/reservation/settlement table or service exists in the current server schema. Define and test request idempotency, reservation, provider settlement, retries, provider timeout accounting, daily/monthly quota values and a hard spending ceiling before setting `PERAZIM_AI_ONLINE_QUOTA_ENFORCED=true`. The Worker's KV counter cannot satisfy that requirement by itself. Exact deployed Worker source/configuration, provider model availability, provider data-processing/retention terms, service URL, and current secret configuration were not verified; deployment access is intentionally outside this task.

## Request/response contract

Request: HTTPS `POST /chat`, `Authorization: Bearer <short-lived signed grant>`, JSON `{ "requestId": "<grant jti>", "question": "<bounded text>" }`.

Response: `{ "answer": "...", "remaining": <integer> }`. Errors are controlled 4xx/5xx JSON without credentials, prompts, provider response text or internal configuration. There is no automatic Qwen fallback.

## Activation checklist

Before activation, verify all of the following on a controlled deployment with synthetic accounts:

1. Expired, malformed, wrong-issuer, wrong-audience and altered grants fail; replay policy is documented and enforced.
2. Direct Android-style shared token, client identity substitution and extra context fields fail.
3. Express rejects expired/revoked sessions, inactive accounts, REGISTERED members, revoked/expired/missing permission grants and unverified permission catalogue entries before upstream dispatch.
4. Atomic quota concurrency, idempotent retry, timeout, duplicate and settlement tests pass with approved limits and spending cap.
5. Sensitive/pastoral prompts are blocked before dispatch under the reviewed pilot policy; raw text and secrets are absent from logs.
6. Worker timeout, provider error, empty response, KV outage and service outage return controlled errors without fallback.
7. Deployed source and configuration are independently matched to the reviewed repository source; provider terms and retention are documented.
8. An authenticated end-to-end synthetic test succeeds over HTTPS. Do not claim operational readiness from mock tests.
