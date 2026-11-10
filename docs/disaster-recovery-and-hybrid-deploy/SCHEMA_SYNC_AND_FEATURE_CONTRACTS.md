# SCHEMA, SYNC AND FEATURE CONTRACTS — REVIEW DRAFT

**Date:** 2026-11-10. **No executable migrations or application code are changed here.**
**Authority:** Express validates identity, membership, permissions, and state transitions; PostgreSQL is the sole shared-data authority; Android Room and future iOS SQLite are local projections with durable pending operations.

## 1. Repository-grounded baseline

- Existing Android database: src/main/java/org/perazimchurch/app/data/local/PerazimDatabase.java, **version 6**, schema exports under schemas/, supported migrations 4->5 and 5->6; fallbackToDestructiveMigration currently exists and must not be silently removed or relied upon for future user-data migrations.
- Existing Android queue: SyncQueueEntity and SyncQueueDao, with pending, syncing, failed/rejected, and orphaned-sync recovery.
- Existing server sync: server/src/routes/sync.js, POST /api/sync. It uses optionalAuth and supports certain entity-specific operations. Do not assume all proposed entities are safe or supported through this endpoint; audit guest fallback, payload ownership, per-entity authorization and atomic replay before extension.
- Existing online AI: server/src/routes/ai.js, POST /api/ai/online. It requires authenticated Express session, verified membership, active explicit permission, server-side privacy and quota flags, and a signed-v1 Worker contract. All gates remain disabled until separately approved. Existing ConversationHistory.java is in-memory, NOT Room persistence.
- Existing giving: UI handoff to M-Pesa app/SIM/USSD and manual treasurer reconciliation; no deployed Daraja C2B ledger.
- Existing notification history: in_app_notifications in server/schema.sql and Android NotificationEntity. FCM is not implemented.
- Existing church media: external YouTube/other permitted provider playback and local metadata; no assumption that provider video bytes are backed up by Perazim.

## 2. Global mutation envelope — proposed v1 API contract

Every client-created shared-data mutation must have:

| Field | Contract |
|---|---|
| operation_id | UUID generated once on client; never regenerated on retry |
| entity_type, entity_id, operation | Explicit allowlisted values |
| actor | Derived from validated server session, never client-supplied role/identity |
| client_created_at | Informational only; never authoritative ordering |
| base_revision | Server-issued version for edit conflict detection |
| payload | Size-limited, schema-validated, field-allowlisted; no credentials |
| result | APPLIED / ALREADY_APPLIED / REJECTED_CONFLICT / REJECTED_AUTH / RETRYABLE |
| server_revision, server_time | Returned by Express after authoritative commit |
| cursor | Opaque server pagination token, scoped to authenticated identity |

**Atomicity:** PostgreSQL must commit the domain mutation and idempotency receipt in one transaction. Use a unique operation_id (scoped to actor where necessary), and compare a canonical request fingerprint on replay; a reused ID with different payload is a conflict, not a second write. On network timeout after server commit, retry same ID and receive ALREADY_APPLIED. Use transactional outbox for notifications rather than sending push inside an uncommitted DB transaction.

**Client queue lifecycle:** PENDING -> IN_FLIGHT -> ACKNOWLEDGED or RETRY_WAIT / PERMANENT_REJECTION. A process crash reclassifies orphaned IN_FLIGHT to PENDING without changing operation_id. Retry with exponential backoff + jitter, respect Retry-After, and stop on logout/identity change until user confirms account-scoped recovery. Never delete unacknowledged operations to hide a conflict. WorkManager network constraints are opportunistic; a manual sync action remains available.

**Conflicts:** server wins for membership, privacy consent, role, giving status, notification read ownership, and permissions. Create-once prayer/amen/event RSVP uses stable IDs/uniqueness. Edits use server revisions/ETag and 409 with a human-resolvable conflict record. Deletes require tombstones and authorization; do not resurrect deleted items from stale offline caches. Server cursor must not leak private groups, direct messages, or other accounts' data.

**Security gate:** Existing optional-auth sync is NOT approved as a transport for private messages, payment confirmations, AI history, or administrative changes. Restrict sensitive entity types to authenticated, server-authorized endpoints. Build adversarial tests for forged userId, swapped actor, stale JWT, replay, oversized payload, cross-group access, and 401/403/429.

## 3. AI spiritual guide — private offline-first by default

### Proposed Android Room additions (not yet implemented)

| Table | Minimal fields | Privacy rule |
|---|---|---|
| ai_sessions | id, local_account_id, title_opt_in, created_ms, updated_ms, retention_policy, deleted_ms | Local-only by default; partition by account; do not include in generic sync |
| ai_messages | id, session_id, role, body_encrypted_or_protected, mode (LOCAL/ONLINE), created_ms, local_status, sensitivity_flag | Store only after user opts in; no raw prompt in logs or push |
| ai_pending_online_requests (optional) | id, session_id, message_id, requested_ms, expires_ms, consent_snapshot, status | Explicit per-prompt consent; must be reviewed again on reconnect; never silently submit pastoral content |

**Behavior:** Offline AI should use the already-approved on-device engine/knowledge when available; a deterministic scripted answer is labeled as such, not claimed generative. The inspected build currently disables OFFLINE_QWEN_ENABLED, so device capability requires renewed evidence. Online questions are sent only by deliberate user action to Express POST /api/ai/online; Express checks current membership, permission, quota, safety and Worker configuration. Existing Gemini/Cloudflare Worker is the approved provider route; OpenRouter is NOT adopted by this draft. Never put provider API keys in APK/IPA. The Worker does not receive client-supplied role, name, chat history or arbitrary context.

**Retention and user rights:** Design explicit save-history toggle (default OFF), per-session delete, all-history delete, device backup policy, sensitive content warning, retention expiry, and optional encrypted local storage. No automatic cross-device AI history sync or server prompt archival without a separate approved purpose, privacy notice, consent/legal basis, threat model, and deletion mechanism. A pending prompt that expires or loses entitlement stays local and requires a fresh user action. AI advice must not masquerade as pastoral authority or medical/financial guidance.

**AI test matrix:** airplane mode, disabled model, low memory, app kill, account switch, history off, delete history, expired queued prompt, revoked online permission, quota race, Worker unavailable, unsafe prompt, no logs or network submission without explicit consent.

## 4. M-Pesa giving — intent is not payment

**Paybill:** 4069983 (already in public README). The existing handoff is a convenience flow, not proof that money was transferred.

### Proposed client state machine

~~~text
INITIATED --handoff--> PENDING_CONFIRMATION
      \--handoff cancelled--> UNRESOLVED (or FAILED for local handoff only)
PENDING_CONFIRMATION --authoritative matched evidence--> VERIFIED
PENDING_CONFIRMATION --timeout/no callback--> NEEDS_REVIEW
NEEDS_REVIEW --treasurer/provider reconciliation--> VERIFIED or FAILED
VERIFIED --later refund/dispute--> DISPUTED/REFUNDED (append-only event)
~~~

**Never** promote a payment to VERIFIED from a user-entered receipt, offline timestamp, M-Pesa app launch success, local screenshot, or optimistic sync response. A payment may succeed even when the app never receives a callback; lack of callback is not proof of failure. A verified record is immutable as an event; corrections are compensating audit events.

### Proposed Android Room table

**giving_transactions** (local projection / giving intent): client_intent_id PK, local_account_id, paybill, account_reference_opt_in, expected_amount_minor_units, currency=KES, created_ms, state, last_server_revision, server_transaction_id_nullable, last_checked_ms. Do not store PIN, M-Pesa credentials, full SMS, or unneeded payer personal data. Client can queue only intent creation or explicit status-refresh request; NEVER queue a verified-payment mutation.

### Proposed PostgreSQL migration sketch (NOT READY TO EXECUTE)

~~~sql
-- Design sketch; use a new numbered migration only after security/finance approval.
CREATE TABLE giving_transactions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id),
    client_intent_id UUID NOT NULL,
    paybill VARCHAR(32) NOT NULL,
    account_reference TEXT,
    expected_amount_minor BIGINT,
    confirmed_amount_minor BIGINT,
    currency CHAR(3) NOT NULL DEFAULT 'KES',
    state VARCHAR(32) NOT NULL,
    provider_receipt_id VARCHAR(128) UNIQUE,
    verification_source VARCHAR(32),
    verified_by VARCHAR(64),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    verified_at BIGINT,
    UNIQUE (user_id, client_intent_id),
    CHECK (expected_amount_minor IS NULL OR expected_amount_minor > 0),
    CHECK (confirmed_amount_minor IS NULL OR confirmed_amount_minor > 0),
    CHECK (state IN ('INITIATED','PENDING_CONFIRMATION','NEEDS_REVIEW',
                    'VERIFIED','FAILED','DISPUTED','REFUNDED'))
);
CREATE TABLE payment_reconciliation_events (
    id UUID PRIMARY KEY,
    transaction_id UUID REFERENCES giving_transactions(id),
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(128),
    provider_receipt_id VARCHAR(128),
    event_type VARCHAR(48) NOT NULL,
    evidence_reference TEXT,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64),
    received_at BIGINT NOT NULL,
    UNIQUE (provider, provider_event_id)
);
~~~

This sketch is intentionally incomplete until finance/privacy signoff: define secure provider event admission, permitted nullable event IDs, external receipt matching, donor-account reference rules, amount mismatch, C2B reconciliation, ledger immutability, chargeback/refund handling, role separation, audit retention, encryption, and migrations.

**Daraja C2B trust boundary:** Standard Safaricom C2B callbacks have no documented inbound HMAC signature. A callback alone is neither a cryptographically signed receipt nor independent proof of settlement. Use a high-entropy, environment-specific secret in the registered HTTPS callback route (never in Git, APK or logs), strict bounded JSON schema/type checks, approved Paybill/business-number matching, unique receipt ID, immutable idempotency/replay records, duplicate/out-of-order handling, and rate limits. A route key is a bearer secret that may leak and must be rotated; optional IP allowlists are defense-in-depth, not authentication. Only transition to VERIFIED after an authorized Safaricom Transaction Status inquiry **if supported and permitted for that transaction** or independently reconciled merchant/treasury evidence, matching Paybill, amount, account reference and receipt with an auditable verifier. Otherwise remain NEEDS_REVIEW; do not assume universal Transaction Status entitlement. Use a separate append-only payment evidence store; no generic sync endpoint may write VERIFIED.

### Proposed server workflow

1. Authenticated user creates a giving intent (optional; users may pay outside the app). Express creates idempotent PENDING_CONFIRMATION; app opens M-Pesa handoff.
2. Paybill transaction occurs outside Perazim. Only the provider/merchant and church treasury can establish settlement.
3. **If separately authorized for Daraja:** register stable HTTPS C2B confirmation/validation endpoints with merchant-owner approval. Require a secret route key, bounded payload/schema, unique receipt, replay defense, expected Paybill/amount/reference, and independent treasury reconciliation; there is no standard inbound C2B HMAC signature.
4. Commit unverified callback evidence atomically; promote to VERIFIED only after independent authorized provider-status or merchant-statement reconciliation, in an auditable transaction; match a member intent only with unambiguous evidence. Unmatched receipts enter a restricted treasurer review queue; do not expose donor identity publicly.
5. Reconcile against Safaricom merchant statements/approved independent source. A verified transaction appears in the member's authenticated private view via Express; app caches read-only server status. Corrections require dual-role audit or approved finance procedure.
6. If Daraja is not authorized, retain M-Pesa handoff and an optional user-visible UNVERIFIED intent, with treasurer manual verification; do not advertise automated receipts.

**Finance test matrix:** callback replay, duplicate receipt, spoofed callback, wrong Paybill, wrong account, amount mismatch, concurrent clients, missing callback, out-of-order confirmation, server crash between event and ACK, refund, user account switch, restored database + replay. All tests use synthetic data and no real transfers.

## 5. FCM/APNs — alerts only, never application authority

**Proposed PostgreSQL:** device_push_registrations(id, user_id, platform ANDROID/IOS, installation_id, encrypted_provider_token, token_hash, consent_at, last_seen_at, revoked_at); notification_outbox(id, user_id, event_id, event_type, payload_class, created_at, sent_at, retry_count, last_error) with UNIQUE(user_id,event_id,event_type). Existing in_app_notifications remains the authoritative history, filtered by recipient.

**Proposed endpoints (NOT IMPLEMENTED):**
- PUT /api/push/devices — authenticated, consented token upsert; server derives user.
- DELETE /api/push/devices/{installation_id} — logout, token rotation, device removal.
- GET /api/notifications?cursor=... — authenticated, recipient-scoped authoritative fetch (reuse existing route if equivalent exists).
- POST /api/notifications/{id}/read — recipient-scoped idempotent update.
- Internal event publisher -> durable outbox -> FCM HTTP v1 -> Android; on iOS, FCM-to-APNs bridge or direct APNs, subject to Apple credentials and platform review.

**Event order:** commit prayer Amen/message/urgent prayer event and in-app notification -> outbox -> background sender -> provider push -> device wakes -> app authenticates and pulls record. Push may arrive late, duplicated, or not at all; clients dedupe by event ID and show in-app history on refresh. For sensitive events use generic text such as 'You have a new Perazim update', never message text, private prayer details, phone numbers, donation amounts, or AI prompts.

**Delivery and cost:** Firebase Cloud Messaging is listed as no-cost in current Firebase pricing, but project registration, server operation, Apple distribution, and future provider policy/quotas still matter. No guaranteed instant delivery, especially on battery-optimized Android devices. Test Infinix Android 11 idle/Doze, app background/force-stop, token rotation, logout, no-permission case, and duplicate delivery. Android 13+ requires runtime notification permission. iOS needs APNs entitlement, real device, Apple signing and distinct acceptance.

**Avoid dual realtime stacks for pilot:** Start with FCM + authoritative REST pull and periodic WorkManager sync. Supabase Realtime or a persistent WebSocket is optional later only if measured latency and battery needs justify it; do not add a second authority or an unmonitored always-on server.

## 6. API contract and versioning

Before coding, commit an OpenAPI 3.x contract and synthetic JSON fixtures for: auth/session expiry, member permission errors, /api/sync idempotency/revisions/cursors, AI online request, giving intent/read-only verification, notification token registration/read, media metadata and expiry, and standardized error codes. Android and iOS must run the SAME conformance fixtures. Contract-breaking changes require version bump, migration plan, compatibility window, and explicit release gate.

**No executable migration in this documentation PR.** The future migration must:
1. Allocate next PostgreSQL numbered migration without changing checksums of 0001–0012.
2. Allocate a non-destructive Room v6->v7 migration with exported compiler schema and synthetic v4/v5/v6 upgrade coverage; preserve queued operations, Bible, saved preferences and account data. All later post-pilot schema changes require the same tested non-destructive rule; destructive fallback is not an approved migration mechanism.
3. Implement server API and unit/integration/security tests with disposable DB.
4. Implement Android repository/WorkManager with controlled test APK, then independently implement iOS client.
5. Verify no old client can forge VERIFIED giving, online AI entitlement, membership, or notification recipients.
6. Pass full isolated restore + rollback rehearsal before any real rollout.

## 7. Cross-platform field parity

| Contract | Android now | iOS target |
|---|---|---|
| Local data | Room/SQLite v6 | SQLite/GRDB or approved equivalent |
| Sync | Existing Room queue; hardening planned | Same operation IDs, conflicts, cursor and retry rules |
| Offline AI | Existing local engine/spec, current build gated | Separate model/runtime feasibility proof |
| Online AI | Express permission-gated, currently OFF | Same Express entitlement, never client-side provider secret |
| Giving | M-Pesa intent handoff, manual verification | External authorized payment handoff; same server verification |
| Push | FCM planned | APNs (optionally via FCM) planned |
| Bible and media | Existing Android assets and provider embeds | Rights-reviewed iOS assets and platform-appropriate player |
| Identity | Existing JWT/PIN plus approved verification policies | Equivalent API; Keychain for tokens; never duplicate authority |

**Open decisions:** AI retention and consent; Daraja production entitlement; C2B evidence source; donor reconciliation and data retention; push consent and payload categories; iOS release mechanism; server hosting, independent backup target, and recovery custodian. None may be silently inferred by implementers.

## 8. Source documents

- ../PERAZIM_AI_PROJECT/DECISIONS_REGISTER.md
- ../PERAZIM_AI_PROJECT/16_API_AND_DATABASE_CONTRACTS.md
- ../PERAZIM_AI_PROJECT/15_DATA_RETENTION_AND_PRIVACY.md
- ../phase-b/ONLINE_AI_WORKER_INTEGRATION_HANDOFF.md
- ../foundation/ROOM_MIGRATIONS.md
- https://firebase.google.com/pricing/
- https://developer.safaricom.co.ke/apis/CustomerToBusiness
