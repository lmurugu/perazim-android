# 12 — CODEX IMPLEMENTATION GUIDE

## 1. Purpose

This document defines how Codex must inspect, plan, implement, test, document, and report changes for the Perazim Missions Church International AI project.

Codex is the software implementation agent. It is responsible for translating the approved project specifications into working software within the existing repository.

Codex must not independently redefine the product, invent church policy, change approved theological positions, or replace established architecture without justification and authorisation.

The primary objective is to extend the existing application safely and incrementally.

## 2. Authority and Responsibilities

The project uses three distinct forms of work.

### 2.1 ChatGPT — Architecture and specification

ChatGPT is responsible for assisting with:

- Product requirements.
- System architecture.
- AI behaviour specifications.
- Theological and institutional knowledge rules.
- Security and privacy requirements.
- API and database design.
- Implementation sequencing.
- Testing and acceptance criteria.
- Reviewing Codex reports and proposed changes.
- Preparing the final implementation instructions.

ChatGPT does not independently modify the user's local repository.

### 2.2 NotebookLM — Source document analysis

NotebookLM may be used to analyse and cross-reference source documents, including:

- The Perazim Constitution.
- Official church policies.
- Statements of Faith.
- Approved teaching materials.
- Historical and institutional documents.

NotebookLM is a source-analysis assistant, not the software implementation authority.

Its findings must be checked against the actual source documents and the approved knowledge-authority framework.

### 2.3 Codex — Repository inspection and implementation

Codex is responsible for:

- Inspecting the actual codebase.
- Identifying existing reusable components.
- Reporting implementation gaps.
- Identifying technical risks and dependencies.
- Implementing approved changes.
- Writing and updating tests.
- Running builds and relevant test suites.
- Reporting actual results.
- Maintaining technical documentation.

Codex must not treat its own technical recommendations as automatically approved product or church policy decisions.

## 3. Specification Folder

The authoritative project specification folder is:

`/home/murugu/Android/app/docs/PERAZIM_AI_PROJECT/`

Codex must read the complete contents of this folder before beginning implementation.

The documents are intended to work together. A requirement in one document may depend on a decision, constraint, or definition in another.

Codex must not read one document in isolation and assume it represents the entire project.

The final master implementation instruction will identify the complete set of authoritative documents and authorise the implementation phase.

If Codex cannot access the folder or a particular document, it must report the exact problem. It must not pretend to have read missing documentation.

## 4. Mandatory Pre-Implementation Inspection

Before changing source code, Codex must establish the actual state of the repository.

The inspection must cover:

### 4.1 Repository state

Record:

- Current branch.
- Current commit.
- Working-tree status.
- Existing uncommitted changes.
- Relevant project structure.
- Existing build configuration.
- Available test suites.
- Existing migration history.

Codex must not overwrite or discard unrelated user changes.

If the working tree contains user modifications, Codex must preserve them and identify any possible conflict with the proposed work.

### 4.2 Android application

Inspect the existing Java Android application, including:

- Application entry points.
- Main activity and navigation.
- AI interface and dialogs.
- Local AI engine.
- Qwen model integration.
- Room database.
- Entities and DAOs.
- Existing migrations.
- Network clients.
- Authentication integration.
- Background and synchronisation services.
- Notifications and permissions.
- Android manifest.
- Build configuration.

The inspection must identify which existing components can be extended and which genuinely require new components.

### 4.3 Backend

Inspect:

- Node.js and Express application structure.
- Existing routes and middleware.
- Authentication and authorisation.
- JWT handling.
- PIN or password verification.
- PostgreSQL schema.
- Existing migrations.
- Database access patterns.
- Rate limiting.
- Logging.
- Existing Cloudflare Worker integration.
- Environment and secret management.

Codex must identify the authoritative location for each security-sensitive decision.

### 4.4 AI implementation

Inspect:

- `PerazimAiEngine.java`
- `OnDeviceAi.java`
- `AiRelayClient.java`
- `LocalPrompt.java`
- `BibleContextExtractor.java`
- `worker.js`
- Existing programmed responses.
- Existing model configuration.
- Existing context construction.
- Existing online request flow.
- Existing offline request flow.

The inspection must establish actual runtime behaviour rather than relying solely on class names or comments.

### 4.5 Python toolkit

Inspect the existing Python scripts and their dependencies, particularly:

- `compile_bible_database.py`
- `gdrive_catalog_builder.py`
- `generate_background_assets.py`
- `capture_full_app.py`

Codex must reuse or extend appropriate existing functionality instead of creating duplicate tools without a demonstrated need.

## 5. Architecture Preservation

The approved direction is to preserve and extend the existing architecture.

The current stack is:

| Component | Existing technology |
|---|---|
| Android application | Native Java and Android SDK |
| Local database | Room and SQLite |
| Offline AI | Qwen2.5 0.5B Instruct through llama.cpp |
| Backend | Node.js and Express |
| Server database | PostgreSQL |
| Online AI | Gemini through Cloudflare Worker |
| Development toolkit | Python |

Codex must not introduce an unnecessary replacement architecture.

In particular:

- Do not rewrite the Android application in Kotlin, Flutter, or React Native merely for convenience.
- Do not replace Room with another local database without an approved architectural decision.
- Do not introduce FastAPI or another Python production backend.
- Do not replace PostgreSQL without an approved decision.
- Do not replace Qwen or Gemini merely because another model is available.
- Do not rebuild the entire application to implement the AI system.
- Do not duplicate existing authentication, messaging, navigation, or synchronisation infrastructure without first assessing reuse.

A proposed architectural change must explain the problem it solves, alternatives considered, migration impact, security implications, and maintenance cost.

Changes to the approved technology stack require explicit authorisation.

## 6. Implementation Must Be Incremental

The AI system must be implemented in manageable, verifiable stages.

Codex must avoid combining unrelated changes into one large modification.

Each stage should have:

1. A clearly defined objective.
2. A list of affected components.
3. Identified dependencies.
4. A technical implementation plan.
5. A security and privacy assessment.
6. Relevant tests.
7. A documented result.
8. A clear statement of what remains incomplete.

A stage must not be reported as complete merely because its code has been written.

The relevant build, tests, integration checks, and acceptance requirements must be satisfied.

## 7. Required Implementation Sequence

The following is the preferred implementation sequence. Codex may recommend a justified adjustment after repository inspection, but it must explain dependencies and obtain authorisation where the adjustment changes approved scope or policy.

### Phase 1 — Repository baseline and migration safety

Establish a reliable baseline before implementing new functionality.

Tasks include:

- Record the current repository state.
- Run appropriate existing tests.
- Confirm the current Android build.
- Confirm backend test status.
- Inspect Room and PostgreSQL migration paths.
- Enable appropriate Room schema export and version control.
- Establish supported upgrade paths.
- Prepare migration regression tests.
- Identify destructive migration behaviour.
- Establish backup and rollback requirements.

Do not perform destructive migrations.

Do not remove the existing destructive migration fallback until all supported upgrade paths have been implemented and verified.

### Phase 2 — Identity, membership verification, and permissions

Implement the distinction between:

- Registered account.
- Verified church member.
- Assigned application permissions.

Implement the approved membership verification workflow.

The backend must remain authoritative for membership status and permission enforcement.

The Android application may display membership and permission state, but it must not independently grant either.

Implement the necessary audit history for verification, correction, and revocation.

### Phase 3 — Secure online AI gateway

Establish the authoritative online request path.

The recommended direction is:

Android → Express → Cloudflare Worker → Gemini

Express must validate:

- Authentication.
- Verified membership.
- Required permissions.
- Request scope.
- Safety and privacy restrictions.
- Usage allowance.
- Request validity.

The Worker must authenticate the trusted backend request separately.

Remove reliance on permanent APK-embedded shared credentials as proof of user identity.

Codex must inspect the deployed Worker configuration and determine whether it matches the repository source before making production assumptions.

### Phase 4 — Usage accounting and cost controls

Implement reliable request and cost accounting.

Use PostgreSQL transactions and idempotent request handling for authoritative quota accounting.

Test concurrent requests, retries, provider failures, and interrupted requests.

Cloudflare KV must not be treated as an atomic authoritative counter where correctness depends on concurrent updates.

Establish hard usage and spending controls.

### Phase 5 — Knowledge ingestion and administration

Implement the knowledge-management lifecycle.

The system must support:

- Source registration.
- Document ingestion.
- Metadata validation.
- Revision tracking.
- Draft and review states.
- Approval.
- Publication.
- Replacement.
- Withdrawal.
- Historical traceability.

During the pilot, the user is the sole authorised technical administrator for knowledge approval, correction, replacement, withdrawal, publication, and AI configuration.

The system must preserve the distinction between technical content approval and ecclesiastical authority to amend doctrine.

### Phase 6 — Retrieval and shared AI orchestration

Implement the approved retrieval and routing architecture.

The system should use relevant deterministic responses, approved exact cache entries, approved knowledge retrieval, and the appropriate local or online model.

Retrieval must enforce source approval, audience restrictions, and permissions.

Start with exact-reference and suitable full-text retrieval. More advanced semantic retrieval may be introduced after evaluation demonstrates a clear benefit.

Do not allow client-supplied passages or arbitrary user content to override trusted system instructions.

### Phase 7 — Grounded offline and online answers

Integrate retrieved Scripture and approved knowledge into both Qwen and Gemini workflows.

The offline model must receive relevant evidence rather than generating from model weights alone.

The online model must receive only authorised, relevant, and appropriately screened context.

Both modes must apply the same approved identity, scope, theological, privacy, and safety principles.

### Phase 8 — Caching and optimisation

Implement approved exact-answer caching and safe retrieval optimisation.

Cache entries must carry appropriate source, revision, approval, expiry, and permission information.

Cache hits must never bypass scope, safety, or access checks.

Semantic similarity may initially assist retrieval. It must not automatically cause unrelated questions to receive the same previously generated answer.

### Phase 9 — Signed offline knowledge packages

Implement package generation, signing, distribution, validation, activation, and rollback.

Use asymmetric signatures and a documented key-management strategy.

Private signing keys must never be embedded in the Android APK or committed to source control.

Packages must be audience-specific and must not contain the unrestricted membership register, confidential pastoral cases, attendance records, or other prohibited data.

### Phase 10 — Pastoral escalation

Implement the separate consent-based escalation workflow.

Preserve the sensitive-information restrictions established in Document 09.

Test identified and anonymous submissions, offline pending states, backend receipt confirmation, permissions, and audit history.

Do not introduce permanent ordinary AI conversation history as part of this work.

### Phase 11 — Reminders and AI deep links

Implement local reminders and QR-based AI navigation as specified.

These features must remain independent of AI provider availability and must not grant membership or attendance status.

They may be developed alongside other phases if dependencies permit.

### Phase 12 — Attendance

Implement attendance only after membership verification, permissions, backend authentication, and the approved offline validation policy are in place.

The first version is limited to headquarters.

Implement the separate signed attendance QR workflow, 60-minute minimum recognition rule, offline pending events, and human review requirements.

Do not introduce BLE, NFC, continuous GPS, or branch attendance in Version 1.

### Phase 13 — Final security, regression, and release readiness

Run the complete acceptance suite.

Verify:

- Security.
- Privacy.
- Theological faithfulness.
- Retrieval and citations.
- Online and offline functionality.
- Membership and permissions.
- Quota accounting.
- Knowledge publication.
- Package integrity.
- Escalation.
- Attendance.
- Existing application regression.
- Backup and recovery.
- Device performance.

Prepare a final release-readiness report.

## 8. Coding Standards

Codex must follow the conventions already established in the repository unless a documented reason justifies improvement.

### 8.1 Android Java

- Use the existing application architecture.
- Reuse existing components where appropriate.
- Keep UI, data access, and business logic appropriately separated.
- Avoid putting complex AI orchestration directly inside Activities or Dialogs.
- Handle lifecycle changes and cancellation safely.
- Avoid blocking the main thread.
- Handle network and model failures explicitly.
- Preserve accessibility and existing navigation conventions.
- Avoid unnecessary third-party dependencies.

### 8.2 Backend JavaScript

- Follow existing Express conventions.
- Keep authentication and permission checks in reusable middleware or services.
- Validate all external inputs.
- Use parameterised database operations.
- Use transactions for operations requiring atomicity.
- Implement idempotency for retryable operations.
- Avoid exposing database internals to clients.
- Handle errors consistently.
- Never trust client-supplied identity, membership, permission, or quota values.

### 8.3 PostgreSQL

- Use version-controlled migrations.
- Preserve existing data.
- Add appropriate constraints and indexes.
- Use transactions where operations must succeed or fail together.
- Define foreign-key relationships deliberately.
- Avoid storing sensitive information without a documented need.
- Maintain audit records for sensitive administrative changes.
- Test backups and restores.

### 8.4 Python

- Reuse the existing Python environment and established project conventions.
- Keep toolkit functions modular.
- Validate inputs and metadata.
- Use reproducible processing steps.
- Preserve original source material.
- Report extraction and OCR uncertainty.
- Pin or otherwise manage dependencies appropriately.
- Never store secrets in scripts or configuration committed to source control.

## 9. Security Rules Codex Must Not Violate

The following are mandatory project constraints.

1. Android client-side checks are not authoritative security controls.
2. User-supplied role, membership status, device ID, and permission fields must not establish authority.
3. Provider credentials must not be embedded as permanent secrets in the APK.
4. Private package-signing keys must remain server-side or in an appropriately protected signing environment.
5. Sensitive pastoral content must not be sent to Gemini during the pilot.
6. Confidential attendance and escalation records must remain separate from ordinary AI retrieval and shared cache.
7. Ordinary AI conversation history must not become permanent stored chat history.
8. AI must not grant its own access permissions.
9. AI must not automatically approve or publish knowledge.
10. A knowledge package must be verified before activation.
11. A cache hit must not bypass security or safety checks.
12. Production communication must use HTTPS.
13. Logs must not expose secrets or sensitive personal content.
14. No hidden administrative account or backdoor may be introduced.
15. No AI-generated answer may automatically become official Perazim doctrine.

If an existing implementation violates one of these constraints, Codex must document the issue and propose a controlled correction.

## 10. Theological and Institutional Boundaries

Codex is implementing software, not exercising ecclesiastical authority.

Codex must preserve the approved authority hierarchy and doctrinal representation rules.

In particular:

- Scripture remains supreme.
- The Constitution and official Statement of Faith remain authoritative institutional sources subject to Scripture.
- Approved church documents must be distinguished from personal interpretations and external scholarship.
- Conflicting authoritative documents must be reported.
- Technical approval of knowledge content must not be treated as formal doctrinal amendment.
- The AI must faithfully represent clear official Perazim positions.
- The AI must not invent an institutional position where none exists.

If a requirement depends on an unresolved church policy decision, Codex must identify the dependency and avoid implementing an invented policy.

## 11. Handling Ambiguity and Conflicts

Codex must classify unresolved issues into three categories.

### 11.1 Technical implementation decisions

These concern how to implement an already approved requirement.

Examples:

- Choosing an appropriate database index.
- Selecting a suitable Java class structure.
- Choosing a supported signature format.
- Deciding how to organise a service layer.
- Selecting an appropriate test framework compatible with the repository.

Codex may recommend and implement a sound technical choice within the approved architecture, documenting important decisions.

### 11.2 Product decisions

These affect application behaviour, user experience, feature scope, or access rules.

Examples:

- Introducing a new AI mode.
- Changing access categories.
- Adding new administrative capabilities.
- Changing attendance recognition rules.
- Changing the retention of user information.

Codex must not silently decide these matters.

It must present the options, consequences, and recommendation for approval.

### 11.3 Church policy and doctrinal decisions

These affect doctrine, ecclesiastical authority, official teaching, or institutional governance.

Codex must not make these decisions.

It must identify the relevant source, explain the conflict or missing authority, and wait for an authorised decision.

Where a clear approved position already exists, Codex must implement it faithfully rather than reopening the decision unnecessarily.

## 12. Change Management

Before implementing a major change, Codex must identify:

- The requirement being implemented.
- The relevant specification document.
- The existing files affected.
- New files required.
- Database changes.
- API changes.
- Security implications.
- Compatibility risks.
- Tests required.
- Rollback considerations.

For small, clearly authorised changes, this analysis may be recorded concisely.

For architectural or data-sensitive changes, a more detailed plan is required.

Codex must avoid unrelated refactoring during feature implementation.

If refactoring is genuinely necessary, it must explain why and keep the change as narrow as practical.

## 13. File and Database Preservation

Codex must not:

- Delete existing application data to simplify migration.
- Replace an existing database with an empty database.
- Use destructive migration fallbacks as a normal upgrade strategy.
- Remove existing application features without approval.
- Overwrite user modifications.
- Delete source documents used for knowledge ingestion.
- Remove historical knowledge revisions without an approved retention basis.
- Commit generated credentials or secrets.
- Alter production data during development testing.

Before risky operations, Codex must establish the available recovery mechanism.

Any migration that could affect existing users must have a documented test and rollback strategy.

## 14. Build and Test Discipline

Codex must use the actual project build system and test commands.

Before changes, record the relevant baseline.

After changes:

- Run the appropriate build.
- Run relevant unit tests.
- Run relevant integration tests.
- Run migration tests where database structures changed.
- Run relevant security and permission tests.
- Run regression tests for affected existing features.
- Report all failures and skipped tests.

A successful compilation does not prove feature correctness.

A successful unit test does not prove integration correctness.

A successful emulator test does not prove actual device performance.

Where a test cannot be run, Codex must state why and identify the remaining risk.

## 15. Git and Version Control

Codex must preserve a clear, reviewable development history.

Before significant work, record the starting commit and working-tree state.

Changes should be logically grouped so they can be reviewed independently.

Codex must not:

- Force-push without explicit authorisation.
- Rewrite existing history without approval.
- Delete branches or tags without approval.
- Reset or discard uncommitted user changes.
- Commit secrets.
- Hide failed tests or incomplete work.

Commits may be created when authorised by the implementation workflow, but Codex must not assume permission to publish changes to a remote repository.

## 16. Documentation Requirements

Implementation must be accompanied by updated technical documentation.

Where applicable, Codex must document:

- New components and their responsibilities.
- API endpoints and authentication requirements.
- Database schema and migration changes.
- Knowledge package formats.
- Signing and verification procedures.
- Configuration variables.
- Environment requirements.
- Test commands.
- Deployment requirements.
- Known limitations.
- Recovery procedures.

Documentation must describe actual implemented behaviour, not merely planned behaviour.

If implementation differs from the approved specification, Codex must report the difference and obtain approval where necessary.

## 17. Mandatory Progress Reports

Codex must provide a structured report at the end of every major implementation phase.

Each report must contain:

### A. Phase identification

State the phase name, objective, and relevant specification documents.

### B. Work completed

Identify the actual functionality implemented.

### C. Files changed

List created, modified, and deleted files, with a brief explanation of each significant change.

### D. Database changes

Identify schema changes, migrations, data transformations, and validation results.

### E. API changes

Identify new or modified endpoints, request structures, response structures, and permission requirements.

### F. Testing

List commands run, results, skipped tests, and unresolved failures.

### G. Security and privacy

Explain the relevant controls implemented and tests performed.

### H. Known limitations

Identify incomplete features, external dependencies, and remaining risks.

### I. Next phase

Recommend the next implementation step and identify its prerequisites.

### J. Repository state

Report the current commit, working-tree status, and whether any deployment or infrastructure operation occurred.

Reports must distinguish actual implementation from planned work.

## 18. Prohibited Shortcuts

Codex must not use the following shortcuts to make implementation appear complete:

- Hardcoded administrator credentials.
- Permanent shared API secrets embedded in the APK.
- Client-controlled membership verification.
- Client-controlled quota accounting.
- Unauthorised access based only on role labels.
- Fake or placeholder citations presented as real.
- Unapproved documents inserted into published knowledge.
- Automatic doctrinal compromise.
- Sensitive pastoral data sent to Gemini for convenience.
- Permanent AI chat history introduced without approval.
- Unverified attendance automatically recognised.
- Destructive database migration used to avoid writing migrations.
- Disabled security checks to make tests pass.
- Suppressed exceptions that conceal failed operations.
- Test results fabricated or inferred from code inspection.

If a feature cannot be completed safely, Codex must report the limitation and propose an acceptable alternative.

## 19. External Services and Deployment

Codex must distinguish local code changes from changes to external infrastructure.

Changes involving the following require explicit authorisation unless the final master instruction specifically grants that authority:

- Production Cloudflare Worker deployment.
- Production Gemini configuration.
- Production database migrations.
- Production credentials.
- Domain or DNS configuration.
- Signing-key creation or rotation.
- Production package publication.
- Live notification delivery.
- Production membership verification.
- Real attendance records.
- Changes to live user permissions.

Development and test environments should be used for implementation and verification.

A successful local build must not be reported as a successful production deployment.

## 20. Completion Standard

A phase is complete only when:

1. The approved requirement has been implemented.
2. The implementation is integrated into the existing architecture.
3. Relevant tests have been executed.
4. Security and privacy requirements have been verified.
5. Existing functionality has been checked for regression.
6. Documentation has been updated.
7. Known limitations have been reported.
8. The phase report has been delivered.

A feature that has been coded but not tested must be described as implemented but unverified.

A feature blocked by an unresolved policy or external dependency must be described as blocked, not complete.

## 21. Implementation Authorisation Boundary

This document establishes Codex's implementation method. It does not, by itself, authorise Codex to begin implementation.

Codex must wait until:

1. The complete project specification set has been supplied.
2. The final master implementation instruction has been supplied.
3. The user has explicitly authorised implementation under that instruction.

Until then, Codex may perform only the repository inspection and documentation review specifically requested by the user.

It must not begin modifying application code, database schemas, production configuration, or infrastructure merely because this document describes how implementation should proceed.

## 22. Final Instruction to Codex

Treat the existing Perazim Android application as a functioning system that must be extended carefully.

Read the complete approved specification before implementation. Inspect the real repository before making technical assumptions. Reuse existing functionality where appropriate. Preserve user data and existing features. Enforce security on the backend. Test the complete system. Report honestly.

Your responsibility is to implement the approved Perazim AI system, not to redefine it.

**End of Document 12.**
