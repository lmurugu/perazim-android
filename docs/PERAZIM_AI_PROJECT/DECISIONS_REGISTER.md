# PROJECT DECISIONS REGISTER

## Perazim Missions Church International AI Project

**Document:** DECISIONS_REGISTER.md  
**Status:** Project decision control document  
**Applies to:** Product design, architecture, Android, backend, AI, knowledge management, privacy, administration, testing, and deployment.

---

## 1. Purpose and Authority

This document is the consolidated register of decisions made during the planning of the Perazim Missions Church International AI system.

It must be read together with all numbered project specifications.

Its purpose is to establish which decisions are final for the current project scope, which implementation choices are technical defaults, which matters remain unresolved, and which approaches are prohibited.

Codex must use this register when planning and implementing the system.

### 1.1 Decision status definitions

**APPROVED**

A decision explicitly confirmed by the project owner. It is binding within the approved project scope.

Codex must not silently reverse, dilute, or reinterpret it.

**TECHNICAL DEFAULT**

A recommended technical approach. Codex may adopt it after repository inspection and technical validation.

If the existing implementation offers a better compatible approach, Codex may recommend that alternative and explain the evidence.

**UNRESOLVED**

A decision that has not yet been approved or requires further investigation, institutional authorisation, legal review, or technical verification.

Codex must not invent an answer.

**REJECTED**

An approach expressly excluded from the current project.

Codex must not introduce it without a formally approved scope change.

### 1.2 Conflict resolution

When documents appear to conflict, Codex must:

1. Identify the conflicting requirements.
2. Identify their source documents.
3. Determine whether the conflict concerns an approved policy, technical implementation, or unresolved matter.
4. Preserve explicit approved decisions.
5. Avoid inventing compromises.
6. Report material conflicts before implementing affected functionality.

The project owner must approve material changes to approved requirements.

Technical implementation choices must not be used to alter church doctrine, institutional governance, privacy policy, or other approved institutional decisions.

---

# 2. APPROVED DECISIONS

The following decisions have been explicitly approved or established during project planning.

## 2.1 Project identity and purpose

| ID | Decision | Status |
|---|---|---|
| A-001 | The project is an AI assistant integrated into the existing Perazim Missions Church International Android application. | APPROVED |
| A-002 | The project is not a standalone replacement application. | APPROVED |
| A-003 | The project must preserve and improve the existing application rather than rebuild it from scratch. | APPROVED |
| A-004 | The AI must serve the church's Christian, biblical, discipleship, teaching, and institutional purposes. | APPROVED |
| A-005 | The AI must remain within its defined Perazim and Christian faith-related scope. | APPROVED |
| A-006 | The project must support both offline and online AI operation. | APPROVED |
| A-007 | Existing application functionality must be preserved during implementation. | APPROVED |

## 2.2 Existing technology architecture

| ID | Decision | Status |
|---|---|---|
| A-008 | Preserve the native Java Android application. | APPROVED |
| A-009 | Preserve Room/SQLite for local application storage. | APPROVED |
| A-010 | Preserve the existing Qwen offline model and llama.cpp integration unless evidence justifies a specific compatible improvement. | APPROVED |
| A-011 | Preserve Node.js/Express as the production backend. | APPROVED |
| A-012 | Preserve PostgreSQL as the server database. | APPROVED |
| A-013 | Preserve Gemini as the online AI provider through the existing Cloudflare Worker integration, subject to security improvements. | APPROVED |
| A-014 | Python is for development, knowledge processing, and evaluation utilities, not a new production backend. | APPROVED |
| A-015 | No greenfield replacement architecture is authorised. | APPROVED |

## 2.3 AI modes and access

| ID | Decision | Status |
|---|---|---|
| A-016 | The application must support offline AI without requiring internet connectivity. | APPROVED |
| A-017 | The application must support online AI for authorised users. | APPROVED |
| A-018 | Verified church members are eligible for online Gemini access, subject to permissions and usage limits. | APPROVED |
| A-019 | Application registration alone does not establish verified church membership. | APPROVED |
| A-020 | Public users may access approved public offline resources. | APPROVED |
| A-021 | A registered but unverified user must not be treated as a verified church member. | APPROVED |
| A-022 | AI access permissions must be distinct from membership status. | APPROVED |
| A-023 | More AI usage allowance must not automatically grant access to more restricted information. | APPROVED |
| A-024 | Attendance must not be a prerequisite for AI access. | APPROVED |
| A-025 | A user must be able to open the AI interface through normal application navigation without scanning an AI QR code. | APPROVED |

## 2.4 Authentication and membership verification

| ID | Decision | Status |
|---|---|---|
| A-026 | The authorised membership register is the source for church membership verification. | APPROVED |
| A-027 | New application accounts initially remain registered but unverified. | APPROVED |
| A-028 | Membership matching must use authorised identifying information. | APPROVED |
| A-029 | An exact unambiguous match may be presented for user confirmation. | APPROVED |
| A-030 | Ambiguous, duplicate, or incomplete matches require manual review. | APPROVED |
| A-031 | Only the authorised pilot administrator may approve membership verification. | APPROVED |
| A-032 | Verification, correction, and revocation actions must be audited. | APPROVED |
| A-033 | The membership register must not be exposed to AI models or ordinary offline packages. | APPROVED |
| A-034 | Existing application roles or an `is_active` field must not be treated as proof of verified church membership. | APPROVED |
| A-035 | Membership verification must remain separate from administrative permissions. | APPROVED |
| A-036 | Inactivity or lack of attendance must not automatically revoke membership verification. | APPROVED |

## 2.5 Pilot administrative authority

| ID | Decision | Status |
|---|---|---|
| A-037 | The project owner is the sole authorised technical administrator during the pilot. | APPROVED |
| A-038 | During the pilot, the authorised technical administrator may approve, correct, replace, withdraw, classify, and publish knowledge-base content within the approved governance boundaries. | APPROVED |
| A-039 | The pilot administrator may manage AI configuration, instructions, and assigned permissions within the authorised technical scope. | APPROVED |
| A-040 | Knowledge approval for technical publication does not confer ecclesiastical authority to amend official church doctrine. | APPROVED |
| A-041 | Formal doctrinal changes must follow the church's authorised governance process. | APPROVED |
| A-042 | Technical administration and ecclesiastical authority must remain separate. | APPROVED |
| A-043 | No hidden administrator accounts or undocumented privileged access mechanisms are permitted. | APPROVED |

## 2.6 Doctrinal and knowledge authority

| ID | Decision | Status |
|---|---|---|
| A-044 | Scripture is the supreme authority for Christian doctrine. | APPROVED |
| A-045 | The ESV is the preferred Bible translation for the project, subject to licensing and distribution rights. | APPROVED |
| A-046 | Official approved Perazim positions must be represented faithfully and consistently. | APPROVED |
| A-047 | The AI must not dilute, relativise, or invent compromises concerning a clear official Perazim position. | APPROVED |
| A-048 | Alternative theological positions may be described fairly but must be distinguished from the official Perazim position. | APPROVED |
| A-049 | Personal theological interpretations must not automatically be presented as official church doctrine. | APPROVED |
| A-050 | External scholarship and internet material must not automatically become approved church knowledge. | APPROVED |
| A-051 | The AI must disclose when church documents do not establish an official position. | APPROVED |
| A-052 | Conflicting institutional documents must be flagged rather than silently reconciled. | APPROVED |
| A-053 | Approved knowledge must preserve its source, authority, revision, approval, and publication history. | APPROVED |
| A-054 | AI-generated responses must not automatically enter the approved knowledge base. | APPROVED |
| A-055 | User questions and cache patterns may identify knowledge gaps but cannot independently approve doctrine or institutional facts. | APPROVED |

## 2.7 AI conversation history and training

| ID | Decision | Status |
|---|---|---|
| A-056 | Ordinary AI conversations must not be retained as permanent chat history. | APPROVED |
| A-057 | Temporary conversation context may be used while an interaction is active. | APPROVED |
| A-058 | Ordinary conversations must not automatically become knowledge-base documents. | APPROVED |
| A-059 | Private conversations must not be used to train or fine-tune AI models. | APPROVED |
| A-060 | Operational usage and security metadata may be retained separately where necessary. | APPROVED |
| A-061 | Sensitive pastoral records must be handled through a separate protected escalation workflow. | APPROVED |

## 2.8 Privacy and external providers

| ID | Decision | Status |
|---|---|---|
| A-062 | Identifiable or sensitive pastoral information must not be sent to Gemini during the pilot. | APPROVED |
| A-063 | Sensitive-content detection must occur before external AI transmission. | APPROVED |
| A-064 | User self-selection alone is insufficient to establish that content is safe for external processing. | APPROVED |
| A-065 | Sensitive content that cannot be reliably made safe must remain local or be blocked from external processing. | APPROVED |
| A-066 | Removing a person's name alone must not be treated as sufficient anonymisation. | APPROVED |
| A-067 | Pastoral escalation records must be separate from ordinary AI conversations and shared caches. | APPROVED |
| A-068 | The application must not promise absolute anonymity. | APPROVED |
| A-069 | Private information must not appear in ordinary notifications or diagnostic logs. | APPROVED |
| A-070 | Data minimisation and appropriate access restrictions apply across all system components. | APPROVED |

## 2.9 Pastoral escalation and safety

| ID | Decision | Status |
|---|---|---|
| A-071 | Meaningful pastoral concerns requiring human attention must be eligible for escalation. | APPROVED |
| A-072 | Ordinary pastoral submissions require an informed user choice and consent, subject to applicable emergency and safeguarding obligations. | APPROVED |
| A-073 | Users may choose identified or anonymous submission where the workflow supports it. | APPROVED |
| A-074 | Anonymous submission must minimise identity exposure and explain its limitations. | APPROVED |
| A-075 | Offline escalation submissions must be clearly marked as not yet received until server confirmation. | APPROVED |
| A-076 | The pilot administrator is the authorised recipient for the pilot's AI knowledge and escalation administration unless an appropriate separate safeguarding arrangement is approved. | APPROVED |
| A-077 | Immediate emergency safety guidance must not wait for administrator review. | APPROVED |
| A-078 | The AI must not present itself as a substitute for human pastoral care or emergency services. | APPROVED |

## 2.10 Attendance, QR, and reminders

| ID | Decision | Status |
|---|---|---|
| A-079 | Version 1 includes local Sunday reminders. | APPROVED |
| A-080 | Version 1 includes QR-based AI navigation. | APPROVED |
| A-081 | Version 1 includes minimal QR-based attendance verification. | APPROVED |
| A-082 | Attendance recognition requires a minimum of 60 minutes. | APPROVED |
| A-083 | Version 1 attendance is limited to church headquarters. | APPROVED |
| A-084 | Attendance QR and AI navigation QR must be separate systems. | APPROVED |
| A-085 | AI QR scanning must not automatically register attendance. | APPROVED |
| A-086 | Attendance QR scanning must not grant AI access or membership verification. | APPROVED |
| A-087 | A valid attendance session QR may be used by multiple authorised members. | APPROVED |
| A-088 | Replay protection must be applied per member and session rather than globally consuming the shared QR. | APPROVED |
| A-089 | Automatic attendance recognition requires two valid scans successfully validated online. | APPROVED |
| A-090 | The second valid scan must be at least 60 minutes after the first, based on authoritative server timestamps. | APPROVED |
| A-091 | Device time must not establish automatic attendance recognition. | APPROVED |
| A-092 | Offline scans remain pending and require later review where trustworthy server timing cannot be established. | APPROVED |
| A-093 | A missing second scan must not automatically be treated as proof of absence or misconduct. | APPROVED |
| A-094 | Attendance data must remain separate from AI prompts, caches, history, and knowledge packages. | APPROVED |
| A-095 | Attendance must not automatically determine spiritual condition or revoke membership. | APPROVED |
| A-096 | Authorised human corrections must be audited. | APPROVED |
| A-097 | Continuous GPS, BLE, and NFC attendance tracking are excluded from Version 1. | APPROVED |
| A-098 | Branch attendance is excluded from Version 1. | APPROVED |

## 2.11 Existing application preservation

| ID | Decision | Status |
|---|---|---|
| A-099 | Existing messaging and community features must remain functional. | APPROVED |
| A-100 | Existing media, classroom, Bible reader, worship, and audio features must remain functional. | APPROVED |
| A-101 | Database migrations must preserve existing application and Bible data. | APPROVED |
| A-102 | Existing components should be reused where technically suitable. | APPROVED |
| A-103 | Regression testing is mandatory before release. | APPROVED |
| A-104 | No production deployment, external infrastructure change, or destructive operation is authorised merely by the existence of these specifications. | APPROVED |

---

# 3. TECHNICAL DEFAULTS

These are recommended implementation choices, not independent church policy decisions.

Codex may adopt them after repository inspection and technical validation.

If an alternative is necessary, Codex must document the evidence and explain the effect on the approved requirements.

| ID | Technical default | Status |
|---|---|---|
| T-001 | Use Express as the authoritative online AI authorization gateway. | TECHNICAL DEFAULT |
| T-002 | Use secure server-to-server authentication between Express and Cloudflare Worker. | TECHNICAL DEFAULT |
| T-003 | Use PostgreSQL transactions for atomic quota reservations and settlements. | TECHNICAL DEFAULT |
| T-004 | Use UUIDs for new externally referenced records unless existing conventions justify another stable identifier. | TECHNICAL DEFAULT |
| T-005 | Use versioned PostgreSQL migration files. | TECHNICAL DEFAULT |
| T-006 | Enable Room schema export and maintain migration tests. | TECHNICAL DEFAULT |
| T-007 | Use incremental Room migrations from all supported installed versions. | TECHNICAL DEFAULT |
| T-008 | Remove destructive migration fallback only after all supported upgrade paths are verified. | TECHNICAL DEFAULT |
| T-009 | Use asymmetric signatures for offline knowledge package verification. | TECHNICAL DEFAULT |
| T-010 | Keep private package-signing keys outside the Android application. | TECHNICAL DEFAULT |
| T-011 | Use exact-reference and full-text retrieval before introducing semantic embeddings. | TECHNICAL DEFAULT |
| T-012 | Initially use semantic similarity for retrieval assistance rather than automatically returning semantically matched generated answers. | TECHNICAL DEFAULT |
| T-013 | Use idempotency keys for operations that may be retried. | TECHNICAL DEFAULT |
| T-014 | Use server-generated timestamps for security-sensitive and attendance operations. | TECHNICAL DEFAULT |
| T-015 | Keep attendance APIs and database records separate from AI orchestration. | TECHNICAL DEFAULT |
| T-016 | Use local Android scheduling for Sunday reminders. | TECHNICAL DEFAULT |
| T-017 | Use a protected local outbox for pending attendance submissions. | TECHNICAL DEFAULT |
| T-018 | Use a separate protected outbox for offline pastoral escalation submissions if offline submission is implemented. | TECHNICAL DEFAULT |
| T-019 | Use configurable retention policies rather than hardcoded arbitrary periods. | TECHNICAL DEFAULT |
| T-020 | Use structured privacy-safe logs with correlation identifiers. | TECHNICAL DEFAULT |
| T-021 | Use exact approved-answer caching before introducing more complex semantic caching. | TECHNICAL DEFAULT |
| T-022 | Invalidate affected caches and packages when an approved source is withdrawn or materially revised. | TECHNICAL DEFAULT |
| T-023 | Preserve a previously valid offline package during download and activate replacements atomically. | TECHNICAL DEFAULT |
| T-024 | Use bounded expiry for restricted offline knowledge because disconnected devices cannot receive immediate revocation. | TECHNICAL DEFAULT |
| T-025 | Use HTTPS and appropriately protected production secrets. | TECHNICAL DEFAULT |
| T-026 | Use synthetic data for evaluation and testing wherever practical. | TECHNICAL DEFAULT |
| T-027 | Use server-side rate limits and hard spending controls. | TECHNICAL DEFAULT |
| T-028 | Use centralised API error codes and consistent response conventions. | TECHNICAL DEFAULT |
| T-029 | Use backend permission checks for every protected administrative operation. | TECHNICAL DEFAULT |
| T-030 | Implement data backup and verify restoration before production migrations. | TECHNICAL DEFAULT |

These defaults are intended to reduce implementation ambiguity without preventing Codex from making evidence-based technical decisions.

---

# 4. UNRESOLVED DECISIONS AND DEPENDENCIES

The following matters are not yet approved as final policy.

Codex must identify them in its implementation plan and must not silently invent decisions.

## 4.1 Privacy and legal

| ID | Unresolved matter | Required action |
|---|---|---|
| U-001 | Numerical retention period for membership records. | Institutional and legal review. |
| U-002 | Numerical retention period for attendance records. | Institutional approval. |
| U-003 | Numerical retention period for pastoral escalation cases. | Pastoral, safeguarding, and legal review. |
| U-004 | Numerical retention period for administrative audit records. | Institutional and legal review. |
| U-005 | Numerical retention period for security and AI usage logs. | Operational and legal review. |
| U-006 | Formal privacy notice and user rights process. | Prepare and approve before production. |
| U-007 | Designated privacy oversight responsibility. | Institutional designation. |
| U-008 | Provider data-processing and cross-border arrangements. | Verify actual service terms and legal requirements. |
| U-009 | Whether a formal data protection impact assessment is required. | Determine through appropriate legal review. |
| U-010 | Formal safeguarding record-retention policy. | Institutional approval. |

## 4.2 Bible text and licensing

| ID | Unresolved matter | Required action |
|---|---|---|
| U-011 | ESV offline storage and redistribution rights. | Verify applicable Crossway permissions and API terms. |
| U-012 | ESV use in external AI processing. | Verify licensing and provider arrangements. |
| U-013 | Licensing status of every existing Bible translation in the database. | Inspect source and licence documentation. |
| U-014 | Whether the current Bible database is complete and accurate. | Validate against authorised source editions. |
| U-015 | Exact translation fallback if ESV distribution is not authorised. | Establish an approved fallback without misrepresenting the translation. |

The ESV preference is approved. Its technical distribution mechanism remains dependent on licensing verification.

## 4.3 Offline access and security

| ID | Unresolved matter | Required action |
|---|---|---|
| U-016 | Exact expiry period for restricted offline knowledge packages. | Define according to sensitivity and operational needs. |
| U-017 | Supported device security requirements. | Inspect Android security capabilities and threat model. |
| U-018 | Exact treatment of local data after logout or device transfer. | Define and test. |
| U-019 | Package-signing key rotation and recovery procedure. | Design and document. |
| U-020 | Supported Room upgrade paths and existing installation versions. | Inspect repository and migration history. |

## 4.4 AI provider and quota configuration

| ID | Unresolved matter | Required action |
|---|---|---|
| U-021 | Exact daily and monthly usage quotas. | Determine operational limits. |
| U-022 | Exact token limits per request. | Benchmark and configure. |
| U-023 | Global provider spending ceiling. | Approve a financial limit. |
| U-024 | Exact provider model and deployed Worker configuration. | Verify source and deployed environment. |
| U-025 | Provider timeout, retry, and fallback thresholds. | Establish through testing. |
| U-026 | External research permissions by user category. | Confirm final access policy. |

## 4.5 Attendance and operational configuration

| ID | Unresolved matter | Required action |
|---|---|---|
| U-027 | Headquarters location record and official location identifier. | Confirm actual institutional record. |
| U-028 | Attendance session creation permissions. | Assign explicit administrative permission. |
| U-029 | Attendance correction authority and review procedure. | Approve administrative policy. |
| U-030 | Attendance QR validity duration. | Define through operational testing. |
| U-031 | Exact Sunday reminder default time. | Confirm church schedule and user preference. |
| U-032 | Whether and when branch attendance will be introduced. | Future scope decision. |

## 4.6 Institutional governance

| ID | Unresolved matter | Required action |
|---|---|---|
| U-033 | Which additional persons may receive delegated AI administration after the pilot. | Formal authorisation. |
| U-034 | Which persons or offices may review pastoral escalation cases beyond the pilot administrator. | Safeguarding and governance approval. |
| U-035 | Formal process for approving future doctrinal clarifications for AI publication. | Confirm against church governance documents. |
| U-036 | Formal process for institutional knowledge corrections and withdrawal requests. | Confirm administrative workflow. |

---

# 5. REJECTED APPROACHES

The following approaches are excluded from the current project.

| ID | Rejected approach | Reason |
|---|---|---|
| R-001 | Rebuilding the existing application from scratch. | Unnecessary replacement of functioning application infrastructure. |
| R-002 | Replacing the existing backend with a new production Python/FastAPI service. | Existing Express backend must be preserved. |
| R-003 | Treating every registered account as a verified church member. | Registration and membership verification are separate. |
| R-004 | Granting permissions solely because of a user's title or role label. | Permissions must be explicit. |
| R-005 | Allowing the Android client to authoritatively declare membership or permissions. | Client input is not a trusted security authority. |
| R-006 | Embedding reusable Gemini or privileged Worker credentials in the APK. | Exposes provider access and creates security risk. |
| R-007 | Relying on Cloudflare KV read-increment-write operations as the sole authoritative quota mechanism. | Not sufficiently reliable for concurrent accounting. |
| R-008 | Sending sensitive identifiable pastoral disclosures to Gemini during the pilot. | Explicitly prohibited by the approved privacy policy. |
| R-009 | Permanently storing ordinary AI conversations. | Explicitly rejected. |
| R-010 | Automatically using private conversations to train or fine-tune models. | Explicitly rejected. |
| R-011 | Automatically publishing AI-generated answers as church doctrine. | Human approval and institutional authority are required. |
| R-012 | Treating technical approval of a knowledge document as formal doctrinal amendment. | Technical and ecclesiastical authority are separate. |
| R-013 | Including the full membership register in AI retrieval or ordinary offline packages. | Unnecessary exposure of sensitive personal information. |
| R-014 | Including attendance records in AI prompts or knowledge packages. | Attendance is a separate administrative system. |
| R-015 | Making attendance a condition for AI access. | Explicitly rejected. |
| R-016 | Automatically revoking membership because of inactivity or missed attendance. | Membership status requires an authorised human process. |
| R-017 | Using continuous GPS, BLE, or NFC attendance tracking in Version 1. | Excluded from approved initial scope. |
| R-018 | Treating an AI navigation QR as an attendance QR. | Separate functions and security requirements. |
| R-019 | Treating a device's local clock as authoritative attendance evidence. | Device time is manipulable. |
| R-020 | Automatically recognising offline attendance without valid server verification of both required scans. | Explicitly rejected. |
| R-021 | Treating a missing second scan as automatic proof of misconduct or spiritual failure. | Unsupported inference and inappropriate use of attendance data. |
| R-022 | Allowing draft or withdrawn knowledge documents into ordinary retrieval. | Only appropriately approved and published content is eligible. |
| R-023 | Allowing semantic similarity alone to establish that two generated answers are interchangeable. | Similarity does not establish doctrinal or factual equivalence. |
| R-024 | Using arbitrary indefinite data retention. | Retention must be purpose-based and approved. |
| R-025 | Leaving destructive Room migrations as the normal upgrade path. | Existing user data must be preserved. |
| R-026 | Deploying production migrations or infrastructure changes without explicit authorisation. | Planning approval is not deployment approval. |

---

# 6. CHANGE CONTROL

Approved decisions must not be silently changed during implementation.

If Codex identifies a reason to change an approved decision, it must produce a change proposal containing:

1. Decision ID.
2. Current approved requirement.
3. Evidence of the problem.
4. Proposed alternative.
5. Technical consequences.
6. Security and privacy consequences.
7. User and institutional consequences.
8. Migration or compatibility consequences.
9. Testing implications.
10. Explicit approval required.

The project owner must approve the change before implementation of the affected requirement.

A code-level workaround must not be used to bypass a policy decision.

A technical discovery may reveal that an approved feature needs a different implementation. That is acceptable if the feature's approved behaviour and protections remain intact.

---

# 7. IMPLEMENTATION INTERPRETATION RULES

Codex must apply these rules throughout implementation.

### 7.1 Do not reopen settled decisions

An approved decision must not repeatedly be presented as an unresolved question.

For example, the policy that ordinary AI conversations are not permanently stored is settled.

The implementation method may be investigated, but the policy itself must not be reopened without a formal change request.

### 7.2 Do not invent institutional authority

Codex must not infer that a technical administrator has ecclesiastical authority.

The knowledge approval workflow must preserve the distinction between technical publication and formal doctrinal approval.

### 7.3 Do not invent retention periods

Codex may implement configurable retention mechanisms, but it must not choose arbitrary numerical periods for sensitive institutional records.

### 7.4 Do not confuse implementation status with approval status

A feature may be approved but not yet implemented.

A feature may exist in the current code but not satisfy the approved specification.

Codex must report both facts accurately.

### 7.5 Do not silently expand Version 1

Features outside the approved Version 1 scope must not be introduced simply because they appear technically convenient.

Future improvements should be documented separately.

---

# 8. CODEX DECISION REPORTING FORMAT

During repository inspection and implementation, Codex must report decisions using the following format.

| Field | Required information |
|---|---|
| Decision ID | Relevant A-, T-, U-, or R-number |
| Requirement | What the specification requires |
| Repository evidence | Relevant files, classes, endpoints, or database structures |
| Current status | Implemented, partially implemented, missing, conflicting, or unverified |
| Proposed action | What Codex recommends |
| Classification | Approved requirement, technical default, unresolved dependency, or rejected approach |
| Approval required | Yes or no |
| Verification | Test or evidence required |

Codex must maintain a record of any technical deviations and their justifications.

---

# 9. FINAL GOVERNING RULE

The project must be implemented according to the approved requirements, not according to assumptions made by an AI coding agent.

Codex has authority to inspect the repository, identify problems, recommend improvements, and implement approved technical work.

Codex does not have authority to invent church doctrine, alter institutional governance, weaken privacy requirements, bypass membership verification, or silently change the approved product scope.

Where a decision is approved, implement it.

Where a technical default is appropriate, validate it against the repository.

Where a decision is unresolved, expose the dependency.

Where an approach is rejected, do not introduce it.

**End of Decisions Register.**
