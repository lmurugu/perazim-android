# SEC-01: Membership Claim Rate-Limiting & Abuse Prevention Requirements

**Target Component:** Backend Membership Ingestion API\
**Endpoint:** `POST /api/membership/claims`\
**Reference Finding:** Antigravity Independent Audit Finding `SEC-01`\
**Author:** Antigravity (Independent Review Agent)\
**Assigned Implementer:** Codex\
**Project Authority:** Rev. Levi\
**Document Status:** Mandatory Specification for Phase B Acceptance\

---

## 1. Vulnerability Summary & Reproduction Evidence

### 1.1 The Vulnerability
The endpoint `POST /api/membership/claims` processes incoming membership claims without HTTP-level request rate limiting.

While the data layer enforces account serialization (preventing more than one pending or review row per account via `SELECT 1 FROM membership_claims WHERE user_id=$1 AND status IN ('PENDING','REVIEW')`), the HTTP server continues to process every incoming request, execute bcrypt/database queries, and return HTTP 202 `{ received: true }` unconditionally.

### 1.2 Reproduction Proof
In the Antigravity independent test suite (`server/test/antigravity_security_audit.test.js`), an authenticated account submitted a burst of 15 rapid, distinct requests within 250 milliseconds:
- **Expected Defense:** Throttling / HTTP 429 after threshold (e.g. 3–5 requests).
- **Actual Result:** 15 out of 15 requests returned HTTP 202 Accepted.
- **Risk:** Malicious or malfunctioning clients can execute unbounded requests, causing server thread starvation, PostgreSQL connection pool exhaustion, and log flooding.

---

## 2. Rate-Limiting Functional Requirements

### 2.1 Layered Enforcement
Rate limiting MUST be applied at two distinct layers:

1. **Account-Level Rate Limiting (Primary):**
   - **Key:** Authenticated User ID (`req.user.id`).
   - **Limit:** Maximum **5 claim submission attempts per 1 hour** per user account.
   - **Rationale:** Legitimate members submit a claim once and await review. Even in case of retries, 5 attempts per hour is more than generous.

2. **IP-Level Rate Limiting (Secondary / DDoS Defense):**
   - **Key:** Client IP address (`req.ip` resolved via trusted proxy hops).
   - **Limit:** Maximum **30 claim submission attempts per 15 minutes** per IP address.
   - **Shared Network Protection (Crucial for Church Wi-Fi / Hotspots):** Church members sharing sanctuary Wi-Fi or cellular CGNAT IP pools MUST NOT be blocked due to another member's claim submission. The per-IP threshold must be higher than the per-user limit, and account-level limits must be evaluated first.

### 2.2 HTTP Response Specification (HTTP 429)
When a rate limit threshold is exceeded, the server MUST respond immediately without querying the database or classifying claims:
- **HTTP Status Code:** `429 Too Many Requests`
- **Response Headers:**
  - `Retry-After: <seconds>` (indicating wait time until next permitted attempt).
  - `Cache-Control: no-store`
- **Response Body (JSON):**
  ```json
  {
    "error": "Too many membership claim attempts. Please wait before trying again.",
    "code": "RATE_LIMITED",
    "retry_after_seconds": 3600
  }
  ```

### 2.3 Privacy & Anti-Enumeration Integrity
The rate-limiting implementation MUST preserve the core privacy guarantees of Phase B:
- An HTTP 429 response MUST NOT reveal whether previous attempts matched any record in `membership_records`.
- An HTTP 429 response MUST NOT disclose candidate counts or registration status.
- Changing `request_id`, `name`, or `phone` payload fields MUST NOT reset or bypass the user's rate-limit bucket.

---

## 3. Recommended Implementation Architecture for Codex

### 3.1 Middleware Strategy
Use a lightweight, in-memory sliding-window or token-bucket rate limiter (or `express-rate-limit` if dependency is available; otherwise a robust in-memory Map in `server/src/phase-b/security.js` with periodic cleanup).

### 3.2 In-Memory Store Safety
If implemented with in-memory Maps:
- Expire and delete old buckets automatically to prevent memory leaks.
- Ensure isolation in test mode: provide a mechanism to reset buckets between test runs (e.g., `resetRateLimits()`).

---

## 4. Verification & Acceptance Criteria

Codex's implementation will be certified by Antigravity only when the following test cases pass in the independent test runner:

1. **Controlled Burst Rejection:** When an account sends 6 requests within 1 minute, the 6th request MUST return HTTP 429 with `code: 'RATE_LIMITED'`.
2. **Retry-After Header Presence:** The HTTP 429 response includes a valid integer `Retry-After` header.
3. **Payload Variation Resilience:** Submitting different `request_id`, `name`, or `phone` values on the 6th attempt STILL yields HTTP 429.
4. **Legitimate User Isolation:** User A being rate-limited does NOT block User B from submitting a valid claim from the same IP (unless the aggregate IP threshold is breached).
5. **Anti-Enumeration Persistence:** Rate-limited responses give zero indication of register match results.
