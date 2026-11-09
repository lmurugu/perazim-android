# OPEN DEPENDENCIES AND PRE-IMPLEMENTATION REQUIREMENTS

## Perazim Missions Church International AI Project

**Document:** OPEN_DEPENDENCIES.md  
**Status:** Project dependency register  
**Purpose:** Identify unresolved technical, institutional, legal, operational, and security matters that must be investigated or resolved during implementation planning.

---

# 1. PURPOSE

This document records matters that remain unresolved or require verification before particular parts of the Perazim AI system can be safely implemented, tested, or deployed.

It complements:

- `DECISIONS_REGISTER.md`
- `12_CODEX_IMPLEMENTATION_GUIDE.md`
- `08_SECURITY_AND_ACCESS_CONTROL.md`
- `09_PASTORAL_SAFETY_AND_ESCALATION.md`
- `13_MEMBERSHIP_AND_ADMINISTRATION.md`
- `14_ATTENDANCE_REMINDERS_AND_QR.md`
- `15_DATA_RETENTION_AND_PRIVACY.md`
- `16_API_AND_DATABASE_CONTRACTS.md`

Codex must read the complete specification folder before making an implementation plan.

This document is not permission to make unresolved policy decisions independently.

---

# 2. DEPENDENCY CLASSIFICATION

Every dependency must be assigned one of the following classifications.

| Classification | Meaning |
|---|---|
| TECHNICAL INVESTIGATION | Codex can investigate using repository inspection, testing, or technical documentation. |
| OWNER DECISION | Requires explicit approval from the project owner. |
| INSTITUTIONAL APPROVAL | Requires an authorised church governance or administrative decision. |
| LEGAL/LICENSING REVIEW | Requires verification of applicable law, licence terms, or contractual obligations. |
| EXTERNAL PROVIDER | Depends on an external provider, service, API, or infrastructure configuration. |
| SECURITY REVIEW | Requires threat analysis, security testing, or formal security approval. |
| OPERATIONAL DECISION | Requires confirmation of how the church intends to operate the feature. |

A dependency may have more than one classification.

Codex must not mark a dependency resolved merely because it has proposed an implementation.

Resolution requires evidence, approval, or both, as appropriate.

---

# 3. TECHNICAL DEPENDENCIES

## 3.1 Existing repository and database baseline

**ID:** D-001  
**Classification:** TECHNICAL INVESTIGATION

Codex must establish the actual repository state before changing code.

Required investigation:

- Confirm the current Git branch and commit.
- Confirm whether the repository has uncommitted changes.
- Identify all Android modules and build variants.
- Identify the current Room database version.
- Inspect every existing Room migration.
- Identify all supported installed database versions.
- Inspect PostgreSQL schema and migration history.
- Identify existing database backup and restoration procedures.
- Identify existing API authentication and authorisation middleware.
- Identify existing tests and their actual execution status.

The previous baseline inspection identified commit `6fe877c`. Codex must verify whether this remains the relevant baseline.

**Resolution evidence:** A repository inspection report containing the current commit, working-tree status, architecture, migration inventory, and test baseline.

## 3.2 Room migration safety

**ID:** D-002  
**Classification:** TECHNICAL INVESTIGATION / SECURITY REVIEW

The existing Room implementation was reported to have destructive migration fallback enabled and schema export disabled.

Codex must investigate:

- Existing production or user-installed database versions.
- Every migration path required to upgrade supported installations.
- Whether the Bible database is part of Room or maintained separately.
- Whether existing local data can survive the proposed changes.
- Whether migration tests cover realistic existing database states.

The final implementation must not rely on destructive migration as the normal upgrade path.

Codex must not remove the fallback until supported upgrade paths have been implemented and verified.

**Resolution evidence:** Migration tests demonstrating that supported existing installations can upgrade without losing protected user data.

## 3.3 PostgreSQL migration and recovery

**ID:** D-003  
**Classification:** TECHNICAL INVESTIGATION / SECURITY REVIEW

Codex must determine:

- How production PostgreSQL schema changes are currently managed.
- Whether migration scripts are versioned.
- Whether migrations are repeatable or safely recoverable.
- Whether backups exist.
- Whether restoration has been tested.
- Whether new tables and indexes can be introduced without disrupting existing application functions.

**Resolution evidence:** Versioned migration strategy, test results, and a documented recovery procedure.

## 3.4 Cross-service authentication

**ID:** D-004  
**Classification:** TECHNICAL INVESTIGATION / SECURITY REVIEW

The existing Android-to-Cloudflare Worker arrangement reportedly uses an APK-embedded shared token and client-supplied device identifiers.

These mechanisms must not be treated as proof of verified membership or administrative authority.

Codex must investigate the existing authentication architecture and implement a secure design in which:

- Express is the authoritative application identity and permission gateway.
- The backend verifies membership and permissions.
- The Worker receives authenticated server-to-server requests.
- Provider credentials remain outside the Android application.
- Revoked or expired access cannot continue through an old client token.
- Service-to-service authentication is independently verifiable.

**Preferred technical direction:** Android → Express → Cloudflare Worker → Gemini.

Codex may recommend a different compatible design only with evidence that it satisfies the approved security requirements.

**Resolution evidence:** Authentication flow documentation and tests proving that unauthorised Android clients cannot call protected online AI functions directly.

## 3.5 Quota accounting

**ID:** D-005  
**Classification:** TECHNICAL INVESTIGATION / OWNER DECISION

The existing Worker quota implementation was reported to use non-atomic KV counter operations that may fail open.

Codex must investigate and design:

- Atomic quota reservation.
- Idempotent request identification.
- Provider request settlement.
- Handling of timeouts and retries.
- Handling of duplicate requests.
- Usage accounting after interrupted responses.
- Daily and monthly quota enforcement.
- Provider cost monitoring.
- Hard spending limits.

The exact numerical quotas and financial ceiling remain unresolved.

Codex may implement configurable quota infrastructure but must not invent final financial limits.

**Resolution evidence:** Concurrency tests, retry tests, duplicate-request tests, and approved quota configuration.

## 3.6 AI retrieval and evidence grounding

**ID:** D-006  
**Classification:** TECHNICAL INVESTIGATION

The existing Bible retrieval and knowledge mechanisms must be inspected before designing replacements.

Codex must establish:

- Which translations are actually present.
- Whether the Bible corpus is complete.
- How passages are indexed.
- How the current retrieval process selects passages.
- Whether the existing KJV default can be changed safely.
- How church documents are currently imported.
- Whether source documents have stable identifiers and revision history.
- How retrieved evidence can be passed to Qwen and Gemini without allowing user-supplied text to become privileged instructions.

**Preferred initial approach:** Exact-reference retrieval and full-text search, with semantic retrieval considered later.

**Resolution evidence:** Retrieval tests demonstrating correct source selection, translation identification, passage references, and knowledge permission filtering.

## 3.7 Offline model integration

**ID:** D-007  
**Classification:** TECHNICAL INVESTIGATION

Codex must inspect:

- Current Qwen model version and quantisation.
- llama.cpp integration and supported device architectures.
- Model memory requirements.
- Inference performance on supported phones.
- Cancellation and timeout handling.
- Local context-window constraints.
- Existing prompt construction.
- Whether retrieved Scripture and approved church knowledge can be supplied reliably.
- Behaviour when the model cannot answer.

The implementation must not imply that an offline model has access to current internet information.

**Resolution evidence:** Tests on representative Android devices, including offline retrieval, response generation, cancellation, memory use, and insufficient-evidence behaviour.

## 3.8 Gemini and Cloudflare configuration

**ID:** D-008  
**Classification:** EXTERNAL PROVIDER / TECHNICAL INVESTIGATION

The previous repository report identified a Worker source configuration referring to `gemini-3.5-flash-lite`. The actual deployed model and configuration were not verified.

Codex must establish:

- Current Worker source configuration.
- Actual deployed environment.
- Provider model availability.
- API authentication method.
- Rate limits and service restrictions.
- Provider retention and processing terms.
- Error handling and timeout behaviour.
- Whether development and production configurations differ.

No claim about the deployed provider configuration should be made without evidence.

**Resolution evidence:** Verified source configuration, deployment configuration evidence available to the project, and provider documentation.

---

# 4. PRIVACY, RETENTION, AND LEGAL DEPENDENCIES

## 4.1 Data retention periods

**ID:** D-009  
**Classification:** OWNER DECISION / INSTITUTIONAL APPROVAL / LEGAL REVIEW

The following records require approved retention rules:

- Membership verification records.
- Attendance records.
- Pastoral escalation cases.
- Administrative audit records.
- AI usage and security metadata.
- Account and authentication records.
- Knowledge approval history.
- Package publication history.

The project must not adopt arbitrary indefinite retention or invent numerical retention periods.

Codex should design configurable retention controls, including:

- Record category.
- Purpose.
- Retention start event.
- Retention period.
- Authorised deletion or archival process.
- Legal or safeguarding hold.
- Deletion audit requirements.

**Resolution evidence:** An approved retention schedule and tested implementation.

## 4.2 Privacy notice and user rights

**ID:** D-010  
**Classification:** INSTITUTIONAL APPROVAL / LEGAL REVIEW

Before production deployment, the church must approve an appropriate privacy notice explaining:

- What personal data is collected.
- Why it is collected.
- Which services process it.
- Whether information is transferred outside Kenya.
- Which records are not retained.
- How membership and attendance data are used.
- How pastoral escalation works.
- How users can exercise applicable rights.
- How privacy concerns can be raised.

The notice must accurately reflect the actual implementation.

Codex must not write a privacy notice that promises protections the system does not provide.

**Resolution evidence:** Approved privacy notice and implementation review.

## 4.3 Kenyan data protection obligations

**ID:** D-011  
**Classification:** LEGAL REVIEW

The project must be assessed against applicable Kenyan data protection requirements, including the Data Protection Act, 2019 and relevant regulations.

Review should consider:

- Controller and processor responsibilities.
- Lawful basis for processing.
- Sensitive personal data.
- Data subject rights.
- Data minimisation.
- Security safeguards.
- Data transfers.
- Retention and deletion.
- Breach response.
- Whether registration or other obligations apply to the church or relevant service providers.
- Whether a data protection impact assessment is required.

Codex must not provide a legal determination merely by implementing technical controls.

**Resolution evidence:** Documented legal review and approved compliance actions.

## 4.4 External AI provider processing

**ID:** D-012  
**Classification:** EXTERNAL PROVIDER / LEGAL REVIEW / SECURITY REVIEW

Before production use, verify the actual provider terms and processing arrangements for Gemini and Cloudflare.

The review must establish:

- What information the provider receives.
- Whether prompts or outputs are retained.
- Whether submitted information is used for model improvement.
- Processing locations and cross-border implications.
- Available data protection agreements.
- Provider security commitments.
- Relevant service-tier differences.
- Whether the intended church use is permitted.

The approved prohibition on sending identifiable or sensitive pastoral information to Gemini during the pilot remains binding regardless of provider assurances.

**Resolution evidence:** Current provider documentation, contractual review where applicable, and tested application-side restrictions.

## 4.5 ESV and other Bible translation licensing

**ID:** D-013  
**Classification:** LEGAL/LICENSING REVIEW

The ESV is the preferred translation, but the project must verify the rights applicable to its intended use.

Investigate:

- Offline storage.
- Local database distribution.
- Search and retrieval.
- Displaying quotations.
- Audio use, if applicable.
- AI processing.
- Offline knowledge packages.
- Commercial or non-commercial distribution conditions.
- Attribution requirements.
- API restrictions.

The same examination must be conducted for other translations already included in the application.

The presence of Bible text in an existing database does not establish permission to redistribute or process it in every proposed way.

**Resolution evidence:** A translation-by-translation licence register and an approved technical distribution approach.

---

# 5. INSTITUTIONAL AND GOVERNANCE DEPENDENCIES

## 5.1 Membership register authority

**ID:** D-014  
**Classification:** INSTITUTIONAL APPROVAL / SECURITY REVIEW

The membership verification system depends on an authorised and sufficiently accurate membership register.

The church must establish:

- Which register is authoritative.
- Who may supply the initial register.
- Who may correct inaccurate entries.
- How duplicate records are resolved.
- How changes are communicated.
- How revoked or departed memberships are handled.
- How the system records the source and date of verification.

Codex may implement the approved matching workflow, but it must not invent membership records or resolve ambiguous identities automatically.

**Resolution evidence:** Authorised register source and documented correction process.

## 5.2 Delegated administration after the pilot

**ID:** D-015  
**Classification:** INSTITUTIONAL APPROVAL

During the pilot, the project owner is the sole authorised technical administrator.

Any future delegation must specify:

- Which persons are authorised.
- Which administrative functions they may perform.
- Whether approval and publication powers are separate.
- Whether sensitive pastoral records are accessible.
- Whether permission assignments can be delegated.
- How delegation expires or is revoked.
- How actions are audited.

No additional administrator should be created merely to simplify software testing.

**Resolution evidence:** Formal delegation decision and explicit permission assignments.

## 5.3 Pastoral escalation and safeguarding oversight

**ID:** D-016  
**Classification:** INSTITUTIONAL APPROVAL / LEGAL REVIEW

The pilot administrator is the authorised recipient for ordinary AI knowledge and escalation administration during the pilot.

The church must separately determine appropriate safeguarding arrangements for cases involving:

- Abuse.
- Threats of violence.
- Children or vulnerable persons.
- Suicidal intent or immediate danger.
- Allegations against church leaders.
- Situations where the normal recipient may have a conflict of interest.

The system must not assume that the ordinary AI administrator is automatically the appropriate recipient for every safeguarding matter.

**Resolution evidence:** Approved escalation and safeguarding referral procedure.

## 5.4 Formal doctrinal approval

**ID:** D-017  
**Classification:** INSTITUTIONAL APPROVAL

The AI knowledge workflow must distinguish:

1. Technical approval to publish a document.
2. Institutional approval of an official church position.
3. Formal amendment of doctrine or governing documents.

The church must identify the authorised process for the second and third categories.

Until that process is confirmed, Codex must preserve the distinction and avoid representing developing interpretations as formal institutional doctrine.

**Resolution evidence:** Authorised governance procedure and approval records.

---

# 6. ATTENDANCE AND OPERATIONAL DEPENDENCIES

## 6.1 Headquarters identity

**ID:** D-018  
**Classification:** OPERATIONAL DECISION / TECHNICAL INVESTIGATION

Version 1 attendance is restricted to headquarters.

Codex must identify how the headquarters location is represented in the existing application and backend.

The final system needs a stable location identifier and approved event/session configuration.

Do not invent branch or headquarters identifiers.

**Resolution evidence:** Confirmed institutional location record and backend identifier.

## 6.2 Attendance session permissions

**ID:** D-019  
**Classification:** INSTITUTIONAL APPROVAL

The church must determine which explicitly authorised person may:

- Create an attendance session.
- Open and close a session.
- Issue or display attendance QR codes.
- Review pending attendance.
- Correct attendance records.
- View attendance reports.

The system must enforce permissions at the backend.

A leadership title alone must not automatically grant these capabilities.

**Resolution evidence:** Approved permission assignments and backend access tests.

## 6.3 Attendance correction procedure

**ID:** D-020  
**Classification:** INSTITUTIONAL APPROVAL

The church must define how a member can challenge or explain an attendance record.

The system should support:

- A correction request.
- A reason for the request.
- Review by an authorised person.
- Approval or rejection.
- Recorded correction history.
- A visible distinction between original and corrected records.

Attendance must not automatically trigger membership suspension or disciplinary action.

**Resolution evidence:** Approved correction procedure and audit tests.

## 6.4 Sunday reminder schedule

**ID:** D-021  
**Classification:** OPERATIONAL DECISION

The application must support configurable local Sunday reminders.

The exact default reminder time must be confirmed against the church's actual service schedule.

Codex may implement the configurable scheduling mechanism before the final default time is approved.

**Resolution evidence:** Tested reminder scheduling and confirmed default configuration.

## 6.5 Attendance QR validity

**ID:** D-022  
**Classification:** TECHNICAL INVESTIGATION / OPERATIONAL DECISION

The exact validity duration of an attendance QR has not been finalised.

Codex must design the system to support configurable expiry and server-side validation.

The chosen validity duration must be tested against the church's actual attendance procedure.

**Resolution evidence:** Approved operational setting and expiry/replay tests.

---

# 7. SECURITY DEPENDENCIES

## 7.1 Production HTTPS

**ID:** D-023  
**Classification:** SECURITY REVIEW / EXTERNAL PROVIDER

The previous repository inspection reported that cleartext traffic is permitted and local development uses HTTP.

Codex must distinguish development from production.

Before production deployment:

- All sensitive production API traffic must use HTTPS.
- Production endpoints must not rely on development-only exceptions.
- Secrets must not be embedded in the APK.
- Certificate validation must not be bypassed.
- Sensitive information must not be exposed through URLs or ordinary logs.

**Resolution evidence:** Production configuration inspection and transport-security tests.

## 7.2 Device and local storage protection

**ID:** D-024  
**Classification:** SECURITY REVIEW

Codex must inspect how the application stores:

- Authentication tokens.
- Membership snapshots.
- Permission snapshots.
- Offline knowledge packages.
- Attendance outbox events.
- Escalation outbox events.
- Temporary AI context.
- Other sensitive local data.

The final implementation must minimise local sensitive data and use appropriate Android storage protections.

A disconnected device cannot receive an immediate server-side revocation. Restricted offline information therefore requires an explicit expiry and exposure assessment.

**Resolution evidence:** Local storage threat assessment and device-level security tests.

## 7.3 Signing key management

**ID:** D-025  
**Classification:** SECURITY REVIEW / TECHNICAL INVESTIGATION

Offline knowledge packages require authenticity and integrity verification.

Codex must establish:

- Signing algorithm.
- Public-key distribution.
- Private-key custody.
- Key identifiers.
- Rotation.
- Revocation.
- Recovery.
- Package versioning.
- Hash verification.
- Behaviour when signature validation fails.

Private signing keys must never be packaged in the Android application.

**Resolution evidence:** Key-management design and tests for invalid signatures, modified packages, expired packages, and key rotation.

## 7.4 Backup and restoration

**ID:** D-026  
**Classification:** SECURITY REVIEW / OPERATIONAL DECISION

The project must establish recoverable backups for relevant server-side records.

The recovery plan must cover:

- PostgreSQL.
- Membership verification history.
- Permissions.
- Knowledge documents and revision history.
- Package publication metadata.
- AI quota and usage records.
- Pastoral escalation records.
- Attendance sessions and corrections.
- Audit records.

Backup retention and access must follow approved privacy and retention requirements.

**Resolution evidence:** Successful restoration test using a controlled backup.

---

# 8. EXTERNAL SERVICE DEPENDENCIES

## 8.1 Cloudflare

**ID:** D-027  
**Classification:** EXTERNAL PROVIDER

Codex must establish which Cloudflare services are currently used and how they are configured.

Investigate:

- Worker deployment.
- Environment variables.
- KV or other storage.
- Service authentication.
- Rate limiting.
- Logs.
- Development and production separation.
- Deployment permissions.
- Secret rotation.

Do not assume the deployed Worker matches the repository source.

## 8.2 Gemini

**ID:** D-028  
**Classification:** EXTERNAL PROVIDER / LEGAL REVIEW

Codex must verify the model and API configuration actually available to the project.

The system must handle:

- Provider unavailability.
- Rate limiting.
- Invalid credentials.
- Provider errors.
- Timeouts.
- Quota exhaustion.
- Partial responses.
- Interrupted requests.

A provider failure must not result in an unauthorised fallback that sends sensitive information elsewhere.

## 8.3 Android notification and scheduling behaviour

**ID:** D-029  
**Classification:** TECHNICAL INVESTIGATION

Codex must inspect the application's existing notification infrastructure and determine how Sunday reminders can operate reliably across supported Android versions.

Test:

- Notification permission.
- Notification channels.
- Device restart.
- Timezone changes.
- Battery optimisation restrictions.
- Duplicate scheduling.
- User disabling reminders.
- App update behaviour.

The application must communicate limitations honestly where Android operating-system restrictions affect delivery.

---

# 9. DEPENDENCIES THAT MUST NOT BLOCK SAFE PREPARATORY WORK

The following work may proceed while other dependencies remain unresolved, provided it does not create an irreversible commitment or expose protected data.

Codex may:

- Inspect the repository.
- Document existing architecture.
- Prepare migration plans.
- Add tests using synthetic data.
- Design configurable retention mechanisms.
- Design permission catalogues.
- Prepare API interfaces.
- Implement non-sensitive internal abstractions.
- Improve error handling.
- Prepare local-only retrieval tests.
- Add development-only fixtures.
- Investigate provider configuration.
- Prepare documentation and technical reports.

However, Codex must not use this permission to:

- Deploy unapproved services.
- Process real sensitive pastoral records in an external provider.
- invent retention periods.
- publish unapproved doctrine.
- distribute unlicensed Bible text.
- expose the membership register.
- create unauthorised administrator accounts.
- automatically recognise offline attendance.
- perform destructive database migrations.

---

# 10. PRE-IMPLEMENTATION GATE

Before substantive implementation, Codex must produce a readiness report.

The report must contain:

### 10.1 Repository readiness

- Current commit and working-tree status.
- Existing architecture.
- Existing database versions.
- Existing migration paths.
- Existing tests and test results.
- Reusable components.
- Conflicting or obsolete code.
- Known regression risks.

### 10.2 Specification readiness

- List of all specification files read.
- Missing documents.
- Contradictions.
- Ambiguous requirements.
- Dependencies that affect implementation.
- Approved decisions that must be preserved.

### 10.3 Security readiness

- Authentication and authorisation gaps.
- Membership verification gaps.
- Secret-management gaps.
- Sensitive-data exposure risks.
- Provider authentication risks.
- Local storage risks.
- Backup and recovery gaps.

### 10.4 Implementation readiness classification

Every feature must be assigned one of these statuses:

| Status | Meaning |
|---|---|
| READY | Requirements are sufficiently clear and dependencies are resolved. |
| READY WITH CONFIGURATION | Implementation can proceed with configurable values pending final approval. |
| BLOCKED BY OWNER | Requires a decision from the project owner. |
| BLOCKED BY INSTITUTION | Requires formal church approval. |
| BLOCKED BY LEGAL REVIEW | Requires legal or licensing confirmation. |
| BLOCKED BY EXTERNAL PROVIDER | Depends on provider evidence or service capability. |
| BLOCKED BY SECURITY | Requires a security issue to be resolved first. |
| NOT IN VERSION 1 | Explicitly outside current implementation scope. |

Codex must explain why each feature has been classified as ready or blocked.

---

# 11. RELEASE READINESS GATE

A feature being implemented does not mean it is ready for production.

Before release, the project must establish:

- All critical tests pass.
- Existing application functions remain intact.
- Database upgrades preserve user data.
- Authentication and permissions are enforced server-side.
- Sensitive pastoral content is blocked from Gemini.
- Ordinary AI conversations are not permanently stored.
- Approved knowledge is correctly filtered by audience.
- Package signatures and integrity checks work.
- Quotas are enforced reliably.
- Attendance follows the approved online/offline policy.
- Privacy notices and retention rules are approved.
- Bible text distribution complies with applicable licensing.
- Production HTTPS is enforced.
- Backups and restoration are tested.
- Operational administrators have approved permissions.
- Outstanding critical security issues are resolved.

No production deployment is authorised solely by this checklist.

Deployment requires explicit project-owner authorisation and any additional institutional approvals.

---

# 12. DEPENDENCY STATUS REPORTING

Codex must maintain this document or an accompanying implementation status report as work progresses.

For every dependency, report:

| Field | Required information |
|---|---|
| Dependency ID | D-001, D-002, etc. |
| Description | The unresolved issue. |
| Classification | Technical, owner, institutional, legal, external, security, or operational. |
| Evidence | Repository file, test, provider document, or institutional source. |
| Current status | Open, investigating, awaiting approval, resolved, or blocked. |
| Impact | Which feature or release milestone is affected. |
| Required action | Specific next step. |
| Responsible authority | Codex, project owner, church authority, legal reviewer, or provider. |
| Resolution evidence | What proves the matter is resolved. |

Codex must not mark an item resolved without recording the evidence or approval supporting that status.

---

# 13. FINAL INSTRUCTION

The purpose of this register is to prevent assumptions from becoming software requirements.

Codex must distinguish between what the project owner has approved, what the repository demonstrates, what a technical recommendation proposes, and what still requires a decision.

The implementation must progress where it is safe to do so, without bypassing unresolved requirements that affect privacy, doctrine, membership, security, legal compliance, or institutional authority.

**Do not invent missing decisions. Do not weaken approved decisions. Do not treat implementation convenience as institutional approval.**

**End of Open Dependencies and Pre-Implementation Requirements.**
