# DATA-01 Addendum 2 — implementation designs

**Status: design reference; implementation is tracked separately.** These notes do not create institutional policy, approve a purpose or retention period, publish a document, authorize real-data processing, or authorize a database migration. The supplied policy, notice, DPIA and retention documents remain drafts. See `DATA-01_IMPLEMENTATION_REPORT.md` for source changes and test evidence.

## Scope and constraints

This document covers publication records, existing-account re-acknowledgement, optional consent, retention and data-subject requests, ambiguous membership imports, legacy account identity reconciliation, and phone changes. Implementation work has since been made in files marked as owned in the hand-off; this document remains the design reference and does not itself establish approval or operational authority.

The real `member_contacts.csv` and notebook photographs are out of scope. They must not be opened, printed, copied, parsed, used for tests, or moved. The avatar-feature worktree is also out of scope. Implementation fixtures must be invented synthetic records and any database testing must use a uniquely named disposable database.

The design preserves these existing project boundaries:

- A phone number identifies or helps secure an app account; it does not prove church membership.
- Membership verification is an administrator decision, never an automatic match result.
- Applicant-facing responses remain neutral and reveal no register candidates or conflicts.
- Notice acknowledgement is distinct from optional consent.
- No optional purpose, lawful basis, publication approval, approver, effective date, or retention duration is invented here.
- No full membership register may enter Android storage, general sync, AI prompts, retrieval sources, logs, or public API responses.

## 1. Public notice and policy publication

### Proposed records

Keep the app notice and the full public policy as separately versioned documents, with an explicit relationship between the app notice and the policy version it describes.

`privacy_document_versions` (or separate notice and policy tables if that fits the existing schema better) should record:

- Stable document kind and version identifier.
- Immutable content hash and storage path for the exact reviewed content.
- Lifecycle state: `DRAFT`, `PUBLISHED`, `WITHDRAWN`, or `SUPERSEDED`.
- Effective timestamp, where approved and applicable.
- The linked policy version for an app notice.
- Creation timestamp and non-sensitive change note.

Store publication decisions as append-only `privacy_publication_events`: document version, event (`SUBMITTED`, `APPROVED`, `PUBLISHED`, `WITHDRAWN`, `SUPERSEDED`), actor ID/role, timestamp, reason, and any institutional decision reference that PMCI actually supplies. Approval evidence must not be fabricated. If formal approval is recorded outside the application, store only a real supplied reference and the responsible authorized actor; otherwise publication stays unavailable.

The publication operation should be transactional: validate that content hash matches immutable content, confirm the actor has the publication permission, record the publication event, set the new effective publication, and supersede the prior effective version. Historical events and content hashes remain queryable to authorized administrators. There may be at most one currently effective published version per document kind. A withdrawal immediately removes the document from public delivery and registration acknowledgement eligibility while preserving history.

The public endpoint serves only the effective published version whose hash verifies. Draft and withdrawn content receive a generic unavailable response. An administrative preview, if later needed, must use a separately authenticated permission and must clearly label draft content; it is not part of public routes.

### Open decisions and implementation notes

The current tree has a privacy-notice version table but no public-policy publication table. Until a reviewed migration and publication authority exist, the public-policy route should fail closed. PMCI must define who may approve and publish, which notice changes are material, how withdrawal is communicated, and the actual policy URL/contact before any production publication. A schema's existence alone is not approval.

## 2. Existing-account re-acknowledgement

### Proposed enforcement

The backend, not the Android client, selects the currently effective notice version. A notice version carries a `requires_acknowledgement`/material-change decision set only through the approved publication workflow. This avoids treating a typo correction as a re-acknowledgement event while still requiring acknowledgement when PMCI decides a change is material.

For authenticated accounts, a privacy gate checks the account's acknowledgement against the effective version and content hash before a route processes registered personal data. It returns a stable response such as HTTP 428 with code `PRIVACY_NOTICE_ACK_REQUIRED` and the server-selected version/hash. It must not expose other account or membership information. Public, guest, offline, sign-out, notice-view, and rights-request entry points remain reachable so a person can read the notice, leave, or submit a privacy request.

Present the new notice in Android at sign-in or the first protected action after publication. Keep the current account signed in only as needed to submit acknowledgement; do not grant access to gated features until the backend confirms the record. Provide a sign-out path and keep non-account public/offline resources available.

An acknowledgement endpoint should require authentication and bind the record to the server's current effective version and hash. Prefer a short-lived server-issued presentation receipt tied to the account, version, hash, and presented timestamp; acknowledgement consumes it once. Store presentation and acknowledgement times server-side and preserve the existing acknowledgement history. A client-supplied version or timestamp alone is never authoritative. The initial registration path needs a separate synthetic test case because no account exists yet; it must validate against the same effective publication and record acknowledgement in the account-creation transaction.

Acknowledgement creates no optional-consent record. Existing accounts are never backfilled as having accepted a later notice. A version change makes the previous acknowledgement stale by comparison; it does not update or delete it.

### Synthetic acceptance cases

- Existing account acknowledged to version A continues while A is current.
- Publishing material version B causes protected account routes to return the re-ack response.
- The same account can view B, acknowledge B, then access protected routes.
- A second account created before B receives the same gate; a new registration after B records B directly.
- Wrong account, stale receipt, wrong hash, expired receipt, withdrawn version and replayed receipt fail without creating acknowledgement evidence.
- Public/offline content, sign-out and rights-request routes remain reachable while acknowledgement is pending.

## 3. Purpose-specific optional consent

No consent purpose should be activated until PMCI supplies the actual optional activity, purpose-specific wording, lawful-basis decision, necessity assessment, affected feature/data, retention linkage, recipient details, and withdrawal behavior.

When a purpose is approved, maintain a versioned `consent_purposes` catalogue and an append-only `consent_events` history. Each event should identify the account, purpose key and wording version/hash, decision (`GRANTED`, `DECLINED`, `WITHDRAWN`), timestamp, collection channel, and any relevant decision reference. A current-state view may be derived from the event history; updates must not erase earlier choices.

The interface presents an unselected choice for that purpose and permits decline without blocking unrelated features. The relevant processing service checks the current grant at the point of use and stops consent-based processing after withdrawal. A UI-only withdrawal is insufficient. Consent remains separate from membership status, account roles, notice acknowledgement, and administrator authority. No general notice acceptance may authorize external AI processing or transfer member information to an AI provider.

Until the institutional inputs exist, keep the purpose catalogue empty and do not collect a placeholder consent.

## 4. Retention, disposal and data-subject requests

### Retention design

Represent retention policy by data category and approved purpose, not by one global account age. A future `retention_rules` record should include category, purpose, trigger definition, approved duration (nullable while unapproved), review date, disposal method, policy version, approval evidence supplied by PMCI, status (`DRAFT`, `APPROVED`, `SUSPENDED`, `RETIRED`), and created/changed metadata. A draft, missing duration, or expired review date is never permission to delete.

For actual record decisions, keep `retention_reviews`, `legal_holds`, and `disposal_events` separately. Record the subject/data scope, triggering date, rule version, review result, reviewer, hold state, disposal method, outcome, and completion time. Preserve security/accountability evidence according to its own approved category rule. Where an audit record must be retained, use a restricted pseudonymous subject reference when feasible; do not silently mutate a record's decision history.

A dry run reads synthetic fixtures only and returns counts and pseudonymous synthetic IDs grouped by rule, trigger, age band, hold status, and proposed outcome. It performs no writes and no deletion. An authorized administrator reviews a dry-run report and creates an explicit proposal; a separate authorized confirmation executes an approved disposal. Use dual review for bulk actions. Do not enable a scheduled deletion job until each applicable rule has documented approval and the process has been verified on disposable data.

### Data-subject request design

Maintain a restricted `data_subject_requests` case record with request type (`ACCESS`, `CORRECTION`, `ERASURE`, and later any separately approved right), submitted time/channel, verified subject reference, verification method/result, scope, assigned reviewer, status, decision, exception/legal-hold reference, response time, and audit events. Do not put request narratives, personal values, or documents in ordinary logs.

Verify identity proportionately without disclosing another person's record. Keep the requester-facing status neutral while identity or eligibility review is pending. An access export is scoped to the verified requester and excludes other people's details and administrator-only match notes. A correction creates an audited revision; it does not rewrite verification history. Erasure requests go through eligibility, statutory exception, purpose, legal-hold, and integrity review. If an operational membership decision or required security evidence must remain, record the reviewed reason and apply the approved restriction, redaction, or pseudonymisation method rather than silently deleting evidence. Preserve the request case and response audit under its own approved retention rule.

PMCI and counsel must determine applicable rights, exceptions, response periods, identity checks, contact point, and retention rules. No timing or deletion period is specified by this design.

### Synthetic acceptance cases

Dry run is read-only; unapproved rules identify no executable disposal; legal hold prevents eligibility; expired rules require review rather than deletion; a reviewed eligible synthetic record produces an auditable proposal; execute requires explicit authorized confirmation. Synthetic access returns only the synthetic subject's permitted scope; correction records a revision; erasure with a hold is deferred with a reason; unauthorized and mismatched requests reveal no records.

## 5. Membership import ambiguity

### Staging and operational records

Separate `membership_import_batches` and `membership_import_rows` from approved operational membership records. Staged rows may have a name and no phone, or an invalid/missing optional phone; they remain incomplete and cannot verify an account. Do not fabricate a phone number or convert an incomplete row into an operational verified member. Preserve supplied source values in restricted staging storage only after a future lawful, authorized import; the design itself uses synthetic fixtures.

Use explicit row states and separate report counts for `ACCEPTED_FOR_REVIEW`, `INCOMPLETE`, `DUPLICATE_REFERENCE`, `POSSIBLE_DUPLICATE_PERSON`, `SHARED_PHONE`, `IDENTITY_CONFLICT`, and `REJECTED`. Distinguish:

- Same source reference repeated in one batch.
- Same source reference already imported in a prior approved batch.
- Duplicate normalized identity candidate.
- Multiple possible person candidates.
- A contact value shared by multiple staged or operational records.
- Missing or malformed optional contact values.

Phones are non-unique contact attributes. Never merge or reject people solely because numbers match. A duplicate or shared phone creates a private review flag. Candidate/conflict details are available only to an authorized administrator in a protected review view; normal logs and applicant responses contain no names, numbers, match booleans, or counts that reveal register content. An applicant sees only a neutral claim-submitted/pending response.

### Idempotency, revisions and corrections

Give each approved import batch a stable source/batch identifier. Derive an idempotency fingerprint using a keyed digest over normalized source reference and row content; do not store personal values in an ordinary log or expose fingerprints to applicants. Re-running an identical approved synthetic batch returns the existing batch/row results and creates no second membership. Changed data creates an explicit revision/correction candidate, never a silent replacement. Preserve prior values and reviewer, timestamp, reason, and decision in protected revision history.

Only an authorized reviewer can promote a staged row to an operational membership record. Import acceptance is not a verification decision. Membership claims continue through a separate administrator decision flow.

### Synthetic acceptance cases

Test name-only rows, no phone, malformed optional phone, shared phone across distinct people, duplicate reference within/across batches, likely duplicate identity, conflicting details, exact batch replay, changed replay, unauthorized detail access, neutral applicant responses, and admin-approved promotion. Assert that no case auto-links or verifies an account and that no conflict details enter logs or client responses.

## 6. Legacy account identity reconciliation

Keep mutable account display name separate from a protected registration identity snapshot. A legacy account without a trustworthy snapshot remains `UNRECONCILED` and unverified. Do not populate it from the current editable profile name.

Create a `membership_identity_reconciliation_requests` workflow with proposed identity fields, source (`USER_SUBMITTED` or an explicitly defined assisted process), submission timestamp, request status, reviewer decision, reason code, linked membership-record reference (administrator-only), and append-only decision events. Proposed values are evidence for review, not proof. The account owner cannot write the trusted snapshot or membership link.

Require an authenticated request and appropriate step-up check; show a neutral submitted/pending state. The pilot administrator compares the request with an authorized operational membership record and records approve, reject, or request-correction. Approval writes a new protected identity snapshot and may then separately approve a membership link, with distinct authorization and audit events. Do not reveal candidate counts or whether a supplied name/phone matched. Preserve the prior account/profile state and each submitted proposal. A correction creates a new request revision rather than overwriting history.

Restrict approval to the named pilot-administrator permission once that authorization is defined and provisioned. Require recent step-up authentication, deny self-approval where the same actor submitted/owns the request, and audit all reads and decisions of sensitive candidate data. An approval must reference the exact reviewed operational record and current record revision so a stale review cannot approve changed data.

### Synthetic acceptance cases

Test legacy account without snapshot, editable profile-name changes, no candidate, one candidate, multiple candidates, shared contact, conflicting identity, stale record revision, request correction/rejection, successful human approval, unauthorized member modification, and applicant-neutral responses. A user-submitted name/phone alone must never create a trusted snapshot or verified link.

## 7. Phone-number change and verified-link protection

Keep phone change disabled until a complete reviewed lifecycle is implemented. When authorized, it should be a change request, not a direct profile update:

1. Authenticate the existing account and perform step-up against the current account credential or another approved factor.
2. Collect the proposed new number separately; verify current access to the proposed number using an approved mechanism. This proves access to that number at that time only.
3. Enforce uniqueness only for account-login constraints if the product requires it, while returning a generic response that does not disclose a matching membership record or another account's identity.
4. Preserve the prior account number in protected revision history and notify the old and new channels where approved.
5. Put any membership link that depends on the old phone into a review/hold state according to the approved rule. Reassess the membership link through an authorized human review; neither possession of the new number nor an OTP restores membership verification.
6. Record request, verification, decision, affected link state, actor, time, and reason. Revoke/reissue sessions only under a separately defined account-security policy.

If the complete lifecycle is not available, the UI and API must report phone change unavailable and leave the existing value/link unchanged. Tests must prove no self-service mutation, no candidate disclosure, correct hold/review behavior, and no membership verification from OTP alone.

## 8. Implementation dependencies and test gates

Before implementation, reconcile the active Phase B files and schema with the audit owner. The designs do not prescribe replacing or duplicating existing tables, routes, middleware, audit trails, or Android screens; inspect the handed-off versions first. Keep `server/src/phase-b/security.js`, the isolated audit connection guard/runner, primary rate-limit implementation/tests, shared auth/membership/admin routes, `server/src/phase-b/`, and migrations unchanged until Levi reports each file `DONE`.

After hand-off, map each approved design to existing schema and route conventions, write migrations only after the audit file boundary is clear, and run tests using synthetic fixtures and a uniquely named disposable database. The minimum regression set includes publication lifecycle, registration and existing-account re-acknowledgement, purpose-specific consent and withdrawal, retention dry run and legal holds, DSAR scope and exception handling, importer ambiguity/idempotency, claim review, legacy reconciliation permissions, phone-change holds, error-log redaction, APK asset exclusion, and SEC-01/SEC-02/ADM-01 suites. Do not weaken assertions to pass.

## 9. File hand-off status

Levi's hand-off marked `auth.js`, `membership.js`, `adminMembership.js`, and selected Phase B files **DONE**. The audit session still owns `server/test/phase_b_persistence.test.js`; migrations 0006 and 0007 are fixed and were not edited. Implementation reporting records each changed file and migration. The CSV and avatar-feature worktree remain out of scope.
