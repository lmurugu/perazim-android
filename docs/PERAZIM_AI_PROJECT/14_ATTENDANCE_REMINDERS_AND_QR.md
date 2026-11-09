# 14 — ATTENDANCE, REMINDERS AND QR SPECIFICATION

## 1. Purpose

This document defines the first-version requirements for three related but independent application features:

1. Church attendance verification.
2. QR-based navigation to the AI assistant.
3. Local Sunday service reminders.

The objective is to provide a reliable, privacy-conscious attendance mechanism while making it convenient for members to access the AI assistant and receive service reminders.

These features must remain independent in their permissions, data, and operational behaviour.

In particular:

- Attendance must not be a prerequisite for using the AI assistant.
- Scanning an AI QR code must not register attendance.
- Attendance QR codes must not grant AI access.
- Reminders must not depend on attendance registration.
- Reminders must work without an internet connection.
- Attendance records must not become AI knowledge or model context.

## 2. Approved Version 1 Scope

The following scope has been approved.

| Feature | Version 1 decision |
|---|---|
| Attendance location | Headquarters only |
| Headquarters | Perazim Missions Church International headquarters |
| Branch attendance | Deferred |
| Minimum recognised attendance | 60 minutes |
| Attendance check-in | QR-based |
| Second verification | Required |
| AI navigation QR | Supported |
| Local Sunday reminders | Supported |
| Offline reminders | Supported |
| Offline attendance recognition | Not automatic |
| BLE attendance | Excluded |
| NFC attendance | Excluded |
| Continuous GPS tracking | Excluded |
| AI access dependent on attendance | Not permitted |

The implementation must not expand Version 1 to include branches, BLE, NFC, continuous GPS, or other attendance-tracking mechanisms without approval.

## 3. Separation of the Three Features

The application must treat attendance, AI navigation, and reminders as separate functions.

### 3.1 Attendance

Attendance records whether an eligible member has completed the approved attendance verification process for a particular church service or event.

It is an administrative record.

### 3.2 AI navigation QR

The AI QR provides a convenient way to open the AI assistant through a supported application link.

It is a navigation mechanism, not an authentication or membership-verification mechanism.

### 3.3 Sunday reminders

Reminders notify users about scheduled Sunday services according to their local application settings.

They must not require the user to be online, attend a previous service, or use the AI assistant.

The three features may share application infrastructure where appropriate, but they must not share authority or security assumptions.

## 4. Attendance Eligibility

Automatic attendance recognition is intended for verified church members.

The system must check membership status through the authoritative backend.

A registered but unverified user must not receive recognised member attendance merely because they can scan a QR code.

The system must distinguish:

- Application registration.
- Verified church membership.
- Attendance verification.
- Attendance recognition.
- Attendance synchronisation status.

These are separate states.

Attendance must not be used as proof of membership.

Membership verification must not be inferred from previous attendance records.

## 5. Attendance Session Creation

An authorised administrator must be able to create an attendance session for a headquarters service or approved event.

A session should include:

- Unique session identifier.
- Event or service type.
- Headquarters location identifier.
- Scheduled date.
- Session start time.
- Session end time where applicable.
- QR token validity period.
- Minimum recognition duration.
- Session status.
- Creating administrator.
- Creation timestamp.
- Relevant policy revision.

Only an account with the appropriate attendance-session management permission may create, modify, or close a session.

The system must not assume that every ministry worker or church leader has attendance administration permission.

## 6. Attendance QR Design

Attendance QR codes must be distinct from AI navigation QR codes.

The attendance QR must contain or reference a cryptographically protected session token.

The token should include or securely reference:

- Unique token identifier or nonce.
- Session identifier.
- Event identifier.
- Location identifier.
- Issuance timestamp.
- Expiry timestamp.
- Relevant policy version.
- Cryptographic signature.

The signature must be verifiable by the backend.

The QR must not contain unnecessary member personal information.

A QR code must not be trusted merely because it contains a plausible session identifier or because it was displayed on a church screen.

The backend must verify the token's authenticity, validity, session association, and expiry.

### 6.1 Multiple members using the same QR

A valid session QR may be used by multiple eligible members.

The system must not globally consume the QR after the first member scans it.

Replay protection must instead be applied to the relevant member, session, and attendance event.

### 6.2 QR screenshot and relay risks

The system must recognise that a screenshot or photograph of a valid QR code may be shared with someone who is not physically present.

The signature and expiry protect against token modification and indefinite reuse, but they do not prove physical presence by themselves.

The implementation must document this limitation.

Version 1 must not claim that QR scanning provides perfect physical-presence verification.

The system must not introduce continuous GPS, BLE, or NFC merely to address this limitation without a separate approved decision.

## 7. Check-In Workflow

The attendance process requires two validated scans.

### First scan — Check-in

A verified member scans the attendance QR displayed for the active headquarters session.

The Android application submits the token and authenticated member identity to the backend.

The backend validates:

- Account authentication.
- Current verified membership.
- Attendance permissions and eligibility.
- QR signature.
- Token expiry.
- Session validity.
- Location and event association.
- Duplicate check-in conditions.

If validation succeeds, the backend records a check-in event using its own server receipt timestamp.

The member's attendance state becomes:

`CHECKED_IN`

The application must clearly show that the check-in has been accepted.

A scan that has not been confirmed by the backend must not be displayed as a successfully recorded check-in.

### Second scan — Attendance verification

The member must complete a second scan for the same session.

The backend validates the second scan and compares its trusted receipt timestamp with the first validated check-in timestamp.

If at least 60 minutes have elapsed, and all other session and membership requirements are satisfied, attendance may be recognised.

If fewer than 60 minutes have elapsed, attendance must not be automatically recognised.

The application should explain the remaining requirement without implying that the member has been recognised prematurely.

## 8. Sixty-Minute Minimum Recognition Rule

The minimum duration for automatically recognised attendance is 60 minutes.

The authoritative calculation is:

`Second validated server timestamp − First validated server timestamp`

The Android device's local clock must not determine the authoritative attendance duration.

The following rules apply:

- Two validated online scans at least 60 minutes apart may qualify for recognition.
- Two scans less than 60 minutes apart do not qualify.
- A missing second scan does not qualify for automatic recognition.
- A duplicate scan must not create additional attendance credit.
- A scan for a different session must not complete the original session.
- An expired or invalid token must be rejected.
- A member whose verification is no longer valid must not receive automatic recognised attendance.

The 60-minute rule is a minimum verification threshold. It does not authorise the AI or application to make conclusions about a person's spiritual condition, faithfulness, or relationship with God.

## 9. Attendance Status Model

The system should maintain attendance status separately from synchronisation status.

### 9.1 Attendance status

| Status | Meaning |
|---|---|
| NOT_REGISTERED | No valid attendance event has been recorded |
| CHECKED_IN | First validated check-in has been recorded |
| PENDING_VERIFICATION | Second verification has not yet completed |
| RECOGNISED | Attendance has met the approved recognition requirements |
| INCOMPLETE | Required verification was not completed |
| REQUIRES_REVIEW | Automatic recognition cannot safely be established |
| REJECTED | Attendance event was rejected by validation |

The implementation may introduce additional internal states while preserving these meanings.

### 9.2 Synchronisation status

| Status | Meaning |
|---|---|
| PENDING_SYNC | Event is stored locally and awaits backend submission |
| SYNCING | Submission is currently in progress |
| SYNCED | Backend has confirmed receipt |
| SYNC_FAILED | Submission failed and may be retried |
| REQUIRES_REVIEW | The event requires authorised examination |

A successful local save is not the same as successful server synchronisation.

The application must communicate this distinction clearly.

## 10. Offline Attendance Policy

The offline attendance policy has been explicitly approved.

**Automatic attendance recognition is permitted only when both required scans have been successfully validated online.**

Offline scans must remain pending and must not automatically become recognised attendance after the phone reconnects.

### 10.1 Offline first scan

If the member scans while offline:

- The application may save the attempt locally.
- The event must be marked `PENDING_SYNC`.
- The application must explain that the scan has not been validated by the server.
- The event must not be treated as a confirmed check-in.
- The application must not promise automatic attendance recognition.

### 10.2 Offline second scan

If the member completes the second scan while offline:

- The application may save the attempt locally.
- The event remains pending synchronisation.
- The system must not calculate recognised attendance using an untrusted device clock.
- The event must not be automatically recognised after reconnection.

### 10.3 Synchronisation and review

When connectivity returns, the application may submit pending events to the backend.

The backend must validate what it can establish, including:

- QR signature.
- Session identity.
- Token validity.
- Membership status.
- Event duplication.
- Available timing evidence.
- Whether the server can establish the required duration reliably.

If trustworthy server timing is unavailable, the event must be marked:

`REQUIRES_REVIEW`

An authorised human may examine the record and apply the appropriate administrative decision.

### 10.4 Device time is not authoritative

The Android device's local time can be modified.

A signed QR issuance time proves when the token was issued, not when an offline phone scanned it.

The system must not claim that an offline scan occurred at a trustworthy time merely because the device recorded a timestamp.

## 11. Attendance Synchronisation

Attendance events must be stored in a dedicated local Room outbox while awaiting synchronisation.

The outbox must support:

- Unique local event identifiers.
- Member account reference.
- Session identifier.
- Event type.
- QR token reference or protected token payload.
- Local creation timestamp for diagnostic purposes.
- Synchronisation status.
- Retry information.
- Backend acknowledgement.
- Review status where applicable.

Local records must be synchronised through authenticated backend APIs.

The backend must use idempotency controls so that repeated submissions do not create duplicate attendance events.

The application must preserve pending records across process termination and device restart.

A failed synchronisation must not silently discard the event.

The system must not mark an event as `SYNCED` until the backend confirms successful receipt.

## 12. Attendance Data and Privacy

Attendance records are confidential church administrative information.

They must be stored separately from ordinary AI conversation data and knowledge resources.

Attendance data must not be:

- Included in Gemini prompts.
- Used as Qwen context.
- Added to AI conversation history.
- Added to shared AI caches.
- Included in general offline knowledge packages.
- Used for external research.
- Exposed to ordinary members without authorisation.

The system must not collect continuous GPS data, travel history, or unnecessary location information.

The headquarters location may be represented by an internal location identifier associated with the authorised attendance session.

Access to attendance records must be governed by explicit permissions and audited.

## 13. Attendance Corrections and Human Review

The system must support authorised correction and review.

Examples include:

- A member scanned the wrong session.
- A valid scan failed to synchronise.
- A device lost connectivity.
- A QR token expired unexpectedly.
- A member completed attendance but the second scan was not recorded.
- An event was incorrectly duplicated.
- A backend validation error occurred.

An authorised administrator may review and correct a record where appropriate.

A correction must preserve:

- Original event.
- Previous status.
- Corrected status.
- Administrator identity.
- Timestamp.
- Reason.
- Audit history.

The system must not silently rewrite attendance records.

Where a case is uncertain, the administrator must be able to retain `REQUIRES_REVIEW` rather than force an automatic recognition or rejection.

## 14. Attendance and Church Accountability

Attendance information may support legitimate church administration, but it must not independently determine a person's spiritual condition or standing before God.

The following are prohibited:

- Automatically labelling a person spiritually unfaithful because of missing attendance records.
- Automatically suspending or revoking membership because of attendance inactivity.
- Automatically initiating church discipline based only on application attendance data.
- Treating application attendance as the complete record of a person's participation in church life.
- Treating a technical attendance failure as proof that a member was absent.

A formal membership or disciplinary action must follow the appropriate authorised church process.

Members must have an appropriate opportunity to explain or challenge an inaccurate attendance record.

The system must recognise that illness, work, family responsibilities, travel, caregiving, and other legitimate circumstances may affect attendance.

Attendance records must not be interpreted as a complete measure of Christian faithfulness.

## 15. AI Navigation QR

The AI navigation QR is separate from attendance.

Its purpose is to open the Perazim AI assistant through a supported application link.

The QR should use a verified application link or another appropriately secured deep-link mechanism.

Where the application is installed, the link should open the intended AI destination.

Where the application is not installed, the fallback behaviour must be clearly defined and must not falsely imply that the user has gained access to a protected service.

### 15.1 AI QR security

The AI QR must not:

- Contain permanent API secrets.
- Grant verified membership.
- Grant administrative permission.
- Bypass authentication.
- Increase usage quotas.
- Create attendance records.
- Grant access to restricted knowledge.

The normal AI authentication and permission workflow must run after navigation.

### 15.2 Existing application navigation

Codex must inspect the existing internal navigation mechanism, including the current `open_ai_guide` navigation extra.

The implementation should extend existing navigation where practical.

If a verified Android App Link or another external deep-link mechanism is required, it must be configured and tested deliberately.

Do not assume that an internal navigation extra automatically supports external QR navigation.

## 16. Local Sunday Reminders

Sunday reminders must operate independently of the AI system.

They must be generated and scheduled locally on the Android device.

The reminder feature must not require:

- Internet connectivity.
- A successful AI request.
- Attendance registration.
- A QR scan.
- A verified membership status unless a separately approved reminder restriction requires it.

The user should be able to configure reminder preferences through the application.

The initial implementation should support:

- Enable or disable reminders.
- Select the reminder time.
- Set the intended Sunday service schedule where appropriate.
- Choose a suitable notification presentation.
- Change or reset reminder preferences.

The exact default time and reminder wording should follow the existing church service schedule and the application's approved settings rather than an invented universal time.

### 16.1 Offline behaviour

The device must be able to schedule and display reminders without an internet connection.

The reminder must not depend on a successful backend synchronisation.

### 16.2 Device restart and timezone changes

The implementation must account for:

- Device restart.
- Timezone changes.
- Daylight-saving changes where applicable.
- Changes to the selected reminder time.
- Application updates.
- Notification permission changes.
- Battery optimisation and background scheduling restrictions.

The system should use an appropriate Android scheduling mechanism, favouring reliable inexact scheduling where exact timing is not essential.

It must reschedule reminders when relevant device or application settings change.

### 16.3 Notification permissions and privacy

The application must request notification permission where required by the Android version.

If permission is denied, the application should explain how the user can enable reminders through Android settings.

Notifications must not reveal private pastoral concerns, attendance records, membership status, or other confidential information.

## 17. Existing Infrastructure and Implementation Requirements

Codex must inspect the current application before designing the feature.

The existing repository includes:

- A QR helper used for connection QR creation and payload parsing.
- Internal AI navigation support through an application navigation extra.
- A Room database with existing migration history.
- An existing synchronisation framework.

However, the previous repository inspection did not establish that the application already contains a complete camera QR-scanning workflow or attendance system.

Codex must verify current functionality rather than assuming these features exist.

### 17.1 Reuse where appropriate

The implementation should reuse:

- Existing authentication.
- Existing Room database infrastructure.
- Existing API client conventions.
- Existing synchronisation patterns.
- Existing navigation.
- Existing notification infrastructure if available.

Reuse must not compromise the distinct security requirements of attendance and AI navigation.

### 17.2 Backend-first attendance authority

The backend must establish the authoritative attendance session, validate signed tokens, record server timestamps, enforce the 60-minute rule, and maintain attendance status.

The Android application must not independently declare attendance recognised.

The backend API and PostgreSQL data model must be implemented and tested before relying on Android attendance submission.

### 17.3 Database changes

The implementation may require new PostgreSQL tables for:

- Attendance sessions.
- Attendance QR tokens or token metadata.
- Attendance events.
- Attendance recognition results.
- Attendance corrections.
- Attendance policy versions.
- Attendance audit records.

Room may require:

- Attendance outbox entities.
- Pending event DAOs.
- Synchronisation metadata.
- Reminder preference storage.
- QR-related local state where required.

The final schema and API contracts are defined in Document 16.

All migrations must preserve existing user data and supported upgrade paths.

## 18. API Requirements

The final API design is specified in Document 16.

At minimum, the backend must support controlled operations for:

- Creating attendance sessions.
- Issuing or validating attendance QR tokens.
- Submitting check-in events.
- Submitting second verification events.
- Retrieving a user's permitted attendance status.
- Synchronising pending offline events.
- Reviewing uncertain events.
- Correcting attendance records.
- Retrieving authorised attendance reports.

Every protected endpoint must enforce authentication, verified membership where required, and the appropriate explicit permission.

The AI navigation QR does not need an attendance API because it is a navigation mechanism, not an attendance event.

Local reminders should not require a backend request for normal operation.

## 19. Testing Requirements

### 19.1 Attendance tests

Test:

- Valid first scan.
- Invalid first scan.
- Expired QR.
- Invalid signature.
- Wrong session.
- Wrong event.
- Wrong location.
- Duplicate first scan.
- Valid second scan after 60 minutes.
- Second scan before 60 minutes.
- Missing second scan.
- Duplicate second scan.
- Account membership revoked between scans.
- Device clock changed.
- QR screenshot shared.
- Backend unavailable.
- Offline scan.
- Device restart with pending event.
- Repeated synchronisation.
- Backend rejection.
- Manual review.
- Administrative correction.

Expected results must match the approved attendance status model.

### 19.2 AI navigation tests

Test:

- QR opens AI when the application is installed.
- QR fallback works when the application is not installed.
- Invalid or malformed link.
- User is not authenticated.
- User is registered but unverified.
- User is a verified member.
- User attempts to modify link parameters.
- QR does not create attendance.
- QR does not bypass AI permissions.

### 19.3 Reminder tests

Test:

- Reminder enabled.
- Reminder disabled.
- Reminder scheduled without internet.
- Reminder after device restart.
- Reminder after timezone change.
- Reminder after application update.
- Notification permission denied.
- Reminder time changed.
- Reminder preferences restored.
- AI service unavailable.
- Attendance feature unavailable.

Reminders must continue functioning independently of the AI and attendance systems.

## 20. Version 1 Exclusions

The following features are explicitly outside the approved Version 1 scope:

- Branch attendance.
- BLE proximity attendance.
- NFC attendance.
- Continuous GPS tracking.
- Background location tracking.
- Automatic disciplinary decisions.
- AI-generated attendance judgements.
- AI access controlled by attendance.
- Automatic recognition of offline scans.
- Phone-to-phone attendance validation.
- Attendance tracking outside authorised headquarters sessions.

Future expansion requires a separate product and policy decision.

## 21. Acceptance Criteria

The feature may be accepted when:

1. Attendance sessions can be created by authorised administrators.
2. Attendance QR tokens are cryptographically verifiable.
3. Only eligible verified members can receive recognised attendance.
4. The two-scan workflow operates correctly.
5. The 60-minute minimum is enforced using validated server timestamps.
6. Offline scans remain pending and require review where trustworthy timing cannot be established.
7. Synchronisation is idempotent and does not lose pending events.
8. Attendance corrections are audited.
9. Attendance data is isolated from AI context, cache, and knowledge packages.
10. AI QR navigation works without granting attendance or membership.
11. Sunday reminders work offline.
12. Reminders survive relevant device lifecycle events.
13. Existing application features continue working.
14. Relevant security, migration, and regression tests pass.

## 22. Implementation Boundaries

This document establishes the approved Version 1 product requirements.

Codex must inspect the existing repository, reuse suitable infrastructure, and implement the feature incrementally.

Codex must not expand the approved scope, weaken the 60-minute requirement, automatically recognise offline attendance, or make attendance a prerequisite for AI access.

Where a technical detail remains unspecified, Codex may recommend a technically sound implementation and document its reasoning.

Where a decision would change church policy, privacy, attendance recognition, or the approved product scope, Codex must seek authorisation.

## 23. Final Operating Principle

Attendance must be accurate, limited, auditable, and respectful of members.

AI navigation must provide convenience without granting authority.

Sunday reminders must serve members without making them dependent on internet access or AI availability.

These features must support church administration and ministry without turning technical records into a substitute for pastoral judgement or ecclesiastical authority.

**End of Document 14.**
