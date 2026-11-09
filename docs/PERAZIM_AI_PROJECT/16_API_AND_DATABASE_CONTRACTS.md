# 16 — API AND DATABASE CONTRACTS

## 1. Purpose

This document defines the target API responsibilities, database structures, entity relationships, data contracts, and migration requirements for the Perazim Missions Church International AI system.

It covers:

1. Authentication and verified membership.
2. Permission management.
3. AI requests and orchestration.
4. AI usage accounting and quotas.
5. Knowledge administration and retrieval.
6. Offline knowledge package distribution.
7. Pastoral escalation.
8. Attendance.
9. Reminders and AI deep links.
10. Administrative audit records.
11. Database migration and integrity requirements.

The document is an implementation contract, not permission to replace the existing application architecture.

Codex must inspect the actual repository before designing migrations or implementing endpoints.

Where existing code already performs a required function correctly, reuse or extend it.

Where the existing implementation conflicts with this contract, document the conflict and propose the smallest safe correction.

## 2. Existing Technology Stack

The system must preserve the existing architecture unless repository inspection establishes a compelling technical reason for a specific change.

| Component | Required technology |
|---|---|
| Android application | Native Android, Java |
| Local database | Room / SQLite |
| Offline generative AI | Existing Qwen model through llama.cpp |
| Backend API | Node.js / Express |
| Server database | PostgreSQL |
| Online generative AI | Gemini through Cloudflare Worker |
| Edge service | Existing Cloudflare Worker |
| Knowledge and evaluation utilities | Python development toolkit |
| Offline knowledge distribution | Versioned, signed packages |

Python must remain a development, knowledge-processing, and evaluation toolkit. Do not introduce a production Python/FastAPI backend.

## 3. API Architecture

### 3.1 Target request flow

The intended online AI flow is:

```text
Android Application
       |
       v
Express Backend
       |
       +--> Authentication
       |
       +--> Verified Membership
       |
       +--> Permission Validation
       |
       +--> Scope and Safety Classification
       |
       +--> Quota Reservation
       |
       +--> Approved Knowledge Retrieval
       |
       +--> AI Orchestration
                 |
                 v
          Cloudflare Worker
                 |
                 v
               Gemini
                 |
                 v
          Response Validation
                 |
                 v
          Quota Settlement
                 |
                 v
          Android Application
```

Express is the authoritative application backend for identity, membership, permissions, quota accounting, approved knowledge retrieval, and request auditing.

Cloudflare Worker acts as a protected provider gateway. It must not independently decide whether a person is a verified Perazim member or whether that person has an application permission.

The Android application must not be trusted to supply its own verified membership status, administrative role, quota balance, or access permissions.

### 3.2 Offline request flow

```text
Android Application
       |
       v
Local Access and Safety Checks
       |
       v
Question Classification
       |
       +--> Deterministic Approved Response
       |
       +--> Local Approved Knowledge Retrieval
       |
       +--> Exact Local Cache
       |
       +--> Qwen Offline Generation
       |
       v
Local Response Validation
       |
       v
Display Response and Available Sources
```

The offline system must function without internet connectivity.

It must use only locally available and authorised resources.

The offline system must not pretend that it has verified current online information.

### 3.3 Separate attendance flow

Attendance must remain separate from AI requests.

```text
Android Attendance Interface
       |
       v
Attendance Session Validation
       |
       v
Attendance API
       |
       v
PostgreSQL
       |
       v
Attendance Validation and Recognition
       |
       v
Attendance Status
```

Attendance data must not pass through the AI orchestration service.

## 4. API Design Standards

All new API endpoints must follow consistent conventions.

### 4.1 Base path

Use the existing backend API prefix where one is already established.

If no consistent prefix exists, use:

`/api/v1/`

Do not introduce a second competing API prefix.

### 4.2 Request format

Requests and responses should use JSON unless an endpoint specifically requires file transfer or binary content.

Example:

```json
{
  "request_id": "uuid",
  "data": {
    "example": "value"
  }
}
```

The exact envelope may be adapted to the existing API conventions, provided the API remains consistent and unambiguous.

### 4.3 Response format

Successful responses should communicate:

- Whether the operation succeeded.
- The relevant result.
- A stable request or resource identifier where appropriate.
- Any relevant version or status information.

Errors should communicate:

- A stable error code.
- A safe human-readable message.
- Whether retrying is appropriate.
- A request identifier for troubleshooting.

Example:

```json
{
  "success": false,
  "error": {
    "code": "MEMBERSHIP_VERIFICATION_REQUIRED",
    "message": "Verified membership is required for this operation.",
    "retryable": false
  },
  "request_id": "uuid"
}
```

Do not return stack traces, SQL errors, provider credentials, internal tokens, or sensitive implementation details to the Android application.

### 4.4 Identifiers

Use stable unique identifiers.

UUIDs are the preferred default for new externally referenced resources, subject to compatibility with the existing database.

Do not expose sequential database identifiers unnecessarily.

### 4.5 Timestamps

Server-generated timestamps must be authoritative for security-sensitive operations.

Use UTC timestamps in API responses, preferably ISO 8601.

Example:

`2026-11-07T10:30:00Z`

The client may display timestamps in local time.

Device time must not be trusted for membership verification, quota accounting, security decisions, or automatic attendance recognition.

### 4.6 Idempotency

Operations that may be retried must support idempotency.

An idempotent operation produces no duplicate side effect when the same request is submitted more than once.

This is particularly important for:

- AI quota reservations.
- AI quota settlements.
- Attendance submissions.
- Attendance corrections.
- Knowledge package publication.
- Membership verification.
- Escalation submissions.

Use unique request identifiers or idempotency keys with backend-enforced uniqueness.

## 5. Authentication and Membership APIs

Authentication and church membership verification are separate concepts.

A valid application account does not automatically establish verified church membership.

### 5.1 Existing authentication

Codex must inspect and reuse the existing authentication implementation, including account registration, credential handling, JWT issuance, token validation, PIN hashing, revocation, and middleware.

Do not introduce a competing authentication system without a documented technical justification.

### 5.2 Membership status endpoint

Proposed endpoint:

`GET /api/v1/membership/status`

Purpose:

Return the authenticated user's own membership verification status.

Example response:

```json
{
  "success": true,
  "membership": {
    "status": "VERIFICATION_PENDING",
    "verified": false,
    "verification_reference": "uuid"
  }
}
```

Possible statuses:

- `REGISTERED`
- `VERIFICATION_PENDING`
- `VERIFIED_MEMBER`
- `VERIFICATION_REVIEW`
- `VERIFICATION_REJECTED`
- `VERIFICATION_REVOKED`

The endpoint must not expose the complete membership register.

### 5.3 Membership verification submission

Proposed endpoint:

`POST /api/v1/membership/verification`

Purpose:

Submit the information required to initiate or continue membership verification.

The request must contain only the approved identifying information needed for matching.

The backend must perform the actual matching against the protected membership register.

The Android client must not receive unrestricted membership search results.

### 5.4 Administrative membership review

Proposed endpoints:

- `GET /api/v1/admin/membership/verifications`
- `GET /api/v1/admin/membership/verifications/:id`
- `POST /api/v1/admin/membership/verifications/:id/decision`

Purpose:

Allow authorised administrators to review ambiguous or exceptional membership verification cases.

The decision endpoint must validate explicit permission on the backend.

A verification decision must record:

- Verification record identifier.
- Account identifier.
- Authorised administrator identifier.
- Decision.
- Decision timestamp.
- Reason or administrative note where appropriate.
- Relevant source membership record reference.
- Previous and resulting status.

The pilot's authorised technical administrator is the only person authorised to approve membership verification unless an explicit authorised delegation is later approved.

### 5.5 Membership register import

Proposed endpoint:

`POST /api/v1/admin/membership/import`

Purpose:

Import an authorised membership list.

The import process must:

- Validate the file format.
- Validate required fields.
- Detect duplicates.
- Preserve existing verified records where appropriate.
- Avoid overwriting valid records silently.
- Report ambiguous matches.
- Produce an import audit record.
- Restrict access to authorised administrators.

The import must not expose the complete register to ordinary application users or AI models.

## 6. Permission APIs

Permissions must be explicit, independently assigned, and enforced by the backend.

### 6.1 Permission catalogue

Proposed endpoint:

`GET /api/v1/access/permissions`

Returns the permissions available to the authenticated account.

The response should distinguish effective permissions from membership status.

Example:

```json
{
  "success": true,
  "membership_status": "VERIFIED_MEMBER",
  "permissions": [
    "ai.online.use",
    "knowledge.public.read"
  ]
}
```

This response informs the interface. It does not replace backend authorisation.

### 6.2 Permission assignment

Proposed administrative endpoints:

- `GET /api/v1/admin/permissions`
- `GET /api/v1/admin/users/:id/permissions`
- `POST /api/v1/admin/users/:id/permissions`
- `DELETE /api/v1/admin/users/:id/permissions/:permission`

Each operation must validate the administrator's explicit permission.

### 6.3 Permission examples

The permission catalogue should support capabilities such as:

```text
ai.offline.use
ai.online.use
ai.online.research
ai.usage.view
ai.configuration.manage

knowledge.public.read
knowledge.internal.read
knowledge.restricted.read
knowledge.create
knowledge.review
knowledge.approve
knowledge.publish
knowledge.withdraw

membership.verify
membership.review
membership.import
membership.correct

attendance.view
attendance.manage
attendance.correct
attendance.export

pastoral.case.submit
pastoral.case.review
pastoral.case.assign
pastoral.case.respond

permissions.view
permissions.assign
permissions.revoke

audit.view
```

Codex may refine naming conventions, but permissions must remain granular and enforceable.

Do not assume that an account with a title such as minister, bishop, administrator, or ministry leader automatically possesses every permission.

## 7. AI Request API

### 7.1 Main endpoint

Proposed endpoint:

`POST /api/v1/ai/ask`

Purpose:

Receive an authenticated online AI request and orchestrate its processing.

The endpoint must perform the necessary checks before dispatching content to an external provider.

### 7.2 Example request

```json
{
  "request_id": "uuid",
  "message": "What does Romans 8:28 mean?",
  "conversation_context": [
    {
      "role": "user",
      "content": "I am studying Romans 8."
    }
  ],
  "preferred_translation": "ESV",
  "response_mode": "standard"
}
```

The exact request fields may be adapted to the existing Android implementation.

The backend must not trust the client to decide:

- Whether the user is a verified member.
- Whether the user may use Gemini.
- Whether the content is safe for external processing.
- Which restricted knowledge the user may access.
- How many tokens the user may consume.
- Whether the user may perform external research.

The backend must derive these decisions from authoritative records and server-side policy.

### 7.3 Conversation context

The application may submit limited temporary context necessary to answer a follow-up question.

The context must be subject to the same safety and privacy checks as the latest message.

The backend must not create permanent conversation history as a side effect of receiving the request.

### 7.4 AI response

Example response:

```json
{
  "success": true,
  "request_id": "uuid",
  "response": {
    "text": "The passage teaches that God works in all things for the good of those who love Him...",
    "model": "gemini",
    "mode": "online",
    "sources": [
      {
        "type": "scripture",
        "reference": "Romans 8:28",
        "translation": "ESV"
      }
    ],
    "limitations": []
  },
  "usage": {
    "input_tokens": 120,
    "output_tokens": 240
  }
}
```

The response must identify the actual processing mode and model where appropriate.

It must not invent citations or claim to have retrieved sources that were not actually used.

If an approved church document was used, the response should include its authorised title and revision or another stable source reference.

### 7.5 AI access endpoint

Proposed endpoint:

`GET /api/v1/ai/access`

Purpose:

Return the authenticated user's current AI access capabilities.

Example:

```json
{
  "success": true,
  "access": {
    "offline": true,
    "online": true,
    "external_research": false
  }
}
```

This endpoint is informational. Every actual request must still be authorised independently.

## 8. AI Quota and Usage APIs

### 8.1 Usage endpoint

Proposed endpoint:

`GET /api/v1/ai/usage`

Purpose:

Return the authenticated user's permitted usage information.

Example:

```json
{
  "success": true,
  "usage": {
    "period": "daily",
    "requests_used": 4,
    "requests_remaining": 6,
    "tokens_used": 2400,
    "quota_status": "AVAILABLE"
  }
}
```

Actual quotas must be configurable and controlled by authorised policy.

### 8.2 Quota reservation

Before an online provider request is dispatched, the backend must reserve the relevant quota.

Reservation must be transactional and concurrency-safe.

The implementation must not use an unprotected read-increment-write pattern in Cloudflare KV as the authoritative quota mechanism.

PostgreSQL is the recommended authority for usage accounting.

### 8.3 Quota settlement

After the provider responds, the backend must settle the reservation using actual usage information where available.

Settlement must be idempotent.

A timeout, duplicate callback, or retried request must not charge the user repeatedly.

The system should distinguish:

- Request allowance.
- Token usage.
- Estimated provider cost.
- Actual provider cost where available.
- Failed requests.
- Refunded or released reservations.

### 8.4 Hard spending controls

The system must support:

- Per-user limits.
- Per-role limits where authorised.
- Daily limits.
- Monthly limits.
- Global provider spending limits.
- Emergency disablement of online requests.

A hard spending limit must be enforced by the backend rather than only by the Android interface.

## 9. Knowledge Management APIs

### 9.1 Knowledge document model

Every knowledge document must have a stable identity and version history.

Required conceptual fields:

```text
document_id
title
category
content_type
source_reference
authority_level
approval_status
audience
offline_eligible
current_revision_id
created_at
created_by
updated_at
updated_by
withdrawn_at
withdrawn_by
```

The exact database structure may separate document metadata from document revisions.

### 9.2 Knowledge document states

Recommended states:

- `DRAFT`
- `PENDING_REVIEW`
- `APPROVED`
- `PUBLISHED`
- `WITHDRAWN`
- `REJECTED`
- `SUPERSEDED`
- `EXPIRED`

Only authorised approval and publication operations may make content available to users.

A draft must never appear in ordinary AI retrieval.

### 9.3 Knowledge administration endpoints

Proposed endpoints:

- `POST /api/v1/admin/knowledge/documents`
- `GET /api/v1/admin/knowledge/documents`
- `GET /api/v1/admin/knowledge/documents/:id`
- `PUT /api/v1/admin/knowledge/documents/:id`
- `POST /api/v1/admin/knowledge/documents/:id/submit`
- `POST /api/v1/admin/knowledge/documents/:id/approve`
- `POST /api/v1/admin/knowledge/documents/:id/publish`
- `POST /api/v1/admin/knowledge/documents/:id/withdraw`
- `GET /api/v1/admin/knowledge/documents/:id/revisions`

The backend must enforce the pilot's approval authority and maintain revision history.

### 9.4 Knowledge retrieval endpoint

Proposed internal service responsibility:

`retrieveApprovedKnowledge(user, question, context)`

This may be an internal backend service rather than a publicly accessible endpoint.

It must:

1. Identify relevant approved documents.
2. Apply audience and permission filters.
3. Exclude drafts and withdrawn content.
4. Respect revision and expiry rules.
5. Return relevant evidence with stable source identifiers.
6. Avoid exposing restricted material to unauthorised users.
7. Return sufficient provenance for answer citations.

The AI model must not be the authority deciding whether a user may access a document.

### 9.5 Knowledge chunks

Long documents may be divided into smaller retrieval chunks.

Each chunk should retain:

- Chunk identifier.
- Parent document identifier.
- Revision identifier.
- Content.
- Section or heading.
- Source location or page reference where available.
- Audience.
- Embedding metadata if embeddings are introduced later.

The initial retrieval implementation should favour reliable exact-reference and full-text retrieval.

Semantic embeddings may be introduced after retrieval evaluation demonstrates a meaningful benefit.

### 9.6 Knowledge authority

The database must preserve the authority classification defined in Document 04.

Technical approval of knowledge content must not be represented as ecclesiastical authority to amend doctrine.

The pilot administrator may approve knowledge-base publication, but formal doctrinal changes must follow the church's authorised governance process.

## 10. Offline Knowledge Package APIs

### 10.1 Package manifest

Every package must have a manifest describing:

```text
package_id
package_version
audience
issued_at
expires_at
minimum_app_version
content_revision
document_ids
document_hashes
package_hash
signing_key_id
signature
```

Additional fields may be included where necessary.

### 10.2 Package endpoints

Proposed endpoints:

- `GET /api/v1/knowledge/packages/available`
- `GET /api/v1/knowledge/packages/:id/manifest`
- `GET /api/v1/knowledge/packages/:id/download`

The backend must filter packages according to the authenticated user's current permissions.

### 10.3 Package security

Packages must use asymmetric signing.

The private signing key must remain server-side or within an appropriately protected signing environment.

The Android application must contain only the public verification key or another appropriately designed trust mechanism.

The application must verify:

- Signature validity.
- Package integrity.
- Manifest structure.
- Package audience.
- Expiry.
- App compatibility.
- Content hashes.

A package must not be activated before successful verification.

### 10.4 Atomic package activation

The application must preserve the currently active valid package while downloading and validating a replacement.

Only after complete validation should the new package become active.

An interrupted download must not corrupt the active package.

The application should retain an appropriate previous valid package for rollback, subject to expiry and permission rules.

### 10.5 Offline revocation limitation

A disconnected device cannot receive immediate revocation instructions.

Restricted offline content must therefore use bounded expiry and appropriate access limitations.

The system must not claim that remote revocation instantly removes already downloaded content from disconnected devices.

## 11. Pastoral Escalation APIs

Pastoral escalation is a separate protected workflow.

It must not be implemented as ordinary AI chat history.

### 11.1 Submission endpoint

Proposed endpoint:

`POST /api/v1/escalations`

Example request:

```json
{
  "submission_id": "uuid",
  "submission_type": "ANONYMOUS",
  "category": "PASTORAL_CONCERN",
  "message": "I would like someone to follow up with me.",
  "consent": true
}
```

For identified submissions, the backend may associate the case with the authenticated account and necessary contact information.

The anonymous submission process must minimise identity exposure and clearly explain its limitations.

### 11.2 Escalation states

Recommended states:

- `SUBMITTED`
- `RECEIVED`
- `UNDER_REVIEW`
- `ASSIGNED`
- `AWAITING_RESPONSE`
- `RESPONDED`
- `CLOSED`
- `WITHDRAWN`
- `REQUIRES_URGENT_ACTION`

### 11.3 Administrative endpoints

Proposed endpoints:

- `GET /api/v1/admin/escalations`
- `GET /api/v1/admin/escalations/:id`
- `POST /api/v1/admin/escalations/:id/assign`
- `POST /api/v1/admin/escalations/:id/respond`
- `POST /api/v1/admin/escalations/:id/close`

Every endpoint must validate explicit permissions.

### 11.4 Separation of identity mapping

If anonymous submissions require a protected identity mapping for a permitted response workflow, that mapping must be stored separately from the ordinary case record.

The ordinary case interface must not expose the mapping.

The application must not promise absolute anonymity.

### 11.5 Offline submission

If the user submits an escalation while offline, the interface must clearly show that it has not yet reached the church.

A pending local submission must not be presented as received, reviewed, or assigned.

Submission must be idempotent to prevent duplicate cases during synchronisation.

## 12. Attendance APIs

Attendance must use its own API family and data model.

### 12.1 Attendance sessions

Proposed endpoint:

`POST /api/v1/admin/attendance/sessions`

Purpose:

Create a service or event attendance session.

Session fields should include:

```text
session_id
event_type
location_id
scheduled_start
scheduled_end
qr_valid_from
qr_valid_until
minimum_duration_minutes
session_status
created_by
created_at
```

Version 1 is headquarters-only.

### 12.2 Attendance QR token

The QR token must be signed and time-limited.

Its validated payload should identify:

- Attendance session.
- Event type.
- Location.
- Unique nonce.
- Issue time.
- Expiry time.
- Signature and signing key identifier.

The QR must not contain private membership information.

The same valid session QR may be used by multiple members.

Replay protection must be applied per member and session rather than globally consuming the shared QR.

### 12.3 Attendance scan endpoint

Proposed endpoint:

`POST /api/v1/attendance/scans`

Example request:

```json
{
  "submission_id": "uuid",
  "session_id": "uuid",
  "qr_token": "signed-token",
  "scan_type": "CHECK_IN"
}
```

A second scan should use:

`"scan_type": "VERIFICATION"`

The backend must validate the token, authenticated account, verified membership, session state, location policy, nonce, and applicable replay rules.

### 12.4 Recognition policy

Automatic recognition requires two valid server-validated scans.

The second scan must occur at least 60 minutes after the first, based on authoritative server receipt timestamps.

The device clock must not be used to establish the recognised attendance duration.

A second scan submitted too early must not produce recognition.

A missing second scan must not automatically be treated as proof of absence or misconduct.

### 12.5 Offline attendance

Offline scans must be stored in a protected local outbox with unique submission identifiers.

They must remain pending until synchronised.

Automatic recognition is permitted only when both required scans have been successfully validated online.

If trustworthy server timing cannot be established for offline attempts, the record must be sent for human review.

The application must not manufacture server timestamps from the device clock.

### 12.6 Attendance states

Attendance status and synchronisation status must remain separate.

Attendance states:

- `NOT_REGISTERED`
- `CHECKED_IN`
- `PENDING_VERIFICATION`
- `RECOGNISED`
- `INCOMPLETE`
- `REQUIRES_REVIEW`
- `REJECTED`

Synchronisation states:

- `PENDING_SYNC`
- `SYNCING`
- `SYNCED`
- `SYNC_FAILED`
- `REQUIRES_REVIEW`

### 12.7 Attendance endpoints

Proposed endpoints:

- `GET /api/v1/attendance/sessions/active`
- `GET /api/v1/attendance/sessions/:id`
- `POST /api/v1/attendance/scans`
- `GET /api/v1/attendance/me`
- `GET /api/v1/admin/attendance/sessions`
- `GET /api/v1/admin/attendance/sessions/:id/records`
- `POST /api/v1/admin/attendance/records/:id/correction`

Administrative attendance access must be explicitly permission-controlled and audited.

Attendance must not be used as an AI access prerequisite or automatic membership suspension mechanism.

## 13. Reminder and Deep-Link Interfaces

### 13.1 Sunday reminders

Sunday reminders are primarily local Android notifications.

They should not require a backend request every Sunday.

The application should support:

- User-configurable reminder time.
- Local scheduling.
- Permission handling.
- Re-scheduling after device restart.
- Timezone changes.
- Appropriate notification channels.
- Neutral notification text.

The system must not include private pastoral or membership information in notification previews.

### 13.2 AI deep link

The application must support a controlled deep link for opening the AI interface.

A proposed route is:

`perazim://ai`

An HTTPS Android App Link should be considered where appropriate.

The deep link must:

- Open the AI interface.
- Validate the user's current session and access state.
- Preserve normal access checks.
- Avoid granting membership verification.
- Avoid granting permissions.
- Avoid automatically submitting a prompt.
- Avoid registering attendance.

AI access must remain available through ordinary application navigation without requiring a QR scan.

### 13.3 Attendance QR separation

Attendance QR tokens and AI deep links must remain separate formats and workflows.

An AI QR must never be interpreted as an attendance token.

An attendance QR must never grant AI access.

## 14. PostgreSQL Database Design

Codex must inspect the existing PostgreSQL schema before creating new tables.

The following structures describe the required logical data model. They are not instructions to duplicate existing tables under different names.

### 14.1 Membership structures

Required logical records:

**membership_records**

Stores authorised church membership records.

Conceptual fields:

```text
id
membership_reference
full_name
approved_matching_fields
record_status
created_at
updated_at
```

Sensitive matching fields must be protected and minimised.

**membership_verifications**

Stores account verification requests and their current status.

```text
id
user_id
membership_record_id
status
submitted_at
reviewed_at
reviewed_by
decision_reason
```

**membership_verification_history**

Stores important verification state transitions.

```text
id
verification_id
previous_status
new_status
actor_id
reason
created_at
```

A verification history record must not expose unnecessary personal information.

### 14.2 Permission structures

Required logical records:

**permissions**

Defines available permission codes.

```text
id
code
description
active
created_at
```

**user_permissions**

Assigns explicit permissions.

```text
id
user_id
permission_id
granted_by
granted_at
expires_at
revoked_at
revoked_by
```

The implementation may support scope or resource restrictions where required.

Expired or revoked permissions must not remain effective.

### 14.3 Knowledge structures

Required logical records:

**knowledge_documents**

Stores stable document identity and current metadata.

```text
id
title
category
authority_level
current_revision_id
approval_status
audience
offline_eligible
created_by
created_at
updated_at
withdrawn_at
```

**knowledge_revisions**

Stores immutable or appropriately versioned document revisions.

```text
id
document_id
revision_number
content
source_reference
content_hash
created_by
created_at
```

**knowledge_approvals**

Stores approval decisions.

```text
id
document_id
revision_id
decision
approved_by
decision_reason
created_at
```

**knowledge_publications**

Stores publication events.

```text
id
document_id
revision_id
published_by
published_at
publication_status
```

**knowledge_chunks**

Stores retrieval-ready sections.

```text
id
document_id
revision_id
chunk_index
heading
content
source_location
audience
content_hash
```

The actual implementation may use separate chunk tables or another suitable structure.

A document revision must remain traceable to its source and approval.

### 14.4 Offline package structures

Required logical records:

**knowledge_packages**

```text
id
package_version
audience
content_revision
issued_at
expires_at
package_hash
signing_key_id
signature
status
```

**knowledge_package_documents**

```text
id
package_id
document_id
revision_id
content_hash
```

These records must support package reproducibility, verification, withdrawal, and rollback.

### 14.5 AI quota structures

Required logical records:

**ai_usage_periods**

```text
id
user_id
period_type
period_start
period_end
request_limit
token_limit
requests_used
tokens_used
created_at
updated_at
```

**ai_quota_reservations**

```text
id
request_id
user_id
reserved_requests
reserved_tokens
reservation_status
reserved_at
settled_at
```

**ai_request_usage**

```text
id
request_id
user_id
provider
model
input_tokens
output_tokens
estimated_cost
actual_cost
request_status
created_at
settled_at
```

The backend must enforce unique request identifiers and transactional accounting.

The implementation may choose a more efficient ledger design, provided concurrent requests and retries cannot bypass limits or cause duplicate charges.

### 14.6 Pastoral escalation structures

Required logical records:

**pastoral_cases**

```text
id
case_reference
submission_type
category
message_content
status
submitted_at
assigned_to
responded_at
closed_at
```

**pastoral_case_identity_mapping**

```text
id
case_id
protected_user_reference
mapping_status
created_at
```

**pastoral_case_audit**

```text
id
case_id
actor_id
action
previous_status
new_status
created_at
```

Identity mapping must be protected separately from ordinary case information.

The database must not make anonymous cases identifiable to ordinary reviewers through unnecessary joins or default query behaviour.

### 14.7 Attendance structures

Required logical records:

**attendance_locations**

```text
id
location_name
location_code
active
created_at
```

**attendance_sessions**

```text
id
location_id
event_type
scheduled_start
scheduled_end
qr_valid_from
qr_valid_until
minimum_duration_minutes
session_status
created_by
created_at
```

**attendance_qr_tokens**

```text
id
session_id
nonce
issued_at
expires_at
signing_key_id
token_status
```

Private signing keys must not be stored in ordinary database rows.

**attendance_events**

```text
id
session_id
user_id
submission_id
scan_type
server_received_at
validation_status
attendance_status
created_at
```

**attendance_records**

```text
id
session_id
user_id
first_valid_scan_id
second_valid_scan_id
recognition_status
recognised_at
review_status
```

**attendance_corrections**

```text
id
attendance_record_id
previous_state
new_state
corrected_by
reason
created_at
```

The database must enforce appropriate uniqueness rules for member/session/event combinations and idempotent submission identifiers.

### 14.8 Administrative audit structures

Required logical records:

**admin_audit_log**

```text
id
actor_id
action
resource_type
resource_id
previous_state
new_state
reason
request_id
created_at
```

The audit log must be protected against unauthorised modification.

It must not store unnecessary copies of sensitive personal information.

## 15. PostgreSQL Integrity Requirements

Codex must implement suitable database constraints and indexes.

At minimum:

- Primary keys for all entities.
- Foreign keys for related records.
- Unique membership references where appropriate.
- Unique permission codes.
- Unique document revision numbers per document.
- Unique package versions within their defined scope.
- Unique AI request identifiers.
- Unique idempotency identifiers.
- Unique attendance submission identifiers.
- Appropriate uniqueness for attendance records.
- Non-null constraints for required fields.
- Check constraints for valid state values where appropriate.
- Indexes for common access and filtering operations.
- Appropriate indexes for status, ownership, and timestamp queries.

Sensitive information must not be indexed or exposed unnecessarily.

Database-level constraints must support, not replace, backend authorisation.

## 16. Android Room Database Design

The existing Room database must be inspected before migration.

The current application uses Room version 6 according to the previous repository inspection.

Codex must determine the actual migration state at implementation time.

Do not assume that every installation has the same database history.

### 16.1 Required logical local structures

The AI and offline functionality may require:

- Local approved knowledge documents.
- Local knowledge chunks.
- Package manifests.
- Active package metadata.
- Previous package metadata.
- Package update checkpoints.
- Local exact-cache records, where appropriate.
- Temporary local AI session state.
- Attendance outbox.
- Escalation outbox, if the approved workflow permits offline submission.
- Reminder preferences.
- Limited membership and permission snapshots for interface decisions.

### 16.2 Local membership snapshots

Local membership or permission snapshots are informational and must not replace backend authorisation.

The backend remains authoritative.

Sensitive membership information must not be copied unnecessarily into Room.

### 16.3 Local attendance outbox

The attendance outbox must store pending submissions reliably.

Each submission must include:

- Unique submission identifier.
- Session identifier.
- Scan type.
- Signed QR token or necessary validated payload.
- Local creation metadata.
- Synchronisation state.
- Retry information.

The client must not manufacture authoritative server timestamps.

### 16.4 Local escalation outbox

If offline escalation submission is supported, the local outbox must:

- Protect submitted content.
- Record the selected identified or anonymous submission type.
- Preserve the user's consent state.
- Use a unique submission identifier.
- Show clearly that the submission has not yet been received.
- Prevent duplicate submissions during retry.
- Remove local copies according to the approved retention and delivery policy.

The implementation must not promise successful human follow-up before the server confirms receipt.

### 16.5 Database migration

Room migrations must preserve existing data.

Codex must:

1. Inspect all current entities and migrations.
2. Identify every supported upgrade path.
3. Enable and maintain Room schema export.
4. Create explicit incremental migrations.
5. Test migration from every supported prior version.
6. Verify existing application data after migration.
7. Remove destructive migration fallback only when all supported upgrade paths are proven.
8. Preserve existing Bible and application data.
9. Document any unavoidable data transformation.

The current destructive fallback must not remain the normal solution for a failed migration.

Do not change the Room database version or schema without updating migration tests and exported schemas.

## 17. Cloudflare Worker Contract

The Cloudflare Worker must act as a protected Gemini provider gateway.

It must not trust the Android application as the authority for membership or quota decisions.

### 17.1 Authentication

Express-to-Worker requests must use a secure server-to-server authentication mechanism.

The implementation should use short-lived, audience-bound credentials or another properly secured mechanism.

The Worker must reject:

- Requests without valid server authentication.
- Expired credentials.
- Invalid signatures or claims.
- Requests outside the intended audience.
- Unsupported model operations.
- Invalid request formats.

The Android application must not contain a reusable Gemini secret or privileged Worker credential.

### 17.2 Provider request

Express should send only the minimum approved information necessary for the model response.

The Worker should not independently retrieve membership data or private church records.

It should not retain prompts or responses unnecessarily.

### 17.3 Provider response

The Worker should return:

- Provider success or failure.
- Model identifier.
- Response content.
- Provider usage information where available.
- Safe error details.
- Request correlation identifier.

Provider errors must be normalised before reaching the Android application.

### 17.4 Existing implementation

Codex must inspect the current `worker.js` and deployed configuration.

It must document the existing shared-token mechanism, device identifier handling, provider model, quota logic, and any relevant Cloudflare KV usage.

It must not assume the source repository and deployed Worker are identical.

Production deployment and secret rotation require explicit authorisation.

## 18. API Authorisation Rules

Every protected endpoint must enforce authentication and permission checks on the server.

The backend must not rely on:

- Hidden Android buttons.
- Client-supplied role names.
- Client-supplied membership status.
- Client-supplied quota balances.
- Client-supplied attendance status.
- Client-supplied document audience.
- Client-supplied approval decisions.

The backend must derive authority from its own trusted records.

The following operations require explicit authorisation:

| Operation | Required authority |
|---|---|
| Online AI | Verified membership and `ai.online.use` |
| External research | Verified membership and `ai.online.research` |
| Restricted knowledge retrieval | Appropriate knowledge permission |
| Knowledge approval | `knowledge.approve` |
| Knowledge publication | `knowledge.publish` |
| Membership verification | `membership.verify` |
| Membership import | `membership.import` |
| Attendance review | `attendance.view` |
| Attendance correction | `attendance.correct` |
| Pastoral case review | `pastoral.case.review` |
| Permission assignment | `permissions.assign` |
| Audit inspection | `audit.view` |

These permission codes are conceptual identifiers and may be refined during implementation without weakening their meaning.

## 19. Error Codes

The API should use stable error codes.

Examples:

```text
AUTHENTICATION_REQUIRED
AUTHENTICATION_EXPIRED
PERMISSION_DENIED
MEMBERSHIP_VERIFICATION_REQUIRED
MEMBERSHIP_REVIEW_REQUIRED

AI_SCOPE_REJECTED
AI_SENSITIVE_CONTENT_BLOCKED
AI_QUOTA_EXCEEDED
AI_PROVIDER_UNAVAILABLE
AI_REQUEST_TIMEOUT
AI_RESPONSE_VALIDATION_FAILED

KNOWLEDGE_NOT_FOUND
KNOWLEDGE_NOT_APPROVED
KNOWLEDGE_ACCESS_DENIED
KNOWLEDGE_PACKAGE_INVALID
KNOWLEDGE_PACKAGE_EXPIRED
KNOWLEDGE_SIGNATURE_INVALID

ESCALATION_SUBMISSION_PENDING
ESCALATION_SUBMISSION_FAILED
ESCALATION_ALREADY_SUBMITTED

ATTENDANCE_SESSION_EXPIRED
ATTENDANCE_QR_INVALID
ATTENDANCE_REPLAY_REJECTED
ATTENDANCE_SECOND_SCAN_TOO_EARLY
ATTENDANCE_REVIEW_REQUIRED

VALIDATION_ERROR
RATE_LIMIT_EXCEEDED
INTERNAL_SERVER_ERROR
```

The error catalogue should be centrally documented and consistent across Android and Express.

Errors must not reveal sensitive records or internal security implementation details.

## 20. Security and Transaction Requirements

The implementation must apply transactions to operations that change related records or enforce limits.

Examples:

- Membership verification and history creation.
- Permission assignment and audit creation.
- Knowledge approval and publication.
- Quota reservation and settlement.
- Attendance scan acceptance and recognition.
- Attendance correction and audit creation.
- Escalation submission and identity mapping.

A partial failure must not leave records in a misleading or inconsistent state.

The backend must enforce appropriate rate limits and abuse protection.

Sensitive endpoints must use secure transport in production.

The existing cleartext development configuration must be reviewed and restricted before production release.

## 21. API Versioning and Compatibility

The API must support controlled evolution.

Breaking changes should be versioned.

Android and backend versions must be compatible.

The backend must not silently change response structures in a way that breaks existing application versions.

Where old application versions remain supported, the backend should either preserve compatibility or return a controlled, understandable upgrade requirement.

Offline packages must also include compatibility metadata.

## 22. Database Migration and Deployment Requirements

### 22.1 PostgreSQL migrations

Use explicit, ordered, version-controlled migration files.

Every migration must:

- Have a documented purpose.
- Be repeatable only where safely designed.
- Preserve existing data.
- Include appropriate constraints and indexes.
- Be tested against a representative database.
- Have a documented recovery or rollback strategy where possible.

Do not edit an already-applied production migration as if it had never run.

### 22.2 Migration ordering

The recommended logical implementation order is:

1. Membership verification and permission structures.
2. AI request and quota accounting.
3. Knowledge documents and revision history.
4. Knowledge approval and publication.
5. Retrieval and package distribution.
6. Pastoral escalation.
7. Attendance sessions and events.
8. Administrative audit and reporting refinements.

Codex may adjust technical sequencing where dependencies require it, but must explain the reason.

### 22.3 Backup and recovery

Before production migrations:

- Verify that a current backup exists.
- Verify that restoration is possible.
- Document the migration.
- Establish the expected impact.
- Avoid irreversible changes without explicit authorisation.

A backup that has never been tested through restoration must not be described as a verified recovery capability.

## 23. API and Database Testing Requirements

Tests must cover both normal and adversarial behaviour.

### Authentication and membership

- Unauthenticated access rejected.
- Registered but unverified user cannot access online AI.
- Verified member can access only authorised capabilities.
- Ambiguous membership match requires review.
- Revoked verification removes applicable access.
- Client cannot forge membership status.

### Permissions

- Each protected endpoint enforces permission checks.
- Role names alone do not grant access.
- Expired permissions are ineffective.
- Revoked permissions are ineffective.
- Permission changes are audited.

### AI and quota

- Concurrent requests cannot bypass quotas.
- Duplicate request identifiers do not create duplicate charges.
- Provider timeouts settle reservations correctly.
- Sensitive pastoral content is blocked from Gemini.
- Client-supplied context cannot bypass access restrictions.
- AI history is not permanently stored.

### Knowledge

- Drafts are excluded from retrieval.
- Unapproved documents are excluded.
- Withdrawn documents are excluded.
- Audience restrictions are enforced.
- Approval and publication history is preserved.
- Package signatures and hashes are validated.
- Invalid packages cannot replace active valid packages.
- Restricted documents are not included in public packages.

### Pastoral escalation

- Consent is required for ordinary submissions.
- Identified and anonymous paths behave differently.
- Identity mapping is protected.
- Duplicate submissions are prevented.
- Offline pending submissions are not falsely shown as received.
- Unauthorised users cannot retrieve cases.

### Attendance

- Invalid or expired QR tokens are rejected.
- Shared QR use by different authorised members is supported.
- Duplicate scans are rejected appropriately.
- Early second scans cannot produce recognition.
- Device clock manipulation cannot produce false recognition.
- Offline submissions remain pending.
- Human corrections are audited.
- Attendance data is excluded from AI context.

### Database integrity

- Foreign keys behave correctly.
- Unique constraints prevent duplicate records.
- Migrations preserve existing data.
- Failed transactions do not leave partial state.
- Concurrent operations remain consistent.
- Backups can be restored in the test environment.

## 24. Implementation Boundaries

This document defines the intended API and database contracts.

Codex must not:

- Rebuild the application from scratch.
- Replace Express with another production backend.
- Introduce a production Python API.
- Duplicate existing authentication systems.
- Trust Android-supplied permissions or membership status.
- Place Gemini credentials in the APK.
- Store ordinary AI conversation history permanently.
- Mix attendance records with AI records.
- Expose membership registers to AI.
- Publish unapproved knowledge.
- Bypass the approved membership verification workflow.
- Implement arbitrary retention periods.
- Deploy migrations or change production infrastructure without explicit authorisation.

Where repository evidence requires a change to this contract, Codex must document the evidence, explain the impact, and obtain approval for material changes before proceeding.

## 25. Final Implementation Principle

The API and database are the enforcement foundation of the Perazim AI system.

The Android interface may display access options, the AI may explain church teaching, and the knowledge system may provide approved resources. However, identity, permissions, quotas, approval status, attendance recognition, and access to restricted records must be enforced by trusted backend services and database constraints.

The system must remain secure even when a user modifies the Android application, submits a forged request, manipulates device time, repeats a request, or attempts to access another person's information.

**End of Document 16.**
