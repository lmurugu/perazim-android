# 13 — MEMBERSHIP AND ADMINISTRATION SPECIFICATION

## 1. Purpose

This document defines how Perazim Missions Church International will manage application registration, church membership verification, administrative authority, and permission assignments within the AI system.

The objective is to ensure that the AI and related application services recognise only properly verified membership, enforce permissions consistently, and preserve the distinction between technical administration and ecclesiastical authority.

This specification applies to:

- User registration.
- Membership verification.
- Membership status changes.
- Administrative permissions.
- Knowledge-base administration.
- AI configuration.
- Access to restricted information.
- Membership and permission audit records.
- Future delegation of administrative responsibilities.

The system must not infer membership or authority from a user's statements, application activity, existing role labels, or AI-generated conclusions.

## 2. Fundamental Distinctions

The system must maintain three independent concepts.

### 2.1 Application registration

Registration means that a person has created an application account.

A registered account does not automatically establish that the person is a member of Perazim Missions Church International.

Registration provides an application identity, not church membership verification.

### 2.2 Church membership verification

Membership verification establishes that an application account corresponds to an authorised church membership record.

Only the approved verification process can establish this status.

A user must not become a verified member simply because:

- They registered successfully.
- They selected a church role.
- They entered the name of a pastor.
- They claim to be a member.
- They attended a church service.
- They received an application invitation.
- An AI response describes them as a member.

### 2.3 Administrative permission

Administrative permission establishes what an authenticated account is authorised to do within the application.

A verified church member does not automatically receive administrative privileges.

Similarly, a person who has been granted a limited technical permission must not automatically be treated as a verified church member.

Identity, membership, and permissions must be stored and evaluated separately.

## 3. Membership Status Model

The system must support at least the following membership states.

| Status | Meaning |
|---|---|
| REGISTERED | Account exists, but membership has not been verified |
| VERIFICATION_PENDING | A possible membership match is awaiting authorised confirmation |
| VERIFIED_MEMBER | Membership has been confirmed through the approved workflow |
| VERIFICATION_REVIEW | Matching information is ambiguous or requires manual review |
| VERIFICATION_REJECTED | The submitted verification attempt was rejected |
| VERIFICATION_REVOKED | A previous verification has been withdrawn through an authorised process |

The implementation may introduce additional internal states where necessary, but it must preserve these meanings.

A rejected verification attempt does not necessarily mean that the person is not a church member. It means that the particular verification attempt was not accepted.

A revoked verification status must retain its audit history and reason where appropriate.

## 4. Approved Membership Verification Workflow

The following workflow has been approved for the pilot.

### Step 1 — Import the authorised membership list

The pilot administrator imports the authorised church membership list into the protected membership-verification system.

The list is the authoritative source used to verify whether an application account corresponds to an existing membership record.

The import process must:

- Accept only supported and validated file formats.
- Validate required fields.
- Detect duplicate records.
- Identify incomplete or inconsistent entries.
- Preserve source and import information.
- Record the administrator and import timestamp.
- Avoid exposing the complete membership list to ordinary users or AI models.

The system must not silently merge potentially different people because their names appear similar.

### Step 2 — Register the application account

A new user registers through the existing application registration process.

The account is created with status:

`REGISTERED`

Registration must not automatically change the account to `VERIFIED_MEMBER`.

The existing application role value `MEMBER`, where present, must not be treated as proof of membership verification.

### Step 3 — Attempt membership matching

The system compares the account's authorised identifying information against the imported membership records.

The matching process may identify:

- One clear possible match.
- Multiple possible matches.
- No possible match.
- Incomplete or inconsistent information.

Matching must use appropriate identifying information and must not rely solely on a common name.

The system should use a combination of available, authorised identifying fields, subject to data minimisation and the actual quality of the church membership register.

### Step 4 — Present an unambiguous match for confirmation

Where exactly one sufficiently reliable match exists, the system may present the possible match for confirmation by the authorised pilot administrator.

The system must not automatically grant verified status merely because a matching algorithm found a likely record.

The administrator must be able to inspect the relevant information and confirm whether the account corresponds to the membership record.

### Step 5 — Manual review of ambiguous matches

Where multiple possible matches exist, information is incomplete, or the matching result is uncertain, the system must require manual review.

The system must not choose a record automatically in ambiguous cases.

The administrator may request clarification through an appropriate process before confirming the account.

### Step 6 — Authorised confirmation

Only an authorised administrator may confirm the match during the pilot.

After confirmation, the account status becomes:

`VERIFIED_MEMBER`

The confirmation must be recorded with:

- Application account identifier.
- Membership record identifier.
- Previous membership status.
- New membership status.
- Administrator identifier.
- Confirmation timestamp.
- Verification method.
- Relevant audit information.

### Step 7 — Corrections and revocation

Where an error is discovered, the authorised administrator must be able to correct or revoke the verification.

A correction must not erase the original event from the audit history.

The system must preserve the previous state, new state, administrator, timestamp, and reason where appropriate.

Revocation must remove access that depends on verified membership according to the relevant access-control rules.

It must not automatically delete the user's application account or unrelated personal records.

## 5. Membership Data Protection

The membership register is sensitive institutional information.

It must be protected independently of ordinary AI knowledge and application content.

The following requirements apply:

- The complete membership register must not be exposed to the AI model.
- The register must not be included in general offline knowledge packages.
- Ordinary verified members must not be able to browse the full register.
- Membership matching must be performed through controlled backend functionality.
- Membership records must not be included in Gemini prompts.
- Membership records must not enter shared AI caches.
- Membership data must not be exposed through ordinary application logs.
- Access to membership records must be audited.
- Data must be minimised to what is necessary for verification and administration.

The system should use an internal membership record identifier rather than repeatedly copying personal information across services.

A user must not be able to retrieve another person's membership details by changing an account ID, membership ID, or request parameter.

## 6. Permission Architecture

Permissions must be assigned explicitly and enforced by the backend.

A person's title, ministry position, or membership status must not automatically grant access to unrelated administrative functions.

The system should support a permission model in which access is granted according to:

- Authenticated identity.
- Verified membership where required.
- Explicit permission assignment.
- Resource ownership or scope where applicable.
- Permission validity and revocation status.

The implementation may use role-based access control, permission-based access control, or a combination. However, the actual permission granted must be explicit and enforceable.

A broad role must not silently confer unrestricted access to all sensitive records.

## 7. Application Access Categories

The following categories provide a high-level access structure.

They are not a substitute for individual permission checks.

### 7.1 Public

A public user has not necessarily registered or authenticated.

Permitted capabilities may include:

- Accessing public church information.
- Using approved public offline AI resources.
- Reading public teaching materials.
- Accessing publicly available ministry information.

A public user must not access restricted membership, administrative, pastoral, attendance, or financial records.

### 7.2 Registered

A registered user has an application account but has not completed membership verification.

Permitted capabilities may include:

- Managing their own application account.
- Using ordinary application features allowed to registered users.
- Accessing approved public knowledge.
- Using approved public offline AI resources.

A registered user must not receive online AI access where verified membership is required.

### 7.3 Verified Member

A verified member has completed the authorised membership verification workflow.

Permitted capabilities include:

- Accessing member-level application resources.
- Using online AI subject to applicable quotas and restrictions.
- Accessing member-specific features where separately authorised.

Verified membership does not automatically grant administrative access to confidential records.

### 7.4 Ministry Worker

A ministry worker is a person assigned one or more specific ministry-related application permissions.

The title does not automatically grant access to all ministry records.

Examples of possible permissions include:

- Managing an assigned ministry programme.
- Viewing authorised ministry resources.
- Managing assigned ministry activities.
- Accessing specifically authorised ministry information.

### 7.5 Minister or Clergy

A minister or clergy account may receive specific pastoral or ministry permissions where authorised.

The title alone must not grant unrestricted access to all pastoral cases, membership records, financial information, attendance administration, or AI configuration.

Pastoral access must be explicitly assigned and appropriately limited.

### 7.6 Senior Leadership

Senior leadership accounts may receive specific institutional oversight permissions according to the church's authorised governance arrangements.

The application must not invent ecclesiastical powers or automatically convert a leadership title into unrestricted technical access.

### 7.7 Administrator

An administrator is a person assigned technical application administration permissions.

Administrative access must be permission-specific, authenticated, audited, and revocable.

The category must not be treated as a blanket authorisation to access every confidential record.

## 8. Pilot Administrator Authority

The pilot administrator arrangement has been explicitly approved.

During the pilot, the user is the sole authorised technical administrator of the Perazim AI system.

The pilot administrator may:

- Import and update authorised membership-verification data.
- Confirm membership matches.
- Correct membership-verification errors.
- Revoke incorrect verification.
- Approve knowledge-base content for technical publication.
- Correct, replace, or withdraw knowledge-base content.
- Authorise publication of offline knowledge packages.
- Manage AI configuration and approved response instructions.
- Manage technical permissions and administrative access.
- Review authorised system audit records.
- Manage pilot AI settings and operational controls.

These powers must be implemented as explicit technical permissions.

The system must not assume that every person described as a pastor, bishop, minister, leader, or administrator has these permissions.

### 8.1 Pilot authority is not ecclesiastical authority

The pilot administrator's technical approval of a knowledge item does not automatically make that item an official church doctrinal position.

Three different actions must remain distinct:

1. Approving a source for inclusion in the knowledge system.
2. Approving an institutional policy or teaching position through the appropriate church authority.
3. Formally changing doctrine through the authorised church governance process.

The AI system must preserve these distinctions.

Formal doctrinal changes must follow the church's authorised governance process.

Technical administrators must not be able to bypass this distinction by changing a knowledge document's metadata.

## 9. Administrative Permission Catalogue

The following is the proposed initial permission catalogue.

Codex must inspect the existing application and adapt naming conventions without weakening the intended boundaries.

| Permission | Purpose |
|---|---|
| `membership.import` | Import an authorised membership register |
| `membership.view` | View authorised membership verification information |
| `membership.match.confirm` | Confirm an unambiguous membership match |
| `membership.review` | Review ambiguous membership matches |
| `membership.correct` | Correct a membership verification record |
| `membership.revoke` | Revoke a membership verification |
| `knowledge.source.create` | Register a knowledge source |
| `knowledge.source.update` | Update source metadata |
| `knowledge.document.review` | Review submitted knowledge documents |
| `knowledge.document.approve` | Approve knowledge content for technical publication |
| `knowledge.document.withdraw` | Withdraw published knowledge content |
| `knowledge.package.publish` | Authorise offline package publication |
| `ai.config.view` | View AI configuration |
| `ai.config.update` | Update approved AI configuration |
| `ai.access.manage` | Manage AI access permissions |
| `ai.usage.view` | View authorised AI usage records |
| `ai.system.audit.view` | View authorised technical audit records |
| `escalation.view` | View authorised pastoral escalation cases |
| `escalation.assign` | Assign a case to an authorised reviewer |
| `escalation.respond` | Record an authorised human response |
| `attendance.session.manage` | Create and manage attendance sessions |
| `attendance.records.view` | View authorised attendance records |
| `attendance.correct` | Correct attendance records |
| `admin.permissions.manage` | Assign or revoke technical permissions |

These permissions must not be assigned automatically merely because a user belongs to a category.

During the pilot, only the authorised pilot administrator may exercise the relevant administrative permissions.

## 10. Permission Enforcement

The backend must enforce permissions on every protected operation.

The Android interface may hide unavailable functions for usability, but this is not sufficient enforcement.

The backend must reject unauthorised requests even when they are manually constructed outside the application.

Permission checks must include:

- Authenticated account.
- Current permission assignment.
- Permission validity.
- Required membership status.
- Resource ownership or scope.
- Applicable restrictions.
- Revocation state.

The system must not rely on an old permission snapshot as proof of current authority for online operations.

For offline operation, locally cached permission information may support interface behaviour and bounded access to approved offline resources, but it must be treated as a potentially stale snapshot.

The application must not claim that an offline permission snapshot guarantees that a permission has not been revoked on the server.

## 11. Permission Assignment and Revocation

Permission assignment must be an explicit administrative action.

The system must record:

- User receiving the permission.
- Permission identifier.
- Assigning administrator.
- Assignment timestamp.
- Effective date where applicable.
- Expiry date where applicable.
- Reason or administrative reference where appropriate.
- Revocation status.
- Revoking administrator and timestamp.

Where a permission is temporary, the backend must enforce its expiry.

Revocation must take effect for subsequent online requests once the authoritative backend records the change.

The system must not rely on a user logging out or reinstalling the application to enforce revocation.

## 12. Separation of Administrative Domains

Administrative access must be separated by information domain.

For example:

- Membership administration must not automatically grant financial access.
- Attendance administration must not automatically grant access to pastoral cases.
- Knowledge administration must not automatically grant access to private member messages.
- AI configuration must not automatically grant access to the membership register.
- General application administration must not automatically grant unrestricted access to every record.

The implementation should support the principle of least privilege: each administrator receives only the permissions necessary for their assigned responsibilities.

For the pilot, the sole authorised technical administrator may hold the permissions required to manage the pilot system, but those permissions must still be explicitly defined and audited.

## 13. Administrative Audit Records

Sensitive administrative actions must generate audit records.

At minimum, record:

- Membership imports.
- Membership confirmation.
- Membership correction.
- Membership revocation.
- Permission assignment.
- Permission revocation.
- Knowledge approval.
- Knowledge replacement.
- Knowledge withdrawal.
- Offline package publication.
- AI configuration changes.
- Administrative access to protected records where required.

Each record should include:

- Unique event identifier.
- Actor account identifier.
- Action.
- Affected resource identifier.
- Timestamp.
- Previous state where relevant.
- New state where relevant.
- Outcome.
- Reason or reference where appropriate.

Audit records must not contain unnecessary sensitive personal content.

Ordinary users must not be able to modify or delete administrative audit history.

## 14. AI Access Rules

The AI must receive only the access and information permitted for the current user.

The system must apply the following rules:

1. Public and registered users may access approved public offline AI resources.
2. Registered but unverified users must not receive online AI access where verified membership is required.
3. Verified members may access online AI subject to quotas, scope, safety, and applicable permissions.
4. Ministry, clergy, leadership, and administrator categories do not automatically expose confidential information.
5. Restricted knowledge must be filtered by assigned permission and intended audience.
6. The AI must not disclose membership records merely because a user asks for them.
7. The AI must not disclose private attendance information without explicit authorisation.
8. The AI must not disclose confidential pastoral escalation records without explicit authorisation.
9. The AI must not infer that a user has administrative permission from their statements.
10. The AI must not grant, revoke, or modify permissions through ordinary conversation.

Where a user asks for information they are not authorised to access, the system must refuse without revealing whether a particular confidential record exists.

## 15. Account Changes and Membership Lifecycle

The implementation must consider membership status changes, account changes, and verification corrections.

Examples include:

- A member changes their phone number.
- A member changes their display name.
- A user creates a duplicate account.
- A member loses access to an old account.
- A membership record is corrected.
- A membership verification is revoked.
- A member dies or leaves the church.
- A user is incorrectly matched to another person's record.

These events must be handled through authorised administrative workflows.

The AI must not independently determine that a person has ceased to be a church member.

The system must distinguish account inactivity, application inactivity, attendance inactivity, and actual church membership status.

Absence from application activity or church attendance must not automatically revoke verified membership.

Any formal membership-status change must follow the appropriate church process and authorised administrative action.

## 16. Administrative Interface Requirements

The administrative interface should provide clear, controlled screens for:

- Membership register import.
- Membership matching.
- Ambiguous match review.
- Membership verification confirmation.
- Membership correction and revocation.
- Permission assignment and revocation.
- Knowledge approval and publication.
- AI configuration.
- Authorised audit review.

Each administrative action must display enough information to prevent accidental changes.

High-impact actions such as membership revocation, permission changes, knowledge withdrawal, and package publication should require an explicit confirmation step.

The interface must not expose unrestricted bulk data merely because the administrator has access to an individual record.

## 17. API and Database Requirements

The final API and database design will be specified in Document 16.

At a minimum, the implementation must support:

- Application accounts.
- Membership records.
- Membership verification history.
- Membership matching and review states.
- Permission definitions.
- Permission assignments.
- Permission revocation and expiry.
- Administrative audit records.

The database must enforce appropriate uniqueness, foreign-key, and integrity constraints.

The API must provide controlled operations for:

- Membership import.
- Membership matching.
- Verification confirmation.
- Verification correction.
- Verification revocation.
- Permission assignment.
- Permission revocation.
- Permission inspection.
- Administrative audit retrieval.

All protected operations must require backend authentication and appropriate permissions.

The API must not expose the entire membership register to the Android client.

## 18. Testing Requirements

The implementation must be tested against at least the following scenarios.

| Scenario | Expected behaviour |
|---|---|
| New user registers | Account remains REGISTERED |
| Unique membership match found | Match awaits authorised confirmation |
| Ambiguous match found | Manual review required |
| No match found | Account remains unverified |
| Pilot administrator confirms match | Account becomes VERIFIED_MEMBER |
| Unauthorised user attempts confirmation | Request rejected |
| Verified status is revoked | Restricted access removed |
| User changes role in client request | Backend ignores unauthorised change |
| User changes membership status in client request | Backend ignores unauthorised change |
| Verified member has no admin permission | Administrative endpoint rejects request |
| Ministry worker requests unrelated financial records | Access denied |
| Administrator accesses membership record | Access checked and audited |
| Permission expires | Protected operation rejected |
| Permission is revoked | Subsequent online request rejected |
| Duplicate membership record imported | Duplicate detected and flagged |
| Account changes identifying information | Verification is not silently transferred to another person |
| Offline permission snapshot is stale | System does not claim it is current server authority |

Additional tests must verify that the AI cannot retrieve restricted membership or administrative information through prompts, cache entries, or modified client context.

## 19. Future Delegation

The pilot begins with one authorised technical administrator.

Future delegation may be introduced when the church and project owner approve an appropriate administrative model.

Delegation must not be automatic.

Before introducing additional administrators, the system should support:

- Named administrator accounts.
- Explicit permission assignment.
- Limited administrative domains.
- Effective and expiry dates where appropriate.
- Revocation.
- Audit history.
- Separation of duties for particularly sensitive operations.

Any future delegation of ecclesiastical authority must follow church governance and must not be inferred from a technical permission assignment.

## 20. Implementation Boundaries

This document defines the membership and administration requirements.

It does not authorise Codex to:

- Change official church membership policy.
- Invent a membership register.
- Automatically verify users.
- Grant administrative access based on titles.
- Create hidden administrative accounts.
- Treat technical administration as ecclesiastical authority.
- Change doctrine through knowledge metadata.
- Expose the membership register to AI models.
- Modify live membership records without authorisation.

Codex must inspect the existing authentication and membership implementation and reuse appropriate components where possible.

If the current system cannot support an approved requirement safely, Codex must explain the limitation and propose a controlled implementation.

## 21. Final Operating Principle

The Perazim AI system must know the difference between a person who has registered, a person whose church membership has been verified, and a person who has been authorised to perform a particular administrative action.

These distinctions protect the church, its members, its leadership, and the integrity of the AI system.

**End of Document 13.**
