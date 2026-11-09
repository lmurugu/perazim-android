# 06 — ONLINE AI SPECIFICATION

## 1. Purpose

This document defines the functional, technical, security and operational requirements for the online AI capability of the Perazim Missions Church International Android application.

The online AI service supplements the offline AI capability. It provides access to a more capable generative model and, where authorised, current external research. It must nevertheless operate within the same institutional identity, theological foundation, scope restrictions, privacy requirements and response standards as the offline AI.

The online service must not become an independent or less restricted version of Perazim AI.

The requirements in this document must be implemented alongside:

- `00_PROJECT_OVERVIEW.md`
- `01_TECHNICAL_ARCHITECTURE.md`
- `02_AI_IDENTITY_AND_BEHAVIOUR.md`
- `03_THEOLOGICAL_FOUNDATION.md`
- `04_KNOWLEDGE_ARCHITECTURE.md`
- `05_OFFLINE_AI_SPECIFICATION.md`

Where another document establishes a more specific security, privacy, doctrinal or knowledge-management rule, that rule remains applicable to the online service.

## 2. Online AI Access Policy

### 2.1 Eligible users

Online generative AI access is restricted to authenticated, verified Perazim church members during the pilot.

The following distinctions are mandatory:

| Account status | Online AI access |
|---|---|
| Public user | Not permitted |
| Registered, unverified user | Not permitted |
| Verified member | Permitted, subject to quota and other controls |
| Ministry worker | Permitted only if also verified and otherwise eligible |
| Minister or clergy | Permitted only if verified and otherwise eligible |
| Senior leadership | Permitted only if verified and otherwise eligible |
| Administrator | Permitted only if verified and otherwise eligible |

An account's role, title, registration status or possession of an active session must not independently establish verified membership.

The backend must confirm that the account is currently eligible. The Android application must never be the final authority on membership verification.

### 2.2 Membership verification

The membership-verification workflow is defined in the project-wide membership and administration requirements.

For online AI access, the authoritative rule is:

1. The user must have an authenticated account.
2. The account must have a verified membership status.
3. The verification must not have been revoked or suspended through an authorised process.
4. The backend must evaluate the account's current permissions and applicable restrictions.
5. The request must satisfy the applicable usage quota and safety controls.

The system must not infer verification from an existing database role named `MEMBER`, an `is_active` flag, a locally stored membership label or a client-supplied assertion.

### 2.3 Permission changes

A user whose membership verification or online AI permission has been revoked must lose online access when the backend recognises the change.

Short-lived access tokens may be used for authentication, but they must not permit indefinite access after revocation. The backend must check current eligibility or use a revocation-aware mechanism.

Offline access remains governed by the offline AI specification. Revocation of online access must not accidentally delete or disable publicly available offline resources.

## 3. Target Online Architecture

### 3.1 Required request path

The recommended target request path is:

**Android application → Express backend → Cloudflare Worker → Gemini API**

The Express backend is the authoritative gateway for:

- User authentication.
- Membership verification.
- Permission evaluation.
- AI request validation.
- Scope and safety enforcement.
- Usage quota reservation.
- Approved knowledge retrieval or retrieval coordination.
- Request auditing.
- Provider response handling.
- Usage settlement and accounting.

The Cloudflare Worker provides a controlled provider-integration layer between the Express backend and Gemini.

Gemini is an external model provider. It does not become an authority over Perazim doctrine, church governance, membership, user permissions or approved knowledge.

### 3.2 Why Express is the authoritative gateway

The current application includes a Cloudflare Worker integration. The target architecture must not rely on the Android application as the authority for user identity, membership or usage accounting.

The Express backend already provides the principal account and application backend infrastructure. It should therefore become the authoritative decision point for online AI requests.

The Android application must not be permitted to bypass Express and directly obtain unrestricted Gemini access through the Worker.

The implementation must investigate the existing Worker integration and migrate it safely. Existing application functionality must not be broken during the transition.

### 3.3 Express-to-Worker authentication

Express-to-Worker communication must use a separate server-to-server authentication mechanism.

The mechanism must:

- Authenticate the calling backend.
- Prevent unauthorised direct invocation.
- Have a defined token or credential lifetime.
- Restrict the credential to the intended service and purpose.
- Support rotation and revocation.
- Avoid embedding permanent secrets in the Android APK.
- Avoid exposing provider credentials to the Android application.
- Avoid logging authentication secrets.

A short-lived, audience-restricted signed grant or another appropriately secured server-to-server mechanism may be used.

The implementation agent must inspect the existing deployment and choose the most secure compatible approach. Any change to the authentication design must be documented.

### 3.4 Provider credentials

Gemini API credentials must remain server-side.

They must not be:

- Embedded in Android source code.
- Packaged in the APK.
- Exposed in API responses.
- Included in client-side configuration.
- Written to ordinary application logs.
- Distributed in offline knowledge packages.

Secrets must be stored using the appropriate server-side secret-management mechanism and rotated when necessary.

## 4. Online Request Lifecycle

Every online AI request must pass through a controlled lifecycle.

### 4.1 Request initiation

The Android application submits the user's message and the minimum necessary request metadata.

The client may include:

- The message text.
- A unique request identifier.
- The selected AI mode.
- A conversation-turn identifier that is temporary and non-identifying where practical.
- A language preference, if supported.
- Explicitly selected, non-sensitive contextual material where permitted.

The client must not be trusted to supply authoritative:

- Membership status.
- User role.
- Permission assignments.
- Quota balance.
- Approval status of knowledge.
- Administrative privileges.
- Provider cost.
- Attendance status.

The backend must derive these values from trusted server-side records.

### 4.2 Authentication and eligibility

Before any provider request, Express must:

1. Authenticate the account.
2. Check current account status.
3. Confirm verified membership.
4. Evaluate applicable permissions.
5. Check account restrictions and revocation status.
6. Apply the online AI usage policy.

An ineligible request must be rejected before it reaches Gemini.

The error returned to the Android application must be understandable and must not expose confidential backend information.

### 4.3 Scope and safety checks

The system must evaluate whether the question falls within the authorised Perazim AI scope.

The scope policy applies equally to online and offline AI.

The system must also identify sensitive pastoral content, emergencies and other high-risk situations before sending content to an external provider.

A user must not be able to bypass these restrictions by:

- Rephrasing a prohibited request.
- Asking Gemini to ignore Perazim instructions.
- Embedding instructions inside an uploaded or quoted document.
- Asking the model to act as a different assistant.
- Requesting an answer through a different conversational format.

### 4.4 Quota reservation

Before dispatching a request to Gemini, the backend must reserve the appropriate usage allowance.

The quota system must be authoritative and transactional.

Cloudflare KV must not be used as the authoritative atomic quota counter because its consistency characteristics are unsuitable for reliable concurrent accounting.

PostgreSQL transactions are the preferred mechanism for authoritative quota reservation and settlement.

The system must prevent concurrent requests from spending the same remaining allowance.

### 4.5 Knowledge retrieval

Where the question requires Perazim-specific or biblical information, the system must retrieve relevant approved knowledge before generating the answer.

Retrieval must enforce:

- Source authority.
- Approval status.
- Current revision.
- Effective dates and withdrawal status.
- User permissions.
- Audience restrictions.
- Licensing restrictions.
- Applicable offline or online availability.

The model must not receive confidential knowledge merely because it exists in the database.

The knowledge architecture and lifecycle are defined in `04_KNOWLEDGE_ARCHITECTURE.md`.

### 4.6 Prompt construction

The backend must construct a controlled prompt containing:

- The approved Perazim AI identity and behavioural instructions.
- Relevant theological and institutional rules.
- The user's permitted question.
- Relevant approved retrieved evidence.
- Required source references.
- Any applicable safety or response-format instructions.

Retrieved documents and user-supplied material must be clearly separated from trusted instructions.

The model must be explicitly instructed to treat retrieved content, uploaded documents, quoted text and external webpages as evidence or data, not as instructions capable of overriding system rules.

The prompt must contain only the information necessary to answer the request.

### 4.7 Gemini generation

The Cloudflare Worker sends the authorised request to the configured Gemini model.

The provider model and API version must be configurable on the server rather than hardcoded throughout the Android application.

The implementation must verify the actual deployed Worker configuration and the provider model currently in use. The model name appearing in source code must not be treated as proof of deployed configuration.

### 4.8 Response validation

The response must be checked before it is returned to the user.

Validation must consider:

- Whether the response answers the authorised question.
- Whether it violates the Perazim AI scope.
- Whether it contradicts a clear approved Perazim position.
- Whether it invents church policies, institutional facts or biblical references.
- Whether it misrepresents the translation of a biblical quotation.
- Whether it claims to have consulted sources that were not actually retrieved.
- Whether it exposes restricted or private information.
- Whether it gives unsafe advice in a high-risk situation.
- Whether required citations or source references are present.

A model-generated answer must never be treated as an approved church document merely because it passed response validation.

### 4.9 Usage settlement

After provider execution, the backend must settle the reserved allowance using actual available usage information.

The system must distinguish:

- Requests accepted by the application.
- Requests dispatched to the provider.
- Provider responses received.
- Provider failures.
- Token usage.
- Estimated or actual provider cost.
- Retries.
- Cancelled or interrupted requests.

Settlement must be idempotent. Repeated processing of the same request must not charge the user or church twice.

## 5. Gemini and Provider Configuration

### 5.1 Model selection

The provider model must be selected through server-side configuration.

The implementation must document:

- Provider name.
- Model identifier.
- API version.
- Supported capabilities.
- Maximum request size.
- Maximum output size.
- Timeout.
- Retry policy.
- Applicable provider pricing or usage measurement.
- Any provider-specific data-processing restrictions.

The existing Worker source references a Gemini model configuration. Codex must verify whether the deployed configuration matches the source and identify any differences.

No model identifier should be assumed current without verification.

### 5.2 Provider replacement

The architecture should allow the provider or model to be changed without rewriting the Android application.

Provider-specific code should be isolated behind a backend integration interface.

A provider change must not alter:

- Membership rules.
- Perazim theological authority.
- Knowledge approval requirements.
- Scope restrictions.
- Privacy rules.
- Usage accounting requirements.

### 5.3 Provider terms and data processing

Before production deployment, the project owner must verify the provider's current terms, data-processing commitments, retention settings and applicable service tier.

The project must not assume that a particular provider configuration guarantees that submitted information is never retained or used in any other way.

Sensitive pastoral information is prohibited from Gemini during the pilot regardless of any provider privacy assurances.

## 6. Sensitive Pastoral Information

### 6.1 Pilot restriction

Identifiable or sensitive pastoral information must not be sent to Gemini during the pilot.

This is a mandatory policy.

Examples include:

- Personal marital difficulties.
- Abuse disclosures.
- Suicidal thoughts or self-harm disclosures.
- Confidential pastoral confessions.
- Sensitive family conflicts.
- Identifiable personal allegations.
- Other information that could expose a person's private pastoral circumstances.

### 6.2 Detection and handling

The system must attempt to identify sensitive content before external transmission.

It must not rely exclusively on a checkbox or on the user knowing what qualifies as sensitive.

Where sensitive content is detected or reasonably suspected, the system must:

1. Prevent the sensitive content from being transmitted to Gemini.
2. Avoid copying the sensitive message into ordinary server logs.
3. Offer an appropriate local response where safe and technically possible.
4. Explain that online AI cannot process the sensitive disclosure.
5. Offer the separate human follow-up or escalation process where appropriate.

Automated sensitivity detection is not guaranteed to be perfect. The architecture must therefore minimise the information sent to Gemini and use conservative handling where uncertainty remains.

### 6.3 General pastoral questions

General questions about pastoral ministry, Christian living, marriage, grief or spiritual struggles may be handled online when they do not contain sensitive personal disclosures or identifiable confidential information.

For example, a general question about biblical principles for marriage is different from a detailed account of a member's private marital crisis.

### 6.4 Emergency situations

An emergency must not be treated as an ordinary provider-routing problem.

Where a user indicates immediate danger, suicidal intent, abuse or a threat of violence, the system must prioritise immediate safety guidance and appropriate human assistance.

It must not wait for Gemini to respond or depend on online AI availability.

The full emergency and escalation procedure is defined in `09_PASTORAL_SAFETY_AND_ESCALATION.md`.

## 7. External Research

### 7.1 Access restriction

External research through the online AI is available only to users authorised to use online AI.

It must remain subject to the Perazim scope and safety rules.

External research must not be presented as official Perazim knowledge.

### 7.2 Research presentation

When the service uses external research, it should distinguish:

1. What Scripture says.
2. What approved Perazim documents establish.
3. What historical or academic sources report.
4. What other Christian traditions believe.
5. What remains disputed or uncertain.

Where external sources are used, the response should identify the source sufficiently for the user to verify it.

The system must not invent citations, URLs, authors, publication details or quotations.

If browsing or external retrieval is unavailable, the AI must not claim that it conducted live research.

### 7.3 Institutional authority

External research cannot automatically:

- Amend Perazim doctrine.
- Override the Constitution.
- Change an approved institutional policy.
- Establish a new church position.
- Publish a new knowledge-base document.

A conflict between external research and an approved Perazim position must be handled according to the theological foundation and knowledge authority rules.

### 7.4 Research and knowledge-base separation

External research results must remain separate from approved institutional knowledge.

A user may request that useful research be considered for future inclusion, but the research must enter the formal knowledge review and approval process before it becomes approved knowledge.

Gemini must never directly publish its own answers into the church knowledge base.

## 8. Usage Limits and Financial Controls

### 8.1 Quota policy

Online AI usage must be controlled through configurable server-side limits.

The system should support:

- Per-account daily request limits.
- Per-account daily token limits.
- Per-account monthly usage limits.
- Global provider spending limits.
- Per-minute request limits.
- Maximum input and output token limits.
- Restrictions on repeated or abusive requests.

Exact numerical limits should be configurable rather than scattered through application code.

The initial pilot limits must be set by the project owner before production deployment.

### 8.2 Atomic quota accounting

The PostgreSQL backend must provide authoritative quota accounting.

The implementation must use transactions or an equivalent concurrency-safe mechanism to:

1. Validate remaining allowance.
2. Reserve usage.
3. Associate the reservation with a unique request identifier.
4. Prevent duplicate reservations.
5. Settle or release the reservation according to the request outcome.
6. Record the resulting usage.

Concurrent requests must not bypass the limit.

### 8.3 Idempotency

Each online AI request must have a unique request identifier.

Retries must reuse the same identifier where they represent the same logical request.

The backend must distinguish a legitimate new request from a repeated submission caused by a timeout, lost response or network retry.

The same logical request must not produce duplicate charges or duplicate quota consumption.

### 8.4 Provider cost controls

The system must support a hard global spending limit.

When the configured limit is reached, new provider requests must be blocked or handled through an explicitly approved fallback.

The system must not silently continue generating provider costs after reaching the limit.

Administrators should be able to inspect aggregate usage and cost information without needing access to private pastoral conversations.

### 8.5 Abuse detection

The backend should detect patterns such as:

- Excessive rapid requests.
- Repeated failed authentication.
- Unusual token consumption.
- Automated request patterns.
- Repeated attempts to bypass limits.
- Abnormal account or device usage.

Abuse controls must not rely on a client-supplied device identifier as proof of identity.

## 9. Online AI Conversation Context

### 9.1 Temporary context

The online AI may use a limited amount of recent conversational context to maintain continuity within an active conversation.

This context must be temporary.

It must not become a permanent user profile, a permanent conversation archive or a source of automatic model training.

### 9.2 No persistent AI conversation history

The project-wide policy is that ordinary AI conversations are not retained as persistent chat history.

The online service must therefore not create a permanent transcript of ordinary conversations in PostgreSQL or another shared storage system.

Minimal technical usage and security records may be retained under the project's data-retention policy, but these must not become a transcript of the user's conversation.

### 9.3 Context minimisation

Only relevant recent conversational context should be sent to Gemini.

The service must avoid sending unnecessary personal information, unrelated previous questions, private account details or confidential institutional records.

The context window must have a defined token budget.

### 9.4 Separation of users

Conversation context must never be shared between different accounts.

A temporary conversation identifier must not be accepted as proof of account identity.

A user must not be able to retrieve another user's context by changing an identifier.

### 9.5 No automatic training

Ordinary user conversations must not be used to train, fine-tune or improve a model automatically.

Any future proposal to use conversation data for model improvement requires a separate policy, explicit approval and appropriate consent and privacy safeguards.

## 10. Online Knowledge Retrieval and Citations

### 10.1 Approved evidence

Where the answer depends on Perazim-specific information, the online AI must use approved knowledge sources.

It must not rely on Gemini's general model knowledge as the authority for:

- Church leadership.
- Official church history.
- Current ministry schedules.
- Membership rules.
- Church policies.
- Official doctrine.
- Institutional contacts.
- Administrative procedures.

If approved evidence is unavailable, the AI must disclose the limitation.

### 10.2 Biblical references

Biblical answers must identify references accurately.

The ESV is the preferred translation for quotations and teaching, subject to confirmed licensing and redistribution rights.

Until the necessary rights are established, the system must not assume that the ESV text already present in a database can legally be redistributed or included in offline packages.

Where another translation is used, it must be identified correctly.

The system must never label KJV, NASB or another translation as ESV.

### 10.3 Source traceability

Where a response relies on retrieved church documents, the system should retain enough internal source metadata to identify:

- The source document.
- Its revision.
- Its approval status.
- The relevant section or passage.
- The applicable publication date.

The user-facing response should provide a suitable source reference where possible.

Internal source metadata must not expose confidential administrative information to users who lack permission.

### 10.4 Unsupported claims

The AI must not fabricate:

- Constitution clauses.
- Church policies.
- Official statements.
- Scripture quotations.
- Historical events.
- Academic references.
- Research findings.
- Provider actions or browsing activity.

If it cannot establish a claim from available evidence, it must say so.

## 11. Error Handling and Fallback

### 11.1 General principle

Online AI failure must not cause the entire application to fail.

The Android application must handle provider and network errors gracefully and preserve access to other app features.

### 11.2 Failure conditions

The system must handle:

- No internet connection.
- Express backend unavailable.
- Cloudflare Worker unavailable.
- Gemini unavailable.
- Provider rate limiting.
- Authentication failure.
- Membership verification failure.
- Quota exhaustion.
- Request timeout.
- Malformed provider response.
- Invalid or withdrawn knowledge.
- Failed response validation.
- Interrupted requests.
- Duplicate request submissions.

### 11.3 Fallback behaviour

Where online AI cannot be used, the application may offer the offline AI if it is available and the request is permitted under offline rules.

The user must be informed when the application switches from online to offline AI.

The application must not imply that an offline response has access to current external research or live information.

The offline AI must apply its own local permission and knowledge-package restrictions.

### 11.4 Provider failures

A provider failure must not be reported as a successful answer.

The quota system must reconcile failed and interrupted requests according to the actual provider execution status and available usage information.

The system must not blindly retry a request in a way that creates duplicate provider costs.

### 11.5 User-facing messages

Messages should be clear and practical.

Examples:

- "Online AI is currently unavailable. You can try again later or continue with offline AI."
- "Your account is not currently verified for online AI access. Please contact the church administrator if you believe this is incorrect."
- "Your online AI usage allowance has been reached."
- "This question contains sensitive personal information and cannot be sent to the online AI service. You may use the available private follow-up process."
- "The online AI could not verify this information from approved church sources."

These messages are examples, not a requirement to use identical wording.

## 12. Logging, Monitoring and Audit

### 12.1 Permitted technical records

The system may retain minimal records required for:

- Authentication and access decisions.
- Request identifiers.
- Request timestamps.
- Provider and model identification.
- Token usage.
- Cost accounting.
- Quota reservation and settlement.
- Error categories.
- Security events.
- Administrative configuration changes.

### 12.2 Prohibited ordinary logging

Ordinary logs must not contain:

- Full user prompts.
- Full model responses.
- Confidential pastoral disclosures.
- Private membership information.
- Authentication secrets.
- Provider API keys.
- Sensitive retrieved documents.

Where debugging requires additional information, it must use synthetic or appropriately redacted data wherever possible.

### 12.3 Administrative visibility

The pilot administrator should be able to inspect:

- Aggregate online AI usage.
- Account-level usage totals where authorised.
- Provider costs.
- Quota exhaustion.
- Authentication failures.
- Error rates.
- Service availability.
- Security incidents.
- Configuration changes.

The ordinary usage dashboard must not become a mechanism for reading members' private AI conversations.

### 12.4 Auditability

Important administrative and security actions must be recorded with:

- Actor.
- Action.
- Target.
- Timestamp.
- Outcome.
- Relevant configuration or revision identifier.

Audit records must not unnecessarily reproduce sensitive message content.

## 13. Security Requirements

The online AI service must implement:

- Server-side authentication and permission enforcement.
- HTTPS for production communication.
- Secure secret storage.
- Short-lived or revocation-aware access credentials.
- Request validation.
- Rate limiting.
- Atomic quota accounting.
- Idempotent request processing.
- Prompt-injection resistance.
- Output validation.
- Privacy-safe logging.
- Provider timeout and retry controls.
- Server-side control of model configuration.
- Protection against unauthorised direct Worker access.
- Protection against cross-account data leakage.

Android UI restrictions are not security controls. The backend must independently enforce all access decisions.

Production deployment must not rely on cleartext HTTP.

## 14. Existing Implementation Migration

The current repository already contains an online Gemini integration through a Cloudflare Worker.

Codex must inspect the existing implementation before changing it.

The migration must:

1. Preserve existing application functionality.
2. Identify all current Android-to-Worker calls.
3. Identify the existing authentication mechanism.
4. Determine how the APK-embedded shared token is used.
5. Identify the current device-ID assumptions.
6. Verify deployed Worker configuration against source code.
7. Identify current quota and rate-limit behaviour.
8. Introduce Express as the authoritative AI access gateway.
9. Implement secure Express-to-Worker authentication.
10. Remove or neutralise any unauthorised direct client access path.
11. Introduce transactional quota reservation and settlement.
12. Apply the approved knowledge retrieval and privacy rules.
13. Test all existing online and offline AI flows.

The migration must not remove the offline Qwen integration or unnecessarily replace the existing AI user interface.

Codex must document any temporary compatibility mechanism required during the migration and remove it when no longer necessary.

## 15. Testing and Acceptance Criteria

The online AI implementation is not complete merely because Gemini returns a response.

It must pass the following tests.

### 15.1 Authentication and membership

- Public users cannot access online AI.
- Registered but unverified users cannot access online AI.
- Verified members can access online AI when otherwise eligible.
- Revoked membership verification prevents new online requests.
- Client-supplied roles and membership labels cannot bypass backend verification.
- A user cannot access another user's account or conversation context.

### 15.2 Permissions

- Role labels alone do not grant confidential permissions.
- The backend enforces assigned permissions.
- Permission changes are respected.
- Direct Worker invocation cannot bypass Express authorisation.

### 15.3 Quotas and cost

- Requests are blocked when the applicable quota is exhausted.
- Concurrent requests cannot exceed the authoritative allowance.
- Repeated request identifiers do not create duplicate charges.
- Provider timeouts and retries are accounted for correctly.
- Global spending limits stop new provider requests when reached.
- Usage records can be reconciled against provider usage information.

### 15.4 Scope and safety

- Unrelated questions are rejected or redirected according to the scope policy.
- Sensitive pastoral information is not sent to Gemini.
- Emergency safety handling does not depend on Gemini.
- Prompt injection inside user content or retrieved documents cannot override trusted instructions.
- Clear approved Perazim doctrine is represented faithfully.
- Unapproved external material cannot be presented as official church doctrine.

### 15.5 Knowledge and citations

- The service retrieves only authorised approved knowledge.
- Withdrawn documents are not used as current authority.
- Restricted content is not exposed to unauthorised accounts.
- Biblical quotations identify the correct translation.
- Source references correspond to actual retrieved material.
- The model does not invent citations or church policies.
- External research is clearly distinguished from approved Perazim knowledge.

### 15.6 Privacy and retention

- Ordinary conversation transcripts are not persistently stored.
- Sensitive prompts and responses do not appear in ordinary logs.
- No ordinary conversation is automatically used for model training.
- No private pastoral content enters shared caches.
- Account-level usage records do not expose conversation content.

### 15.7 Failure and regression

- Online AI failure does not crash the Android application.
- Offline AI remains available when permitted.
- The user is told which mode produced the answer.
- Provider errors do not create false success messages.
- Existing Bible, community, messaging, media, classroom and other application features continue working.

## 16. Deployment Requirements

Before production release, the project must verify:

- Production HTTPS configuration.
- Express and Worker authentication.
- Gemini credentials and secret storage.
- Provider model and API configuration.
- Database backups and recovery.
- Atomic quota accounting.
- Global spending controls.
- Membership verification enforcement.
- Privacy-safe logging.
- Sensitive pastoral data restrictions.
- Monitoring and incident handling.
- Migration and rollback procedures.

No production deployment should occur until the relevant security, privacy, cost and acceptance tests have passed.

## 17. Explicitly Out of Scope for the Pilot

The following are not authorised by this specification:

- Sending identifiable sensitive pastoral disclosures to Gemini.
- Permanent AI conversation history.
- Automatic model training using member conversations.
- Automatic publication of Gemini answers into the church knowledge base.
- Gemini making official doctrinal decisions.
- Gemini changing membership status.
- Gemini granting permissions.
- Direct unrestricted Android access to the provider API.
- Client-controlled quota accounting.
- Unrestricted access to private membership or attendance records.
- Automatic use of external research as official Perazim doctrine.
- Fine-tuning a model on church or member conversations without a separately approved policy.

## 18. Implementation Authority

This document defines the intended online AI behaviour and technical requirements.

Codex must inspect the existing repository and deployment configuration, identify reusable components and implementation gaps, and propose a safe migration consistent with the complete specification set.

Codex must not interpret this document as permission to begin implementation before the project owner provides the final master implementation instruction.

Any conflict between technical convenience and an approved institutional, doctrinal, security or privacy requirement must be reported rather than silently resolved by weakening the requirement.
