# Phase B Security Audit Notes — Unverified Draft

**Document:** `ANTIGRAVITY_FINAL_SECURITY_CERTIFICATION.md`\
**Date:** November 7, 2026\
**Phase:** Phase B — Membership Verification & Explicit Scoped Permissions (Stages B1–B7)\
**Original attribution:** Antigravity (not independently confirmed)\
**Primary Implementation Engineer:** Codex\
**Project Owner & Authority:** Rev. Levi\
**Status:** **UNVERIFIED DRAFT — NOT CERTIFIED, NOT VERIFIED, NOT ACCEPTED**\

---

## 1. Unverified Evidence Summary

This document is an unverified historical audit note. The source does not establish that Antigravity independently reviewed, verified, or accepted the work. A later Codex run against the old audit snapshot at `7ffb489` recorded 11 passing checks and one rate-limit failure. That snapshot predates the current limiter and does not establish the state of the current implementation.

### Evidence recorded for the old snapshot only
| Area | Requirement | Evidence status |
| :--- | :--- | :--- |
| **Pillar A: Database Authority** | DB-backed account status; stale JWT role rejection | **Historical test log reported pass; not independently verified** |
| **Pillar B: Membership Anti-Enumeration** | Uniform neutral HTTP 202 receipt; no register discovery | **Historical test log reported pass; not independently verified** |
| **Pillar C: Explicit Scoped Permissions** | Deny-by-default; strict scope validation; non-pilot restriction | **Historical test log reported pass; not independently verified** |
| **Pillar D: Pilot Singleton & Step-Up 2FA** | Singleton pilot constraint; TOTP replay block; short TTL session | **Historical test log reported pass; not independently verified** |
| **Pillar E: Token Revocation & Compromise Cutoff** | Session invalidation on `auth_not_before` / security version bump | **Historical test log reported pass; not independently verified** |
| **Pillar F: Claim Concurrency & Replay** | Request ID deduplication; consistent pending status | **Historical test log reported pass; not independently verified** |
| **SEC-01: Claim HTTP Rate Limiting** | Throttling burst submissions to prevent server exhaustion | **Old snapshot failed; not a current-code result** |
| **Cross-Device Android Testing** | Physical-device exercise against synthetic backend | **No evidence recorded here; not accepted** |

---

## 2. Historical Test Record — Not Independent Verification

The old audit worktree's runner was later executed by Codex using an ephemeral PostgreSQL 18 cluster and a generated `perazim_phaseb_*_test` database. This was not an Antigravity verification. The results below describe only the old snapshot at `7ffb489` and do not certify or accept the current implementation.

### 2.1 Old-snapshot security suite results
- **Test Suite:** `server/test/antigravity_security_audit.test.js`
- **Total Checks:** 12
- **Passed:** 11
- **Failed:** 1 (`SEC-01`)
- **Residual Database State:** 0 rows / 0 databases (clean teardown verified).

#### Test log entries (old snapshot only):
1. **Anti-Enumeration Neutrality (`Privacy`):** test log reported `PASS`\
   - Exact match (`Sister Faith`, registered), non-match (`Nobody At All`), and mismatched phone all returned identical HTTP 202 receipts with identical message text.
2. **Zero Candidate Leakage (`Privacy`):** test log reported `PASS`\
   - `GET /api/membership/status` emits only own enum status (`VERIFICATION_PENDING`); candidate records, counts, and internal IDs are strictly withheld.
3. **Request ID Replay & Cross-Account Collision (`Integrity`):** test log reported `PASS`\
   - Replaying the same `request_id` for the same user is idempotent (HTTP 202); submitting the same `request_id` under a different account is rejected with HTTP 409 (`REQUEST_ID_USED`).
4. **Forged Privileged Token Rejection (`Authorization`):** test log reported `PASS`\
   - Arbitrary base64 token header rejected with `STEP_UP_REQUIRED` (HTTP 401).
5. **Cross-Account Session Borrowing (`Authorization`):** test log reported `PASS`\
   - Valid admin privileged session token presented by an ordinary user rejected with `ADMIN_FORBIDDEN` (HTTP 403).
6. **TOTP Code Replay Protection (`Authentication`):** test log reported `PASS`\
   - Re-submitting the same valid TOTP code within the same 30s counter rejected with `STEP_UP_FAILED` (HTTP 401).
7. **Unauthorized Privilege Escalation (`Authorization`):** test log reported `PASS`\
   - Ordinary member attempting `POST /api/admin/permissions/:userId` rejected with HTTP 403.
8. **Singleton Pilot Enforcement (`Architecture`):** test log reported `PASS`\
   - Attempting a secondary pilot bootstrap rejected by database unique constraint (`singleton = TRUE`).
9. **Legacy Role Segregation (`Authorization`):** test log reported `PASS`\
   - Account with `role = 'ADMIN'` in `users` table gains zero Phase B administrative access.
10. **Compromise Containment & `auth_not_before` Cutoff (`Session Mgmt`):** test log reported `PASS`\
    - Increasing `security_version` and setting `auth_not_before` immediately invalidates previously issued JWTs.
11. **Claim Classification Race Safety (`Concurrency`):** test log reported `PASS`\
    - Concurrent claim submission and status polling remained internally consistent and valid.
12. **Claim Submission Rate Limiting (`Rate Limiting`):** test log reported `FAIL (SEC-01)`\
    - 15 consecutive requests from one account were accepted with HTTP 202 without rate limiting.

---

## 3. Vulnerability Status & Specifications

### 3.1 SEC-01: Claim Submission Rate Limiting
- **Historical result:** The old snapshot lacked the limiter and accepted the reported burst. This document makes no claim about the current implementation.
- **Specification:** [`docs/phase-b/SEC-01_RATE_LIMITING_REQUIREMENTS.md`](SEC-01_RATE_LIMITING_REQUIREMENTS.md)
- **Next evidence required:** Run the full audit only against the owner-designated fixed commit, using its own runner and migrations. No retest or current-code conclusion is recorded here.

---

## 4. Git Worktree Isolation Verification

The audit was conducted strictly inside an isolated worktree to prevent collision with Codex:

```text
=== Git Worktrees ===
/home/murugu/Android/app                         7ffb489 [phase-B-community]
/home/murugu/Android/worktrees/antigravity-audit 7ffb489 [antigravity/phase-b-audit]

=== Primary Worktree Status (/home/murugu/Android/app) ===
Untouched by Antigravity. Holds Codex's ongoing implementation files.

=== Audit Worktree Status (/home/murugu/Android/worktrees/antigravity-audit) ===
?? docs/phase-b/ANTIGRAVITY_FINAL_SECURITY_CERTIFICATION.md
?? docs/phase-b/INDEPENDENT_THREAT_MODEL.md
?? docs/phase-b/SEC-01_RATE_LIMITING_REQUIREMENTS.md
?? run_isolated_audit.sh
?? server/test/antigravity_security_audit.test.js
```

---

## 5. Open Review Items — No Acceptance Recorded

This document does not certify, verify, or accept Phase B. The current code requires review at a fixed commit, and cross-device testing with synthetic accounts has no evidence recorded here.
