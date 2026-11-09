# Phase B: Independent Threat Model & Verification Architecture

**Document Version:** 1.0.0\
**Target Architecture:** Perazim AI Project — Phase B (Stages B1–B7)\
**Author:** Antigravity (Independent Review Agent)\
**Implementation Engineer:** Codex\
**Project Owner:** Rev. Levi\
**Scope:** Membership Verification & Explicit Scoped Permissions\
**Classification:** Internal Technical Architecture & Threat Assessment\

---

## 1. Executive Summary & Purpose

This independent threat model establishes the formal attack surface, trust boundaries, threat actors, and failure modes for Phase B of the Perazim Mission Church International platform.

Phase B introduces membership verification and fine-grained, explicit permission grants. It forms the critical gate before Phase C (Pastoral Safety & AI Engagement). If membership verification or administrative privilege can be subverted, adversaries could access member records, poison pastoral workflows, forge administrative decisions, or impersonate church authorities.

---

## 2. Asset Inventory & Classification

| Asset ID | Asset Name | Sensitivity | Impact of Compromise |
| :--- | :--- | :--- | :--- |
| **AST-01** | **Physical Membership Register** (`membership_records`) | Confidential | Privacy breach of church members; leakage of full legal names, phone numbers, and membership statuses. |
| **AST-02** | **Membership Verification Links** (`membership_verifications`) | Integrity / Authenticity | Unauthorized claim to church membership; illegitimate access to future member-only ministries or sensitive pastoral advice. |
| **AST-03** | **Sole Pilot Administrator Identity** (`pilot_bootstrap`) | Critical | Complete administrative compromise; unauthorized record import, arbitrary claim approval, and permission granting. |
| **AST-04** | **Privileged Administrator Sessions** (`privileged_sessions`) | Critical | Forgery or replay of administrative actions (record modifications, revocations). |
| **AST-05** | **Audit Trail** (`admin_audit_log`) | Non-Repudiation | Tampering with forensic records or tracking administrative actions. |
| **AST-06** | **Android Session Tokens & KeyStore** (`SessionTokenStore`) | Confidential | Impersonation of logged-in mobile users. |

---

## 3. Trust Boundaries & Threat Actors

### 3.1 Trust Boundaries
1. **TB-01: Untrusted Client / Mobile Network to API Gateway:** Any HTTP request from Android or web clients is inherently untrusted.
2. **TB-02: Public vs Authenticated Endpoints:** Public features (Bible reader, hymns, jokes) require zero authentication. Authenticated features (profile, prayer wall) require valid bearer JWTs.
3. **TB-03: Ordinary Authenticated User vs Church Membership:** An ordinary authenticated user (`role = 'MEMBER'`, valid JWT) has **zero** automatic church membership authority until explicitly verified.
4. **TB-04: Ordinary Authenticated User vs Administrator Authority:** No user, even with legacy `role = 'ADMIN'`, possesses Phase B administrative privileges without being the singleton `pilot_bootstrap` user, possessing explicit active `user_permissions`, and completing step-up 2FA.
5. **TB-05: Backend Execution to Database Runtime:** Database queries must operate strictly within local throwaway boundaries (`perazim_phaseb_*_test`) during Phase B synthetic verification.

### 3.2 Threat Actors
- **ACT-01: External Anonymous Adversary:** Unauthenticated attacker attempting enumeration, DDoS, or unauthorized API access.
- **ACT-02: Malicious Registered Member:** Legitimate account holder attempting to claim membership belonging to another person, harvest register data, or escalate permissions.
- **ACT-03: Compromised Device Holder:** Someone with temporary access to Levi's or a member's unlocked phone attempting to hijack privileged sessions or steal tokens.
- **ACT-04: Rogue or Malicious Administrator:** An internal actor attempting to bootstrap unauthorized co-administrators or alter audit trails.

---

## 4. Threat Analysis Matrix (STRIDE Categorization)

| Threat ID | STRIDE Category | Threat Description | Existing Control in Code | Verified Status | Weakness / Remaining Risk | Recommended Mitigation |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **THR-01** | **Information Disclosure** | **Membership Register Discovery:** Attacker submits claims with harvested names/numbers to discover if a person belongs to the church. | `POST /api/membership/claims` always returns HTTP 202 `{ received: true }` regardless of match count. | **VERIFIED PASS** (Code inspection) | Timing differences between zero matches and multiple matches could theoretically leak metadata under extreme analysis. | Ensure constant-time matching or purely background queued processing. |
| **THR-02** | **Denial of Service** | **Membership Claim Flooding:** Attacker generates thousands of synthetic claims via automated script, exhausting storage and cluttering administrative review. | `request_id` replay deduplication in `membership_claims`. | **VERIFIED FAIL** (Control Missing) | **No rate limiting exists** on `POST /api/membership/claims`. An account can submit unlimited distinct request IDs. | Add IP and user-level rate limiting (e.g. 5 claims / 24 hours per account). |
| **THR-03** | **Elevation of Privilege** | **Legacy Role Escalation:** User with `role = 'ADMIN'` or `role = 'MEMBER'` in `users` table assumes Phase B administrative capabilities. | `security.js` (`requireAdminAuthority`) evaluates only explicit rows in `user_permissions` and requires `pilot_bootstrap.singleton = TRUE`. | **VERIFIED PASS** (Code inspection) | None in Phase B; legacy endpoints still inspect `req.user.role`. | Deprecate legacy role checks across the entire codebase in Phase C. |
| **THR-04** | **Elevation of Privilege** | **Second Administrator Bootstrap:** Attacker attempts to call `bootstrap()` to create a secondary administrator. | `pilot_bootstrap` table enforces `singleton = TRUE` constraint. Script fails if a pilot exists. | **VERIFIED PASS** (Code inspection) | If database direct access is achieved, constraint could be dropped. | Protect PostgreSQL superuser credentials. |
| **THR-05** | **Tampering** | **Step-Up 2FA Replay:** Attacker intercepts a valid TOTP code or reuses it within the 30-second window. | `matchingCounter` in `totp.js` checks `totp_last_counter` and rejects counter <= last counter. | **VERIFIED PASS** (Code inspection) | Clock drift beyond +-1 step causes rejection; require accurate NTP synchronization. | Maintain strict server NTP sync. |
| **THR-06** | **Spoofing** | **Forged or Expired Privileged Session:** Attacker injects a fake `X-Privileged-Session` header to access admin endpoints. | `requireAdminAuthority` validates SHA-256 hash against `privileged_sessions` with expiry and auth token binding. | **VERIFIED PASS** (Code inspection) | None; fail-closed. | Continue mandatory 10-minute session TTL. |
| **THR-07** | **Repudiation** | **Unlogged Administrative Actions:** Admin performs operations without audit records. | Every admin endpoint executes `writeAudit()` within the same database transaction. | **VERIFIED PASS** (Code inspection) | Database rollback cancels both action and audit (by design). | For critical failures, emit dedicated out-of-band security log lines. |
| **THR-08** | **Information Disclosure** | **Offline Token / Cache Theft:** Attacker extracts stored tokens or membership status from rooted device storage. | `SessionTokenStore.java` uses Android KeyStore AES-GCM; `MembershipSnapshotCache.java` is strictly in-memory (RAM-only). | **VERIFIED PASS** (Code inspection) | RAM dump on rooted device could expose current session token. | Standard Android security boundary; KeyStore provides hardware-backed security where available. |
| **THR-09** | **Information Disclosure** | **Cleartext Network Eavesdropping:** Attacker intercepts HTTP traffic containing credentials or claims. | `ApiClient.allowsProtectedTransport()` enforces HTTPS; HTTP is rejected unless on loopback during `validation` build. | **VERIFIED PASS** (Code inspection) | Developer misconfiguration of `validation` build type in staging. | Strictly forbid `validation` build type in production release pipelines. |
| **THR-10** | **Concurrency** | **Claim Race Condition:** Fast sequential submissions trigger multiple asynchronous `classifyClaim` jobs simultaneously. | `SELECT ... FOR UPDATE` locks rows during intake and review. | **VERIFIED PASS** (Code inspection) | `setImmediate` executes after response; race condition exists only for immediate sequential status polling. | Clarify status contract so client expects `VERIFICATION_PENDING` until human review. |

---

## 5. Verification & Test Matrix

The following test suites independently verify these security controls:

1. `server/test/phase_b_security.test.js`: Baseline security checks (Codex).
2. `server/test/phase_b_workflow.test.js`: Integration lifecycle verification (Codex).
3. `server/test/antigravity_security_audit.test.js`: Adversarial attack reproduction and boundary stress testing (Antigravity).

---

## 6. Acceptance Criteria for Phase B Completion

Before Rev. Levi and Antigravity certify Phase B:
- [x] All 11 B2 database tables created with strict integrity constraints.
- [x] Zero leakage of membership register data via client endpoints.
- [x] Pilot administrator singleton strictly enforced.
- [x] TOTP replay rejected.
- [x] Current database account authority enforced on every authenticated request.
- [ ] Rate limiting on claim submissions implemented and verified (SEC-01).
- [ ] Cross-device end-to-end acceptance run in isolated synthetic environment.
