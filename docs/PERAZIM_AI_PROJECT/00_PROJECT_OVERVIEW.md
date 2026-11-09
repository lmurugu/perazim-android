# PERAZIM MISSIONS CHURCH INTERNATIONAL
# AI PROJECT
## Document 00: Project Overview and Implementation Mandate

**Document status:** Foundational project specification  
**Project:** Perazim Missions Church International AI Assistant  
**Target platform:** Existing Perazim Android application  
**Implementation agent:** OpenAI Codex  
**Product and specification authority:** Project owner  
**Repository:** Existing Android application repository  
**Specification directory:** `docs/PERAZIM_AI_PROJECT/`

---

## 1. Project Purpose

The purpose of this project is to develop an intelligent, reliable, Scripture-grounded and institutionally faithful AI assistant integrated into the existing Perazim Missions Church International Android application.

The assistant will help users access approved church information, understand Scripture, learn Christian doctrine, receive discipleship guidance, explore apologetics, and obtain assistance with matters directly related to Christian faith and the ministry of Perazim Missions Church International.

The system must faithfully represent the church's approved teachings and institutional information while distinguishing official church positions from individual theological interpretations, external scholarship and unverified information.

The project is intended to strengthen the church's existing ministry infrastructure, not replace pastoral leadership, ecclesiastical authority, human relationships or the authority of Scripture.

## 2. Existing Application: Preserve and Extend

This is an enhancement of an existing application, not a greenfield software project.

Codex must inspect and preserve the existing application architecture and functionality.

The established technology foundation includes:

- Native Android development using Java.
- Android SDK and Android Views.
- Room and SQLite for local data.
- An existing Qwen2.5 0.5B Instruct quantised model for offline generation.
- Node.js and Express for backend services.
- PostgreSQL for server-side data.
- Cloudflare Workers for the existing online AI relay.
- Gemini as the existing online generative AI provider.

The project should reuse and improve existing components wherever practical.

Codex must not introduce a replacement framework, backend language, database or application architecture merely because a different technology is available.

Python may be used for development utilities, knowledge processing, document preparation, testing and evaluation. It must not become an additional production backend.

## 3. Core Product Requirements

The completed AI assistant must support two operating modes.

### 3.1 Offline AI

The assistant must remain useful without an internet connection.

Offline functionality should include local AI generation, retrieval from approved locally available knowledge, Scripture-related assistance subject to licensing, deterministic responses for appropriate questions and access to approved public church information.

The local model must receive relevant retrieved knowledge and Scripture context when available.

It must acknowledge its limitations rather than inventing information when the local model or available knowledge is insufficient.

Restricted knowledge must not become universally available merely because it is stored locally.

### 3.2 Online AI

The online assistant will use Gemini through an appropriately secured server-side architecture.

Online access is restricted to authenticated, verified church members under the approved membership-verification workflow.

The online system must support more capable reasoning, approved knowledge retrieval and authorised external research.

External research must be clearly distinguished from official Perazim information. Internet research must never automatically become approved church doctrine or institutional knowledge.

Sensitive or identifiable pastoral information must not be transmitted to Gemini during the pilot.

## 4. Knowledge and Institutional Authority

The AI assistant must use an organised, version-controlled knowledge system.

Knowledge must be classified according to its authority, approval status, audience, revision and source.

For Christian teaching, Scripture is the supreme and final authority. Official Perazim documents and approved church teaching must be represented faithfully and must not be misrepresented as interchangeable with external theological opinions.

Where the church has an established position, the AI must communicate that position clearly and consistently.

Where no official position exists, the AI must disclose that limitation. It must not invent a position or present a developing interpretation as established church doctrine.

The AI must distinguish technical approval of knowledge content from ecclesiastical authority to amend doctrine.

Formal doctrinal changes remain subject to the church's authorised governance process.

## 5. Membership, Permissions and Administration

Registration and verified church membership are separate states.

A newly registered account must not automatically be treated as a verified church member.

The pilot membership-verification process will use an authorised membership list, controlled matching, human confirmation and an auditable verification history.

Access to confidential or restricted functions must be governed by explicit server-enforced permissions.

A person's ministry title or account registration alone must not grant unrestricted access to confidential information or administrative functions.

During the pilot, the project owner is the sole authorised technical administrator for knowledge management, AI configuration, response instructions, permissions and publication.

This technical authority does not confer ecclesiastical authority to change official doctrine.

## 6. Privacy and Conversation Handling

Ordinary AI conversations must be temporary and must not be retained as persistent conversation history during the pilot.

Ordinary conversations must not be used to train AI models or automatically converted into church knowledge.

Sensitive pastoral matters must remain outside the Gemini transmission pathway.

Where human assistance is appropriate, the system must use a separate, consent-based and access-controlled escalation process.

Private pastoral information, attendance records and confidential membership information must not be placed in shared AI caches, public knowledge packages or generative model prompts.

Minimal technical usage and security records may be retained under an approved retention policy, without unnecessarily storing conversation content.

## 7. Attendance and QR Functionality

The project includes a limited first version of attendance functionality.

The initial implementation is restricted to headquarters. Branch attendance expansion is deferred.

Attendance verification will use a distinct, signed, time-limited QR mechanism.

The minimum recognised attendance duration is 60 minutes, verified through the approved check-in and subsequent verification process.

Both scans must be successfully validated online before attendance can be automatically recognised.

Offline attempts may be retained as pending records for later review. An offline scan must not be presented as automatically verified attendance.

Attendance is separate from AI access. A member must not need to scan an attendance QR to use the AI assistant.

Attendance data must not be used by the AI to infer spiritual maturity, faithfulness or spiritual condition.

## 8. Security and Reliability

Security must be enforced by the backend, not merely by Android interface restrictions or AI instructions.

The system must protect authentication credentials, signing keys, membership data, knowledge permissions, AI quotas and confidential records.

Usage limits and provider spending controls must be enforceable under concurrent requests and retry conditions.

The system must use secure production communication, privacy-conscious logging, reliable database migrations, backup and recovery procedures, and controlled knowledge publication.

No production deployment should occur until the relevant security, privacy, backup and recovery requirements have been tested and approved.

## 9. Implementation Constraints

Codex must:

1. Inspect the existing repository before modifying code.
2. Treat this documentation set as the approved product specification, subject to the explicit status of each decision.
3. Preserve existing application features and data.
4. Use incremental and data-preserving database migrations.
5. Avoid destructive migration fallbacks in the final safe implementation.
6. Keep AI conversation history ephemeral during the pilot.
7. Never expose Gemini credentials or private signing keys in the Android application.
8. Enforce membership and permissions on the server.
9. Keep attendance, pastoral escalation and AI conversation data appropriately separated.
10. Test existing application functionality after relevant changes.
11. Record implementation decisions, migrations, tests and unresolved risks.
12. Never silently change an approved product or institutional policy.

Codex must not begin implementation merely because this foundational document has been supplied.

Implementation begins only after the complete specification set and the final master implementation instruction have been supplied.

## 10. Acceptance Philosophy

The project is not complete merely because the application compiles or the AI produces plausible answers.

Completion requires demonstrated compliance with:

- Product requirements.
- Approved theological and institutional representation.
- Knowledge approval and publication controls.
- Offline and online access restrictions.
- Membership verification and permissions.
- Privacy and pastoral safeguards.
- Attendance verification rules.
- Database integrity and migration safety.
- Security and cost controls.
- Regression testing of existing application features.

Each major feature must have explicit acceptance criteria and documented test evidence.

## 11. Relationship to Other Project Documents

This document establishes the project's purpose, boundaries and implementation mandate.

The remaining documents will define the detailed architecture, AI behaviour, theological authority, knowledge management, offline and online operation, security, membership, attendance, privacy, APIs, database contracts, testing and implementation procedure.

Where documents appear to conflict, Codex must identify the conflict rather than silently choosing an interpretation.

The approved Decisions Register will establish which decisions are settled. Open Dependencies will identify matters that still require technical verification or institutional authorisation.

---

**End of Document 00**
