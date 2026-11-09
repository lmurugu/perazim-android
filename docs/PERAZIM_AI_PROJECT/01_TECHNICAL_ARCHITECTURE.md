# PERAZIM MISSIONS CHURCH INTERNATIONAL
# AI PROJECT

## Document 01: Technical Architecture and System Integration

**Document number:** 01  
**Document status:** Foundational technical specification  
**Project:** Perazim Missions Church International AI Assistant  
**Target platform:** Existing native Android application  
**Implementation agent:** OpenAI Codex  
**Specification authority:** Project owner  
**Repository:** Existing Perazim Android application repository  
**Specification directory:** `docs/PERAZIM_AI_PROJECT/`  
**Related document:** `00_PROJECT_OVERVIEW.md`

---

# 1. PURPOSE OF THIS DOCUMENT

This document defines the technical architecture through which the Perazim AI assistant will operate within the existing Android application.

It establishes the responsibilities and relationships between:

- The Android application.
- The offline Qwen language model.
- Local knowledge storage and retrieval.
- The online AI service.
- The Gemini model.
- The Cloudflare Worker.
- The Node.js/Express backend.
- The PostgreSQL database.
- The knowledge management and publication system.
- Authentication and permission services.
- Attendance and pastoral escalation services.

The purpose is to establish one coherent architecture rather than implementing disconnected AI features.

Codex must use this document together with the other project specifications. It must not interpret this document as permission to replace existing application components without first establishing a technical necessity.

# 2. EXISTING TECHNICAL ENVIRONMENT

The following architecture was identified during the existing repository inspection.

Codex must verify these details against the current repository before implementation, because the codebase may change.

## 2.1 Android application

The existing application is primarily developed using:

- Java.
- Android SDK.
- Android Views.
- Gradle.
- Room persistence library.
- SQLite.

The inspected configuration indicated:

- Minimum Android SDK: 24.
- Target Android SDK: 34.

The Android application is responsible for the user interface, local application functionality, local AI execution, local knowledge access, authentication interactions and communication with backend services.

The existing application contains numerous features beyond AI. These must remain functional.

## 2.2 Existing offline AI

The application already includes:

**Model:** Qwen2.5 0.5B Instruct  
**Format:** GGUF  
**Quantisation:** Q4_K_M  
**Runtime:** Existing ARM64 llama.cpp integration.

Relevant existing components include:

- `OnDeviceAi.java`
- `LocalPrompt.java`
- `PerazimAiEngine.java`

The existing local model must be reused and improved rather than replaced without a documented performance or compatibility justification.

## 2.3 Existing online AI

The application already communicates with a Cloudflare Worker, which relays requests to Gemini.

Relevant components include:

- `AiRelayClient.java`
- `worker.js`

The inspected Worker source referenced a Gemini model configuration identified as `gemini-3.5-flash-lite`.

Codex must verify the actual deployed Worker configuration, available Gemini model, API compatibility, provider limits and deployment environment before relying on that value.

The Android application must not contain permanent Gemini API credentials.

## 2.4 Existing backend

The application has a Node.js/Express backend with PostgreSQL.

Existing authentication and API infrastructure should be reused where appropriate.

Relevant areas include:

- Authentication routes.
- JWT authentication.
- PIN/password-related authentication mechanisms.
- Authentication middleware.
- API client infrastructure.
- Database schema and migrations.

The backend will become the authoritative enforcement point for online AI access, membership verification, permissions, quotas and protected server-side operations.

# 3. TARGET SYSTEM ARCHITECTURE

The target architecture must preserve the existing technology stack while establishing clear boundaries between the Android application, local AI, online AI and institutional data.

## 3.1 High-level architecture

The intended architecture is:

```text
                    PERAZIM ANDROID APP
                             |
              +--------------+--------------+
              |                             |
         OFFLINE MODE                  ONLINE MODE
              |                             |
       Local AI Orchestrator          Authenticated API
              |                             |
       Scope and Safety Checks       Express AI Gateway
              |                             |
       Local Knowledge Retrieval     Identity Verification
              |                      Permission Checks
       Local Approved Knowledge      Quota Enforcement
              |                      Privacy and Safety
       Scripture Retrieval                  |
              |                      Approved Retrieval
       Qwen2.5 Local Model                  |
              |                             |
       Local Response                Cloudflare Worker
                                            |
                                         Gemini
                                            |
                                      AI Response
                                            |
                                  Server-side Validation
                                            |
                                    Android Response
```

The diagram represents the logical flow. Codex may refine the internal implementation after repository inspection, but must preserve the security and responsibility boundaries defined here.

## 3.2 Android application responsibilities

The Android application must provide:

1. The AI conversation interface.
2. Offline and online mode selection.
3. Authentication state and account presentation.
4. Local execution of Qwen.
5. Local approved knowledge storage and retrieval.
6. Local Scripture retrieval.
7. Input validation and preliminary sensitivity detection.
8. Communication with authenticated backend endpoints.
9. Display of references, citations and relevant limitations.
10. Offline update handling for approved knowledge packages.
11. Clear notification of connectivity and submission status.
12. User consent for appropriate escalation operations.

The Android application may perform preliminary checks for user experience, but client-side checks must never replace server-side security enforcement.

A modified Android client must not be able to grant itself verified membership, administrator privileges, additional quotas or access to restricted knowledge.

# 4. OFFLINE AI ARCHITECTURE

## 4.1 Operating principle

Offline AI must operate without requiring communication with the backend or an external model provider.

The local system should combine:

- Qwen2.5 local generation.
- Local AI orchestration.
- Local approved knowledge.
- Local Scripture retrieval.
- Deterministic approved responses.
- Appropriate local response validation.

The local model must not operate in isolation when relevant approved information is available.

## 4.2 Offline processing flow

The intended flow is:

```text
User Question
      |
Input Validation
      |
Scope and Safety Classification
      |
Sensitive Information Assessment
      |
Knowledge Retrieval
      |
Scripture Retrieval
      |
Prompt Construction
      |
Qwen Local Generation
      |
Response Validation
      |
Display Answer
```

The local orchestrator must determine which information is relevant before invoking the model.

It should avoid loading the entire knowledge base into every prompt.

## 4.3 Local knowledge database

Room and SQLite should be used for locally available knowledge.

The implementation should introduce appropriate entities and relationships for:

- Knowledge documents.
- Knowledge revisions.
- Knowledge chunks.
- Source references.
- Authority classifications.
- Approval status.
- Audience permissions.
- Package manifests.
- Publication versions.
- Expiry information.
- Update checkpoints.

These are target data structures. Codex must inspect the existing database schema and determine the safest incremental migration design.

Local knowledge must be separated according to its approved audience.

Publicly available content may be distributed to all users.

Restricted content may only be distributed to appropriately authorised accounts, subject to the approved offline access policy.

## 4.4 Offline limitations

The system must acknowledge that an offline device cannot receive immediate revocation instructions while disconnected.

Restricted offline packages must therefore have bounded validity, appropriate expiry and secure update handling.

The application must not claim that an offline device has received the latest knowledge or permission changes unless successful verification has occurred.

When a package expires or cannot be verified, the application must restrict access according to the package's security classification.

## 4.5 Local model limitations

The existing Qwen model is relatively small.

Codex must evaluate:

- Response quality.
- Memory requirements.
- Device compatibility.
- Inference speed.
- Context-window limitations.
- Multi-turn behaviour.
- Cancellation.
- Error handling.
- Battery and thermal impact.
- Retrieval quality.

The application must not imply that the local model has the same capabilities as the online Gemini service.

Where the model cannot answer reliably, it must communicate its limitations and, where appropriate, indicate that an online answer or human assistance may be needed.

# 5. ONLINE AI ARCHITECTURE

## 5.1 Operating principle

Online AI will use Gemini, but Android must not communicate with Gemini using embedded permanent provider credentials.

The Express backend must be the authoritative gateway for online AI requests.

The target request path is:

```text
Android Application
        |
        |
Authenticated Express API
        |
        +-- Account Verification
        |
        +-- Verified Membership Check
        |
        +-- Permission Check
        |
        +-- Scope and Safety Checks
        |
        +-- Sensitive Information Protection
        |
        +-- Quota Reservation
        |
        +-- Approved Knowledge Retrieval
        |
        +-- Prompt Construction
        |
        |
Authenticated Server-to-Worker Request
        |
Cloudflare Worker
        |
Gemini API
        |
Cloudflare Worker
        |
Express Response Validation
        |
Quota Settlement and Audit
        |
Android Application
```

This architecture makes Express responsible for access control and policy enforcement.

The Cloudflare Worker remains responsible for its designated provider-relay function.

## 5.2 Express responsibilities

Express must enforce:

- Authentication.
- Verified membership.
- Explicit permissions.
- AI access restrictions.
- Request validation.
- Scope restrictions.
- Sensitive-information protection.
- Usage quotas.
- Request accounting.
- Approved knowledge retrieval.
- Response processing.
- Error handling.
- Appropriate audit records.

The server must not trust a client-supplied declaration that a user is a verified member.

Membership and permissions must be obtained from authoritative server-side records.

## 5.3 Cloudflare Worker responsibilities

The Worker should:

1. Accept authenticated requests from the authorised server-side gateway.
2. Validate the server-to-Worker authentication mechanism.
3. Apply appropriate request validation.
4. communicate with Gemini using server-side provider credentials.
5. Apply provider timeout and error handling.
6. Return a controlled response to Express.
7. Avoid retaining unnecessary sensitive request content.

The Worker must not become an alternative public endpoint through which unauthorised Android clients can bypass Express.

Permanent application-wide shared secrets must not be embedded in the Android APK.

The final authentication mechanism between Express and Cloudflare must be selected and documented by Codex. It must provide server-to-server authentication and protection against replay or unauthorised invocation.

## 5.4 Gemini responsibilities

Gemini provides online generative reasoning.

It must receive only the information necessary to answer the authorised request.

Its context must be constructed from:

- The applicable AI behaviour instructions.
- Relevant approved knowledge.
- Relevant Scripture references or permitted excerpts.
- Necessary conversation context for the current temporary session.
- Relevant safety and response constraints.

The model must not receive unrestricted database records, the complete membership register, attendance records, private pastoral case records or administrative credentials.

Sensitive or identifiable pastoral information must not be transmitted to Gemini during the pilot.

# 6. AI ORCHESTRATION LAYER

The system must use a common conceptual orchestration policy for offline and online responses.

The orchestration layer determines how a question should be handled.

It must not assume that every question requires generative AI.

## 6.1 Request classification

The system should classify incoming requests into appropriate categories, including:

- Church identity and institutional information.
- Scripture and biblical interpretation.
- Official Perazim doctrine.
- Christian theology.
- Apologetics.
- Discipleship.
- Christian living.
- Pastoral matters.
- Church ministry and programs.
- Application assistance.
- Current external research.

The categories must support the scope restrictions established in the AI behaviour specification.

## 6.2 Processing methods

Depending on the request, the system may use:

1. A deterministic approved response.
2. An exact approved-answer cache.
3. Approved knowledge retrieval.
4. Scripture retrieval.
5. Local Qwen generation.
6. Online Gemini generation.
7. A human escalation pathway.
8. An honest limitation or referral.

The order is not a rigid universal sequence.

For example, an emergency safety concern must be recognised before ordinary caching or model generation.

Likewise, a request for an exact church service time may be better answered from an approved institutional record than by invoking a generative model.

## 6.3 Common response policy

Offline and online AI must follow the same fundamental institutional, theological, privacy and safety rules.

Differences in model capability must not create differences in church authority.

Qwen and Gemini must not be allowed to establish or amend official doctrine independently.

# 7. DATABASE RESPONSIBILITIES

## 7.1 Room and SQLite

Room is responsible for appropriate local application data.

It should hold:

- Locally approved knowledge packages.
- Local knowledge indexes.
- Local Scripture data where licensing permits.
- Package version information.
- Local update checkpoints.
- Appropriate non-sensitive configuration.
- Attendance outbox records where required.
- Escalation outbox records where required and protected.

It must not be treated as the authoritative source for server-side membership verification, online quotas or administrator permissions.

Locally stored permission information may support interface presentation and offline restrictions, but server-side access decisions must remain authoritative.

## 7.2 PostgreSQL

PostgreSQL is the authoritative server-side database.

It should hold the records required for:

- User accounts.
- Membership verification.
- Permission assignments.
- Knowledge documents and revisions.
- Knowledge approval and publication.
- Package publication records.
- Online AI usage and quota accounting.
- Approved exact-answer cache.
- Protected escalation records.
- Attendance sessions and events.
- Attendance recognition.
- Administrative audit history.

The final schema must be designed against the actual existing PostgreSQL schema, not created as a disconnected second database.

## 7.3 Data separation

The implementation must preserve logical and permission-based separation between:

1. General AI knowledge.
2. Membership verification information.
3. Attendance records.
4. Ordinary temporary AI interactions.
5. Sensitive pastoral escalation records.
6. AI usage and security audit records.

Attendance and confidential pastoral information must never become ordinary AI retrieval material.

# 8. KNOWLEDGE MANAGEMENT AND DISTRIBUTION

The project requires a controlled knowledge lifecycle.

The intended flow is:

```text
Authorised Source
       |
Knowledge Ingestion
       |
Classification
       |
Technical Review
       |
Approval
       |
Versioning
       |
Publication
       |
Signed Package Generation
       |
Audience-specific Distribution
       |
Android Synchronisation
       |
Local Validation
       |
Activation
```

During the pilot, the project owner is the sole authorised technical administrator for knowledge approval and publication.

Formal doctrinal changes remain subject to the church's authorised governance process.

Knowledge packages must be versioned and verifiable.

The Android application must reject packages with invalid signatures, invalid hashes, unsupported schemas or other integrity failures.

An update must not destroy the currently working package before the replacement has been successfully validated.

The system should support rollback to the last valid package where technically appropriate.

# 9. SECURITY ARCHITECTURE

Security must be implemented at the appropriate system boundary.

The following rules are mandatory:

**Authentication:** User identity must be established through the existing secure authentication infrastructure, with any necessary improvements.

**Membership:** Registration must not automatically confer verified membership.

**Permissions:** Access must be determined through explicit server-side permission checks.

**Provider credentials:** Gemini credentials must remain on the server side.

**Signing keys:** Private package-signing keys must not be stored in the Android application.

**Quotas:** Authoritative online usage accounting must use transactional server-side records.

**Transport:** Production communication must use HTTPS.

**Logs:** Logging must avoid unnecessary conversation content, credentials, personal information and sensitive pastoral information.

**Database migrations:** Existing user data must be preserved through incremental migrations.

**API security:** Every protected endpoint must validate identity, permissions and relevant request constraints.

**Cache security:** Shared caches must not expose private information or return content to users outside the permitted audience.

Codex must conduct a security review before production deployment.

# 10. QUOTA AND COST MANAGEMENT

The existing quota approach must be evaluated and improved.

PostgreSQL should provide authoritative transactional quota accounting.

The implementation should distinguish:

- Number of user requests.
- Input tokens.
- Output tokens.
- Provider usage.
- Estimated or reported provider cost.
- Failed requests.
- Retried requests.
- Reserved quota.
- Settled quota.

Quota reservations and settlements must be idempotent so that network retries or duplicate submissions do not incorrectly charge users multiple times.

Cloudflare KV must not be used as the authoritative atomic quota counter.

Hard spending limits and administrative usage controls must be available.

The final numerical limits remain a configuration dependency and must not be invented by Codex.

# 11. API AND SERVICE BOUNDARIES

The following API families are anticipated:

| Service area | Responsibility |
|---|---|
| Authentication | Account identity and session validation |
| AI | Online AI requests and access checks |
| Knowledge | Approved knowledge retrieval and package distribution |
| Administration | Knowledge approval, publication and configuration |
| Membership | Membership verification and status management |
| Permissions | Explicit permission assignments and checks |
| Escalation | Protected pastoral and high-risk human follow-up |
| Attendance | QR sessions, scan events and attendance recognition |
| Usage | Quota and provider-cost accounting |

These are logical service boundaries, not an instruction to create unnecessary microservices.

The implementation should use the existing Express application and its established conventions unless repository evidence establishes a compelling reason otherwise.

Detailed endpoint definitions, payloads, response contracts and database structures will be specified in Document 13.

# 12. SYNCHRONISATION AND OFFLINE PACKAGE UPDATES

Knowledge synchronisation must work reliably under intermittent connectivity.

The system must support:

- Checking for a newer approved package.
- Downloading only required updates where practical.
- Validating package signatures and hashes.
- Checking version compatibility.
- Handling interrupted downloads.
- Preventing partial activation.
- Activating only validated packages.
- Retaining a valid previous package for rollback where appropriate.
- Reporting the actual synchronisation state.

The application must distinguish:

- Update available.
- Downloading.
- Download interrupted.
- Validation failed.
- Update ready.
- Update activated.
- Synchronisation complete.
- Synchronisation pending.

A device must not claim to have synchronised successfully when it has only downloaded an unvalidated file.

# 13. ATTENDANCE AND AI SEPARATION

Attendance is a separate system capability.

The attendance system may share the Android application and backend infrastructure, but its records and authorisation rules must remain distinct from AI.

The AI must not use attendance records to:

- Determine whether a person is spiritually faithful.
- Infer spiritual maturity.
- Automatically discipline a member.
- Suspend AI access.
- Change membership verification status.

Attendance recognition must follow the approved QR validation policy.

The technical implementation and data contracts will be detailed in Document 11.

# 14. EXISTING APPLICATION PRESERVATION

Codex must preserve all existing unrelated application features.

These include, but are not limited to:

- Community and private messaging.
- Media and image functionality.
- Classes and educational features.
- Bible reader.
- Worship and audio functionality.
- Existing local application features.
- Existing account and authentication functions.

The AI project must not introduce regressions in these areas.

Before implementation, Codex must identify the relevant dependencies and tests.

After implementation, it must run appropriate regression tests and report any tests that could not be executed.

# 15. DATABASE MIGRATION AND DATA INTEGRITY

The existing Room database is at version 6 according to the inspected repository.

The inspected implementation also contained a destructive migration fallback and disabled schema export.

These conditions must be addressed carefully.

Codex must:

1. Identify every supported installed database version.
2. Inspect the existing migration history.
3. Enable appropriate Room schema export and version control.
4. Design incremental migration paths.
5. Preserve existing user data.
6. Test migration paths using realistic database fixtures.
7. Align existing database-generation utilities with the final schema.
8. Use versioned PostgreSQL migrations.
9. Document rollback and recovery limitations.

The destructive migration fallback must not be removed until all required upgrade paths are proven, but it must not remain as the final data-preservation strategy.

No migration may silently erase existing user information.

# 16. DEPLOYMENT AND ENVIRONMENT MANAGEMENT

Development, testing and production environments must be distinguished.

Codex must identify:

- Android development configuration.
- Backend development configuration.
- PostgreSQL development and production databases.
- Cloudflare Worker environments.
- Gemini provider configuration.
- Authentication secrets.
- Signing-key storage.
- Production HTTPS configuration.
- Backup and recovery procedures.

Development credentials must not be reused as production credentials.

The implementation must not expose secrets in source code, APK resources, logs or committed configuration files.

Production deployment must remain separate from local implementation and testing.

No live production migration or deployment may occur without explicit authorisation.

# 17. TECHNICAL DECISIONS AND DEFAULTS

The following architecture choices are established for the project:

| Area | Required direction |
|---|---|
| Android application | Preserve existing native Java application |
| Local database | Preserve and extend Room/SQLite |
| Offline AI | Reuse existing Qwen2.5 integration |
| Backend | Preserve and extend Node.js/Express |
| Server database | Preserve and extend PostgreSQL |
| Online AI | Gemini through Cloudflare Worker |
| Online gateway | Express is authoritative |
| AI history | No persistent ordinary conversation history during pilot |
| Sensitive pastoral data | Must not be sent to Gemini during pilot |
| Membership | Server-verified membership workflow |
| Permissions | Explicit server-side enforcement |
| Quotas | PostgreSQL transactional accounting |
| Knowledge | Approved, versioned and audience-controlled |
| Offline updates | Signed and validated packages |
| Attendance | Separate service and records |
| Python | Development and evaluation tools only |

# 18. TECHNICAL MATTERS REQUIRING VERIFICATION

The following are implementation dependencies, not reasons to redesign the architecture.

Codex must verify:

1. The actual deployed Cloudflare Worker configuration and its parity with repository source.
2. The currently supported Gemini model and API version.
3. The authentication method for Express-to-Worker communication.
4. The exact existing Room migration paths and supported installed versions.
5. The existing PostgreSQL schema and migration conventions.
6. The storage and security implications of the local Qwen model.
7. The exact Bible translation datasets and applicable redistribution licences.
8. ESV licensing and permitted offline storage, quotation, caching and processing.
9. The existing production HTTPS and backup arrangements.
10. The appropriate package-signing and key-rotation design.
11. The feasibility of reliable offline package expiry and revocation.
12. The hosting and recovery requirements for the expanded backend.

Codex must document findings and recommend the safest implementation.

It must not fabricate verification results or treat an unverified assumption as an established fact.

# 19. IMPLEMENTATION RULE

This document defines the target technical architecture.

It does not independently authorise implementation.

Codex must wait until the complete project documentation set and the final master implementation instruction are supplied.

After receiving the complete set, Codex must:

- Read all documents.
- Reconcile technical dependencies.
- Identify contradictions.
- Inspect the current repository.
- Produce an implementation plan.
- Identify migration and security risks.
- Confirm the implementation sequence.
- Wait for the final implementation authorisation.

Any proposed departure from an approved architecture or product decision must be documented and submitted for approval before implementation.

---

**END OF DOCUMENT 01**
