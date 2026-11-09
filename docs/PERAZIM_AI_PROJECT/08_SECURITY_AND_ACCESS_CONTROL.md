# 08 — SECURITY AND ACCESS CONTROL

## 1. Purpose

This document defines the security and access-control requirements for the Perazim Missions Church International AI system.

The purpose is to protect:

- Member accounts and identities.
- Church membership records.
- Approved and unapproved knowledge.
- Confidential institutional information.
- AI services and provider credentials.
- Administrative functions.
- Pastoral escalation records.
- Attendance records.
- Usage quotas and financial resources.
- Local application data and offline knowledge packages.

Security must be enforced throughout the system rather than relying on the Android interface, user instructions or AI prompts.

The Android application, Express backend, PostgreSQL database, Cloudflare Worker, Gemini integration and offline AI components must operate under one coherent security policy.

This document must be read alongside:

- `00_PROJECT_OVERVIEW.md`
- `01_TECHNICAL_ARCHITECTURE.md`
- `02_AI_IDENTITY_AND_BEHAVIOUR.md`
- `03_THEOLOGICAL_FOUNDATION.md`
- `04_KNOWLEDGE_ARCHITECTURE.md`
- `05_OFFLINE_AI_SPECIFICATION.md`
- `06_ONLINE_AI_SPECIFICATION.md`
- `07_RESPONSE_ROUTING_AND_CACHING.md`

## 2. Foundational Security Principles

### 2.1 Server-side enforcement

The backend is the authoritative enforcement point for account identity, verified membership, permissions, online AI access, administrative operations and protected institutional records.

The Android application may hide unavailable functions and display permissions, but hiding a button is not a security control.

Every protected backend operation must independently verify the user's identity and authorisation.

### 2.2 Least privilege

Every account must receive only the access necessary for its authorised functions.

A user must not gain access to confidential records merely because they are:

- A church member.
- A ministry worker.
- A minister.
- A senior leader.
- An administrator of a different application function.
- A person using the church's Android application.

Access must be based on explicitly assigned permissions.

### 2.3 Separation of identity, membership and authority

The system must distinguish three separate concepts:

**Identity:** Who is the person using the application?

**Membership:** Has the person's membership in Perazim been verified through the authorised process?

**Permission:** Which specific actions and information is the account authorised to access?

These concepts must never be treated as interchangeable.

Successful registration does not establish church membership. Verified membership does not automatically grant administrative authority. A leadership title does not automatically grant access to every confidential system.

### 2.4 Defence in depth

The system must use multiple complementary controls.

These should include authentication, authorisation, secure communication, input validation, database access restrictions, secret management, rate limiting, audit logging, encryption where appropriate and secure offline package handling.

No single control should be assumed sufficient to protect the entire system.

### 2.5 No AI-based security authority

Qwen and Gemini must never decide whether a user is authorised to access a protected resource.

AI-generated statements such as "this user is an administrator" or "the user is a verified member" must have no security effect.

The backend must make access decisions using trusted records and enforceable rules.

## 3. User Identity and Authentication

### 3.1 Existing authentication

Codex must inspect the existing authentication implementation before changing it.

The existing application includes account and authentication infrastructure, including JWT-related functionality and PIN hashing.

These components should be reused where they meet the security requirements. They must not be replaced merely to introduce a different authentication framework.

### 3.2 Authentication requirements

The authentication system must:

- Identify the account making each protected request.
- Verify the authenticity and validity of authentication credentials.
- Enforce token expiry.
- Support logout and appropriate session revocation.
- Prevent unauthorised token reuse.
- Protect authentication credentials during transmission and storage.
- Apply rate limits to repeated authentication failures.
- Avoid exposing whether confidential account or membership records exist unnecessarily.

The implementation must document the authentication mechanism, token format, signing or verification method, expiry policy, refresh process and revocation behaviour.

### 3.3 JWT security

If JWT remains the authentication mechanism, the backend must validate:

- Signature.
- Accepted signing algorithm.
- Issuer, where configured.
- Intended audience, where configured.
- Expiry.
- Not-before time, where used.
- Subject/account identity.
- Any required token type or purpose.

The backend must not accept a token merely because it is syntactically valid.

The implementation must not permit insecure algorithm selection or unsigned tokens.

Signing keys must remain server-side and must support secure rotation.

### 3.4 Session revocation

The system must support invalidating compromised or withdrawn sessions.

A valid-looking token must not provide indefinite access after an account has been disabled, membership verification revoked or relevant permission removed.

Codex must determine whether the existing implementation uses token revocation, token versioning, short-lived access tokens, refresh-token rotation or another suitable mechanism.

The selected approach must be documented and tested.

### 3.5 Passwords and PINs

Authentication secrets must not be stored in plaintext.

If the existing application uses PIN authentication, Codex must inspect the hashing algorithm, salt handling, rate limiting and recovery process.

A PIN must not be treated as equivalent to a strong password without considering its limited search space.

Repeated failed attempts must trigger appropriate throttling and protection against automated guessing.

### 3.6 Account recovery

Account recovery must verify control of an authorised recovery method or require an appropriate human verification process.

The system must not allow a person to take over an existing member account simply by knowing publicly available membership information.

Recovery operations must be auditable.

## 4. Membership Verification

### 4.1 Authoritative membership source

The authorised church membership list is the source used for membership verification.

The system must distinguish:

- An account registered in the application.
- An account matched to a membership record.
- An account awaiting human confirmation.
- An account whose membership has been verified.
- An account whose verification has been revoked or otherwise changed through an authorised process.

### 4.2 Approved pilot workflow

The pilot membership-verification process is:

1. The authorised administrator imports the approved membership list.
2. Newly registered accounts initially receive `REGISTERED` status.
3. The system attempts to match the account against the authorised membership data using approved identifying information.
4. Unambiguous matches are presented for administrator confirmation.
5. Ambiguous matches, duplicates and incomplete records are referred for manual review.
6. Only an authorised confirmation changes the account to `VERIFIED_MEMBER`.
7. Verification, correction, suspension and revocation actions are recorded in the audit history.

The system must not automatically verify a person merely because a matching name has been found.

### 4.3 Protection of membership records

The complete membership list is confidential.

It must not be:

- Exposed through ordinary AI conversations.
- Included in public offline packages.
- Sent to Gemini.
- Returned through unrestricted API endpoints.
- Accessible to ordinary members.
- Used as unrestricted model context.
- Exposed through application search functions without permission.

The backend must retrieve only the minimum membership information required for an authorised operation.

### 4.4 Existing role conflict

The current application has registration behaviour that assigns a role named `MEMBER`.

This role must not be interpreted as proof of verified church membership.

Codex must inspect and resolve any conflict between the existing role model and the new membership-verification state.

The system must not use an existing `is_active` flag or registration role as a substitute for verified membership.

## 5. Role and Permission Architecture

### 5.1 User categories

The system must support the following broad account categories:

- Public.
- Registered.
- Verified Member.
- Ministry Worker.
- Minister/Clergy.
- Senior Leadership.
- Administrator.

These categories help describe the user's relationship with the application and church. They do not independently grant every permission associated with their titles.

### 5.2 Permission-based access

The backend must use explicit permission assignments for protected actions.

Examples of permission identifiers include:

- `ai.online.use`
- `ai.usage.view_own`
- `ai.usage.view_aggregate`
- `knowledge.view_public`
- `knowledge.view_restricted`
- `knowledge.create`
- `knowledge.edit`
- `knowledge.approve`
- `knowledge.publish`
- `knowledge.withdraw`
- `membership.import`
- `membership.verify`
- `membership.correct`
- `membership.revoke`
- `attendance.view_own`
- `attendance.view_authorised`
- `attendance.correct`
- `escalation.view`
- `escalation.assign`
- `escalation.respond`
- `ai.configuration.manage`
- `security.audit.view`
- `system.administration`

These identifiers are proposed examples. Codex should establish a consistent permission catalogue and document the final identifiers.

### 5.3 Permission assignment

Permissions must be assigned through an authorised administrative process.

The system should support:

- Assignment to an individual account.
- Assignment through an approved role or group.
- Revocation.
- Expiry where applicable.
- A record of who made the assignment.
- A record of when it took effect.
- A record of subsequent changes.

An account must not retain a permission simply because it previously held a role that once granted it.

### 5.4 Permission evaluation

For every protected request, the backend must determine:

1. The authenticated account.
2. Its current account status.
3. Its verified membership status where required.
4. Its effective permission assignments.
5. Whether the requested resource is within the permitted audience.
6. Whether the permission or resource has expired or been revoked.
7. Whether any additional security condition applies.

The system must deny access by default when a required permission cannot be established.

### 5.5 Role hierarchy must not imply unlimited access

A senior title must not automatically provide access to confidential attendance, financial, pastoral or security records.

For example, permission to view a ministry programme must not automatically grant permission to inspect membership verification records.

Access to AI configuration must not automatically grant access to confidential pastoral escalation records.

The system must enforce these distinctions.

### 5.6 Separation of duties

Where practical, the architecture should support separation between:

- Membership verification.
- Knowledge management.
- Knowledge approval.
- Knowledge publication.
- Attendance administration.
- Pastoral escalation handling.
- Security administration.

During the pilot, the project owner has the specifically approved technical administration authority described in Section 6. That temporary arrangement must not be confused with a permanent church governance structure.

## 6. Pilot Technical Administrator

### 6.1 Pilot authority

During the pilot, the project owner is the sole authorised technical administrator of the Perazim AI system.

Within the scope of technical administration, the administrator may:

- Import authorised knowledge.
- Review and approve content for knowledge-base publication.
- Correct, replace or withdraw knowledge-base material.
- Manage AI configuration.
- Update AI behavioural instructions.
- Manage response rules.
- Manage technical permissions and access configuration.
- Authorise publication of approved knowledge packages.
- Inspect system health and security information.
- Manage the pilot's technical administrative access.
- Approve or reject technical changes within the approved project scope.

The system must support this arrangement without requiring the creation of unnecessary additional administrators during the pilot.

### 6.2 Technical administration is not ecclesiastical authority

Technical authority must remain separate from ecclesiastical authority.

The ability to approve a document for inclusion in the knowledge base does not automatically confer authority to amend official church doctrine.

The system must distinguish:

1. **Technical knowledge approval:** Confirming that material is suitable for publication in the AI knowledge system.
2. **Institutional approval:** Confirming that a document or policy is an authorised institutional record.
3. **Formal doctrinal amendment:** Changing an official doctrinal position through the church's authorised governance process.

Formal doctrinal changes must follow the church's established governance requirements.

The AI must not interpret technical administrator access as permission to bypass church governance.

### 6.3 Administrative audit

Every significant administrative action must be recorded.

The audit record should include:

- Administrator account.
- Action performed.
- Affected record or configuration.
- Previous and new revision identifiers where applicable.
- Timestamp.
- Outcome.
- Reason or change description where appropriate.

Audit records must be protected against unauthorised modification.

### 6.4 Future delegation

The architecture should allow additional technical administrators to be introduced later through an authorised process.

However, future delegation must not be assumed during the pilot.

The project must not create default administrative accounts or hidden backdoor access.

## 7. API Security

### 7.1 Backend enforcement

Every protected API endpoint must perform server-side authentication and authorisation.

The backend must not rely on:

- Android button visibility.
- Client-supplied roles.
- Client-supplied membership status.
- Client-supplied permissions.
- Client-supplied account identifiers without ownership checks.
- AI-generated permission decisions.
- An APK-embedded permanent shared token.

### 7.2 Input validation

All incoming API data must be validated.

Validation must cover:

- Required fields.
- Data types.
- Length limits.
- Allowed values.
- Identifier formats.
- Request sizes.
- Content types.
- Date and timestamp formats.
- Numeric ranges.

Invalid input must be rejected safely.

### 7.3 Object-level authorisation

The backend must verify that a user is authorised to access the specific record requested.

Authentication alone is insufficient.

For example, an authenticated member requesting an attendance record must not be able to change a member ID in the request and retrieve another person's attendance.

The same rule applies to:

- Membership records.
- Knowledge documents.
- AI usage information.
- Pastoral escalations.
- Administrative settings.
- Other private records.

### 7.4 Rate limiting

The API must apply rate limits to sensitive and resource-intensive endpoints.

These should include:

- Login and account recovery.
- Membership verification attempts.
- AI requests.
- Knowledge-management operations.
- Administrative actions.
- Escalation submission.
- Attendance submission.
- Other endpoints vulnerable to automated abuse.

Limits should be configurable and enforced server-side.

### 7.5 Error handling

API errors must not expose:

- Database queries.
- Internal stack traces.
- Secret values.
- Authentication tokens.
- Private membership records.
- Other users' information.
- Internal infrastructure details.

Detailed diagnostic information should remain in appropriately protected server logs.

## 8. Android Application Security

### 8.1 Client trust boundary

The Android application must be treated as an untrusted client.

A user may inspect application files, modify the application, intercept requests on a compromised device or submit requests without using the normal interface.

Security must therefore remain effective even if the client is modified.

### 8.2 Embedded credentials

The existing implementation includes an APK-embedded shared token used in the online AI integration.

This arrangement must not remain the basis for trusted user authentication or unrestricted Worker access.

Codex must identify every use of the token and replace or neutralise it through the approved server-to-server authentication architecture.

No permanent Gemini or backend administrative secret may be packaged in the APK.

### 8.3 Device identifiers

A client-supplied device identifier is not proof of user identity.

It may be used as supplementary telemetry or risk information where appropriate, but it must not replace authenticated account identity.

A modified client must not be able to impersonate another device or account merely by submitting a chosen identifier.

### 8.4 Local storage

Sensitive local information must be stored using appropriate Android security controls.

The implementation must inspect:

- SharedPreferences usage.
- Room database storage.
- Local files.
- Authentication token storage.
- Cached knowledge.
- Offline package storage.
- Temporary AI context.
- Application backup behaviour.

Credentials and sensitive records must not be stored in ordinary plaintext preferences or unprotected files.

Where appropriate, use Android Keystore-backed protection and encrypted storage.

The chosen encryption design must be compatible with the existing application and must include a practical recovery and migration strategy.

### 8.5 Backup and data extraction

The application must review Android backup and data-extraction settings.

Authentication secrets, confidential local records and restricted offline knowledge must not be unintentionally exported through backup mechanisms.

Codex must inspect the manifest and applicable Android configuration before changing these settings.

### 8.6 Application permissions

The application must request only permissions necessary for its features.

Camera access for QR scanning, notification permission for reminders and any other runtime permissions must be requested only when needed and explained clearly.

The application must not request continuous GPS or unrelated permissions for the AI system.

## 9. Network and Transport Security

### 9.1 Production HTTPS

All production communication involving accounts, AI requests, membership, knowledge, attendance and other protected information must use HTTPS.

Cleartext HTTP must not be permitted for production API communication.

### 9.2 Existing cleartext configuration

The current Android manifest or network configuration permits cleartext traffic, and local development has used HTTP.

Codex must inspect the exact configuration and establish a secure production configuration.

Development-only HTTP exceptions must be narrowly scoped and must not weaken production security.

### 9.3 Certificate validation

The application must use standard secure certificate validation.

It must not accept invalid certificates or disable TLS verification to work around development or deployment problems.

Certificate pinning may be considered only if its operational consequences, certificate rotation and recovery process are properly designed. It must not be introduced casually.

### 9.4 Service-to-service security

Communication between Express and the Cloudflare Worker must be authenticated and protected.

The Android application must not be able to impersonate the Express backend.

The Worker must reject unauthorised direct provider requests.

## 10. Cloudflare Worker and Gemini Security

### 10.1 Worker access

The Worker must accept provider requests only from an authorised backend service or through a properly restricted short-lived grant.

The existing APK-embedded shared token must not be treated as sufficient authentication for the target architecture.

### 10.2 Provider secrets

Gemini credentials must be held in server-side secret storage.

They must never be returned to the Android application or included in ordinary logs.

### 10.3 Request validation

The Worker must validate:

- Calling service authentication.
- Required request structure.
- Allowed model configuration.
- Request size.
- Token limits.
- Timeout constraints.
- Applicable provider restrictions.

The Worker must not trust arbitrary provider instructions or configuration supplied by the Android client.

### 10.4 Quota responsibility

The Express backend is the authoritative usage-accounting service.

Cloudflare KV must not be used as the sole authoritative mechanism for concurrent quota reservation and settlement.

The Worker may support secondary operational controls, but these must not replace PostgreSQL-backed authoritative accounting.

### 10.5 Provider isolation

Gemini must receive only the information necessary for the authorised request.

The Worker must not receive unrestricted membership records, attendance records, administrative data or confidential pastoral information.

Sensitive pastoral information is prohibited from Gemini during the pilot.

## 11. Database Security

### 11.1 PostgreSQL

The backend database must use appropriately restricted database credentials and access permissions.

The application must not use unrestricted database administrator credentials for normal runtime operations.

Where practical, database access should follow least privilege.

### 11.2 Room and SQLite

The local Room database must be treated as device-local storage, not as a secure authority for membership, permissions or institutional approval.

Local records may support offline functionality, but protected online actions must be validated against authoritative backend records.

### 11.3 Data separation

The database design must preserve separation between:

- Account identity.
- Membership verification.
- Permission assignments.
- Approved knowledge.
- AI usage records.
- Pastoral escalation records.
- Attendance records.
- Administrative audit history.

Sensitive pastoral and attendance records must not be mixed into ordinary AI conversation storage or shared response caches.

### 11.4 Database migrations

All database changes must use incremental, data-preserving migrations.

The existing Room destructive migration fallback must not be relied upon in the final production design.

Codex must establish and test supported upgrade paths before removing any fallback that may currently be needed for development or installation.

Room schema export and version tracking must be enabled as part of the migration strategy.

PostgreSQL schema changes must also be versioned and applied through controlled migrations.

### 11.5 Backups and restoration

Production databases must have a defined backup and restoration process.

The project must document:

- Backup frequency.
- Retention period.
- Encryption.
- Access restrictions.
- Restoration procedure.
- Recovery objectives.
- Restoration testing.

A backup that has never been tested for restoration must not be assumed reliable.

## 12. Knowledge and Offline Package Security

### 12.1 Approved knowledge access

Knowledge retrieval must enforce document approval, audience, revision and permission requirements before returning content.

An unpublished or withdrawn document must not become accessible simply because it exists in the database or local search index.

### 12.2 Signed offline packages

Offline knowledge packages must be protected against unauthorised modification.

Each package must include a verifiable manifest containing the necessary information, including:

- Package identifier.
- Package version.
- Knowledge revision.
- Audience or access scope.
- Creation time.
- Expiry time where applicable.
- Document or content hashes.
- Schema version.
- Signing key identifier.

Packages must be signed using an appropriate asymmetric digital-signature mechanism.

The signing private key must remain server-side.

The Android application must verify package authenticity and integrity before activation.

### 12.3 Package installation

The application must reject packages with:

- Invalid signatures.
- Invalid hashes.
- Unsupported schemas.
- Missing required metadata.
- Invalid audience declarations.
- Expired validity where expiry is mandatory.
- Unexpected content or structural corruption.

Package activation must be atomic.

If an update is interrupted or fails validation, the application must retain the last valid active package.

### 12.4 Restricted offline content

Restricted offline content must have an explicitly defined audience, expiry policy and revocation limitation.

A disconnected device cannot receive an immediate revocation instruction.

The system must therefore use bounded offline validity for restricted content and disclose this limitation in the technical design.

A local membership or permission snapshot must not become permanent proof of current access.

### 12.5 Public versus restricted content

Public approved knowledge may be packaged for general offline use.

Restricted material must be included only where the user is authorised and the package policy permits it.

The complete membership register and confidential pastoral records must never be included in ordinary AI knowledge packages.

## 13. Pastoral and Attendance Data Security

### 13.1 Pastoral escalation records

Pastoral escalation records must be stored separately from ordinary AI conversation context.

Access must be restricted to explicitly authorised people.

The system must support identified and anonymous submissions according to the escalation policy.

Anonymous submissions must not expose the submitter's identity or contact details to ordinary administrators.

Where a protected identity-to-case mapping is necessary for response delivery, it must be stored separately and accessed only through an authorised workflow.

The system must not promise absolute anonymity.

### 13.2 Attendance records

Attendance records must be stored in the church backend and protected through explicit permissions.

A member must not be able to inspect or alter another member's attendance record.

Attendance data must not be supplied to Gemini, Qwen prompts, ordinary AI history, shared caches or general knowledge packages.

### 13.3 No automatic disciplinary authority

The AI must not use attendance records to decide a person's spiritual condition, suspend membership or impose church discipline.

Any formal membership or disciplinary action must follow the authorised human governance process.

## 14. Logging and Audit Security

### 14.1 Security logging

The system must record significant security events, including:

- Authentication failures.
- Suspicious access attempts.
- Permission denials.
- Membership verification changes.
- Administrative changes.
- Knowledge approval and withdrawal.
- Package publication.
- Quota abuse.
- Security configuration changes.
- Relevant API failures.
- Account recovery and revocation events.

### 14.2 Privacy-safe logs

Ordinary technical logs must not contain:

- Passwords or PINs.
- Authentication tokens.
- Provider API keys.
- Full AI prompts and responses.
- Confidential pastoral disclosures.
- Full membership lists.
- Private attendance records.
- Unnecessary personal identifiers.

Logs should use request identifiers and other minimal references where possible.

### 14.3 Audit integrity

Audit records must be protected against unauthorised alteration or deletion.

Administrative users must not be able to silently rewrite their own security history.

The implementation should document retention, access, backup and integrity protections.

## 15. Threat Model and Required Protections

The following threats must be considered during implementation and testing.

| Threat | Required protection |
|---|---|
| Unverified user accesses online AI | Backend membership verification |
| User changes role in an API request | Server-derived roles and permissions |
| Modified APK bypasses UI restrictions | Server-side authorisation |
| APK secret is extracted | No permanent privileged secrets in APK |
| User changes another member's record ID | Object-level authorisation |
| Token is stolen | Secure storage, expiry and revocation |
| Provider API key is exposed | Server-side secret management |
| Concurrent requests bypass quotas | Transactional PostgreSQL accounting |
| Direct Worker access bypasses Express | Server-to-server authentication |
| Prompt injection changes AI behaviour | Trusted instruction separation and response validation |
| Unapproved knowledge is retrieved | Approval and audience filtering |
| Old cache returns withdrawn policy | Revision-aware invalidation |
| Offline package is modified | Signature and hash verification |
| Restricted package remains valid indefinitely | Bounded expiry and package policy |
| Pastoral disclosure appears in logs | Content-minimised, privacy-safe logging |
| User accesses another person's attendance | Object-level permissions |
| Administrator makes unauthorised changes | Explicit permissions and audit history |
| Database migration destroys records | Tested incremental migrations and backups |
| Production traffic is intercepted | HTTPS and correct certificate validation |

Codex should extend this threat model when repository inspection identifies additional risks.

## 16. Existing Security Gaps Requiring Attention

The following issues have already been identified in the repository inspection and must be addressed.

### 16.1 APK-embedded shared token

The current online AI integration uses a shared token embedded in the APK.

This does not establish verified user identity and must not remain the foundation of online AI authorisation.

### 16.2 Client-supplied device identifier

The existing Worker integration accepts a client-supplied device identifier.

This value must not be treated as a trusted account identity or security credential.

### 16.3 Non-atomic Worker quota counters

The existing Worker KV quota mechanism uses counters that are not suitable for authoritative concurrent accounting and may fail open during errors.

Authoritative quota enforcement must move to a transactionally controlled backend mechanism.

### 16.4 Cleartext network configuration

The application currently permits cleartext traffic in its Android configuration.

Production communication must use HTTPS, and development exceptions must be narrowly scoped.

### 16.5 Destructive Room migration fallback

The current Room configuration includes destructive migration fallback.

The final safe architecture must use tested data-preserving migrations and retain existing user data.

### 16.6 Incomplete permission model

The existing backend account roles do not establish the full fine-grained permissions required by this project.

A server-enforced permission system must be introduced without confusing account role, membership status and administrative authority.

These findings are starting points, not a complete security audit. Codex must inspect the current code and verify the exact implementation state before modifying it.

## 17. Security Testing and Acceptance Criteria

### 17.1 Authentication

- Invalid credentials are rejected.
- Expired tokens are rejected.
- Revoked sessions cannot perform protected operations.
- Authentication secrets are never logged.
- Repeated failed authentication is rate-limited.
- Account recovery cannot be abused to take over another account.

### 17.2 Membership and permissions

- Registered unverified accounts cannot access online AI.
- Verified membership is established only through the approved workflow.
- Existing `MEMBER` roles cannot independently establish verification.
- Permissions are enforced by the backend.
- A user cannot access another user's private records by changing request parameters.
- Revoked permissions are respected.
- Leadership titles do not automatically grant unrestricted access.

### 17.3 Administrator security

- Only the authorised pilot administrator can perform pilot technical administration.
- Administrative actions are audited.
- No hidden or default administrator account exists.
- Technical administrator access does not silently amend formal doctrine.
- Future administrator delegation requires an authorised process.

### 17.4 Android and network security

- No permanent privileged backend or provider secret is embedded in the APK.
- Production API traffic uses HTTPS.
- Invalid TLS certificates are rejected.
- Sensitive local data receives appropriate protection.
- Backup settings do not unintentionally expose restricted data.
- A modified client cannot bypass server-side permissions.

### 17.5 AI and provider security

- Direct unauthorised Worker requests are rejected.
- Gemini credentials remain server-side.
- Sensitive pastoral information is blocked from Gemini.
- Client-supplied roles and quotas are ignored as authority.
- Concurrent quota requests cannot bypass limits.
- Prompt injection cannot override trusted instructions.

### 17.6 Knowledge and offline security

- Unapproved documents are not returned as approved knowledge.
- Restricted documents are not exposed to unauthorised users.
- Invalid package signatures are rejected.
- Corrupted packages are rejected.
- Interrupted package updates do not destroy the active package.
- Expired restricted packages are handled according to policy.
- Withdrawn content is removed or invalidated through the update process.

### 17.7 Database and recovery

- Supported Room upgrade paths preserve user data.
- PostgreSQL migrations preserve existing records.
- Backup restoration is tested.
- Audit records are protected.
- Database credentials follow least privilege.

### 17.8 Regression testing

Security changes must not break existing application functionality, including:

- Community and private messaging.
- Media and file sharing.
- Classroom features.
- Bible reader.
- Worship and audio features.
- Existing account functions.
- Offline AI.
- Online AI.
- Existing application navigation.

## 18. Implementation Requirements

Codex must:

1. Inspect the complete existing authentication and authorisation implementation.
2. Identify all API endpoints and their current access controls.
3. Map current account roles and database relationships.
4. Identify every use of the APK-embedded shared token.
5. Inspect the Cloudflare Worker authentication and quota implementation.
6. Inspect Android local storage and backup settings.
7. Inspect the manifest and network security configuration.
8. Inspect Room and PostgreSQL migration strategies.
9. Design the membership-verification and permission model.
10. Introduce server-side permission enforcement.
11. Secure Express-to-Worker communication.
12. Implement appropriate token and secret management.
13. Establish secure offline package verification.
14. Implement privacy-safe logging and audit records.
15. Add automated security tests.
16. Document any remaining risks and operational dependencies.

The implementation must be incremental and must preserve existing application data and functionality.

Codex must not introduce a new backend framework or replace the existing Java, Express, PostgreSQL, Cloudflare Worker, Qwen or Gemini architecture without first demonstrating that the existing architecture cannot meet a specific requirement.

## 19. Explicitly Prohibited Security Shortcuts

The following approaches are not acceptable:

- Treating Android UI restrictions as authorisation.
- Treating a client-supplied role as proof of authority.
- Treating registration as verified membership.
- Treating a device identifier as authentication.
- Embedding permanent provider secrets in the APK.
- Allowing unrestricted direct Android-to-Worker provider access.
- Using non-atomic counters as authoritative financial quota controls.
- Disabling TLS validation.
- Permitting production cleartext API traffic.
- Returning confidential records based only on authentication.
- Including the full membership list in AI prompts or public offline packages.
- Sending sensitive pastoral disclosures to Gemini during the pilot.
- Storing ordinary AI conversation transcripts permanently.
- Using destructive database migrations as a normal upgrade strategy.
- Creating hidden administrator accounts or backdoors.
- Allowing AI models to grant permissions or make security decisions.

## 20. Implementation Authority

This document establishes the required security and access-control design for Perazim AI.

Codex must inspect the existing repository, identify reusable security components, document gaps and propose a safe implementation sequence.

Where a technical choice is not fixed by an approved project policy, Codex should select a secure, maintainable solution compatible with the existing architecture and explain the decision.

Where a technical requirement conflicts with an approved privacy, membership, institutional or doctrinal policy, Codex must report the conflict rather than weaken the policy.

This document does not independently authorise implementation.

Code changes may begin only after the complete documentation set has been reviewed and the project owner has supplied the final master implementation instruction.
