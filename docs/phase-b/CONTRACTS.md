# Phase B contracts and access inventory

Owner authorisation: 7 November 2026, B1–B7, synthetic development only.
Baseline: 01d9e3e, branch phase-B-community. Specs and Decisions Register intact.

## Identity and access
Account activity, legacy organisational role, membership verification and explicit
permissions are independent. No existing MEMBER/is_active/login becomes verified.
All new administrative requests resolve current DB authority in a transaction,
require the sole bootstrapped pilot identity and an explicit scoped grant, and
use fresh PIN + second factor bound to the bearer session. No real activation.

Public Bible/local audio/worship/games and approved offline resources stay public.
Public feed/prayers/media retain public routing. Existing guest prayer/amen/sync
remain guest operations, never sources of registered identity. Authenticated
profile/progress, community posting/comment/RSVP, private chat, group ownership,
classes/calls and uploads keep existing participant/ownership requirements; no
verified-membership middleware is applied globally. Community account directory
is not the church register and never acquires its fields or verification results.
Legacy ADMIN shortcuts remain confined to existing endpoints and are not new
permission grants. Converting them is a compatibility rollout outside this phase.

## Verification
REGISTERED -> VERIFICATION_PENDING on own claim; neutral acknowledgement before
private matching. All no/duplicate/conflicting matches require review. Only pilot
administrator may decide VERIFIED_MEMBER or VERIFICATION_REJECTED; revocation
records VERIFICATION_REVOKED. Reapplication starts a new pending claim; changing
identity never silently transfers an approved link. History and audit are atomic.
User API never exposes candidates, candidate count or register search results.

## Canonical catalogue
membership.status.self.read: own state only
membership.claim.self.submit: own claim only
membership.record.read: scoped protected records
membership.register.import: validate/apply synthetic register
membership.claim.review: scoped review, never approval
membership.verification.confirm: reviewed link approval
membership.record.correct: register field correction, not verification
membership.verification.correct: audited link/decision correction
membership.verification.revoke: withdraw verification
access.permissions.self.read: own effective capabilities
admin.permissions.read: inspect assignments
admin.permissions.grant: explicit supported grant to sole pilot for admin powers
admin.permissions.revoke: revoke assignment
admin.audit.read: scoped minimal audit

Self-service grants use an explicit registered-account policy, no admin role
inference. Global grants mean the named capability over its documented domain;
resource grants use exact record/claim IDs. Missing/expired/revoked grants deny.
Fresh server time decides validity. No blanket override. Admin grants only to the
synthetic sole pilot. Public operations need no capability row.

## API (/api prefix retained)
GET /api/membership/status -> own status, server time; no candidate information
POST /api/membership/claims {request_id,name,phone} -> neutral receipt
GET /api/access/permissions -> own effective catalogue
GET /api/membership/member-resource -> synthetic capability test; current verified
membership AND membership.status.self.read; no Gemini or real restricted content
POST /api/admin/security/step-up {pin,code} -> session token, short expiry
All following admin calls require X-Privileged-Session and current explicit grant:
GET /api/admin/membership/claims[/:id]
GET /api/admin/membership/records
POST /api/admin/membership/import {source,records:[{reference,full_name,phone}]}
POST /api/admin/membership/claims/:id/decision {decision,record_id,reason,request_id}
POST /api/admin/membership/users/:id/revoke {reason,request_id}
POST /api/admin/membership/users/:id/correct {record_id,reason,request_id}
PATCH /api/admin/membership/records/:id {full_name,phone,reason,request_id}
GET/POST /api/admin/permissions/:userId
DELETE /api/admin/permissions/:userId/:grantId
GET /api/admin/audit
POST /api/admin/security/revoke-sessions

Import/bootstrap commands are guarded to confirmed local unique *_test databases,
NODE_ENV=test, explicit synthetic mode, no normal runtime activation. Matching
fields name+Kenyan phone are synthetic fixtures only, not approved real identity
policy. Administrator checks identity independently; candidate never auto-verifies.
Second factor uses standard TOTP with encrypted server secret and replay protection.
No privileged mutation queue and no persistent privileged Android session.

## Boundaries and gates
B1 contract complete before shared middleware edits. B2 additive 0002 must pass
preservation/repeat/failure/restore before B3. B3–B5 API security tests precede B6
integration acceptance. Only disposable PG handles new protected functions;
missing migration or memory fallback fails closed. Legacy startup/schema.sql and
0001 remain unchanged; no existing DB adoption. Room stays6 and fallback retained.
B7 requires honest executed/blocked results. Unknown Room versions/full recovery,
real register authority and matching policy/privacy/retention, real admin account,
production HTTPS and Phase C direct Worker security remain separate dependencies.
