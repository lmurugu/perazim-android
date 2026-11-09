# 15 — DATA RETENTION AND PRIVACY SPECIFICATION

## 1. Purpose

This document defines the data protection, privacy, information handling, retention, deletion, and access requirements for the Perazim Missions Church International AI system.

The objective is to ensure that personal information is collected only where necessary, processed only for authorised purposes, protected against unauthorised access, and retained only for an appropriate and documented period.

The system must protect:

- Application users.
- Verified church members.
- Ministry workers and church leaders.
- Pastoral care recipients.
- Individuals submitting confidential concerns.
- Attendance records.
- Administrative records.
- Knowledge sources containing personal information.
- AI usage and security records.

The AI system must not become an uncontrolled repository of members' private conversations, confessions, personal difficulties, or sensitive church information.

## 2. Governing Privacy Principles

The implementation must follow these principles.

### 2.1 Purpose limitation

Information must be collected and processed for a defined, legitimate, and documented purpose.

Information collected for membership verification must not automatically become AI context, marketing information, or general church knowledge.

Information collected for attendance must not automatically become a basis for pastoral profiling or AI-generated judgements.

Information submitted for pastoral follow-up must not become ordinary AI conversation history.

### 2.2 Data minimisation

The system must collect only the information reasonably necessary to perform the intended function.

If a feature can operate without collecting a person's name, phone number, precise location, or other identifying information, the system should not collect those details unnecessarily.

The system must avoid copying the same personal information into multiple databases, logs, caches, and services.

### 2.3 Accuracy

Personal and institutional records must be capable of correction.

The system must distinguish verified information from user-submitted information, inferred information, and unverified AI-generated content.

A model-generated conclusion must not silently become an authoritative member record, pastoral record, or institutional fact.

### 2.4 Storage limitation

Personal information must not be retained indefinitely merely because storage is inexpensive or because future use is conceivable.

Each category of retained information must have a defined purpose, responsible owner, retention rule, and appropriate disposal method.

### 2.5 Confidentiality and security

Access must be restricted to authenticated and appropriately authorised users.

Sensitive information must not be exposed through ordinary logs, analytics, notifications, shared caches, public knowledge packages, or unauthorised AI requests.

### 2.6 Accountability

The system must maintain sufficient evidence to demonstrate that privacy requirements have been followed.

This includes documented purposes, permissions, retention rules, relevant consent records, security controls, and audit history.

## 3. Legal and Regulatory Considerations

The project is being developed for a church operating in Kenya and must take applicable Kenyan data protection requirements into account.

The implementation should be reviewed against the **Data Protection Act, 2019 (Kenya)** and applicable regulations, guidance, and requirements issued by the Office of the Data Protection Commissioner (ODPC).

Relevant legal considerations include:

- Lawfulness, fairness, and transparency of processing.
- Purpose limitation.
- Data minimisation.
- Accuracy.
- Storage limitation.
- Security safeguards.
- Data subject rights.
- Lawful processing of sensitive personal data.
- Requirements concerning processors and third-party service providers.
- Cross-border transfers of personal information.
- Data breach response and notification obligations.
- Data protection impact assessments where required.

The project must not assume that a person's church membership, pastoral disclosure, or participation in religious activities is ordinary non-sensitive information. Such information may reveal religious beliefs or other sensitive personal circumstances and must receive appropriately strong protection.

Before production deployment, the church should obtain an appropriate privacy and legal review of its actual data-processing arrangements, including the roles of the church, technology providers, and any external AI services.

Codex must implement the approved technical safeguards but must not claim that software implementation alone establishes legal compliance.

## 4. Data Classification

The system must classify information according to sensitivity and intended use.

### 4.1 Public information

Examples:

- Public church contact information.
- Public service schedules.
- Public ministry descriptions.
- Approved public teaching materials.
- Public announcements.

This information may be distributed through public application features and approved public knowledge packages.

It must still be checked for accuracy and publication authority.

### 4.2 Internal institutional information

Examples:

- Non-public administrative procedures.
- Internal ministry schedules.
- Approved internal operational documents.
- Non-confidential organisational information.

Access must be limited to the intended audience.

Internal information must not automatically become public merely because it has been uploaded to the knowledge system.

### 4.3 Personal information

Examples:

- Names.
- Phone numbers.
- Email addresses.
- Application account identifiers.
- Membership record identifiers.
- User preferences.
- Account and authentication records.

This information must be accessible only where necessary for an authorised application function.

### 4.4 Sensitive personal information

Examples may include:

- Religious beliefs and affiliation.
- Pastoral confessions.
- Marital and family difficulties.
- Abuse disclosures.
- Health information.
- Mental health information.
- Sexual or intimate personal information.
- Financial hardship where linked to an identifiable person.
- Information about children.
- Other information requiring heightened protection under applicable law.

The system must apply stronger access, retention, transmission, and disclosure restrictions to this information.

### 4.5 Restricted administrative information

Examples:

- Membership registers.
- Attendance records.
- Permission assignments.
- Administrative audit records.
- Confidential pastoral escalation cases.
- Internal security investigations.
- AI configuration and provider credentials.

Access must be governed by explicit administrative permissions.

### 4.6 Technical and operational information

Examples:

- Request identifiers.
- Error codes.
- Token usage.
- Provider response status.
- Application version.
- Security events.
- Synchronisation status.
- Performance measurements.

Technical records must not unnecessarily contain the contents of personal conversations or sensitive disclosures.

## 5. Ordinary AI Conversation Retention

The following policy has been explicitly approved.

**Ordinary AI conversations must not be retained as permanent conversation history.**

The application may maintain temporary conversation context during an active interaction so the AI can understand follow-up questions.

However:

- Ordinary AI conversations must not become permanent chat history.
- Conversations must not be stored indefinitely in PostgreSQL or another backend conversation archive.
- Conversations must not be automatically copied into a user's profile.
- Conversations must not automatically become knowledge-base material.
- Conversations must not automatically be used to train or fine-tune AI models.
- Conversations must not be exposed to other members or administrators as ordinary chat records.
- Conversations must not be retained in shared caches.
- Conversation content must not be placed in ordinary diagnostic logs.

The current application uses temporary in-memory AI dialog history. The implementation should preserve this principle and inspect other possible storage paths to ensure that conversation content is not accidentally retained.

### 5.1 Temporary context

Temporary conversation context may be used while a conversation is active.

It should be cleared when the relevant session or dialog ends, subject to the application's actual lifecycle and the technical limitations of the model integration.

The system must not describe temporary context as permanent storage or promise immediate physical erasure from device memory where that cannot be guaranteed.

### 5.2 Technical request records

The system may retain minimal operational records required for security, quota accounting, troubleshooting, and service reliability.

Such records must not include full conversation text unless a separately authorised and documented process explicitly requires it.

A request identifier, account reference, timestamp, token usage, model identifier, status, and error category may be retained where necessary.

Account identifiers should be pseudonymous in technical reporting wherever practical.

## 6. AI Training and Model Improvement

Private user conversations must not be used to train or fine-tune models.

The following are prohibited unless a separate, explicit, authorised policy is established:

- Using member conversations as training datasets.
- Using pastoral disclosures for model fine-tuning.
- Sending private conversations to external providers for model improvement.
- Automatically collecting user questions as future training examples.
- Converting user feedback into approved doctrine.
- Training models on confidential membership or attendance records.

Synthetic evaluation datasets should be used wherever practical.

If the project later considers using anonymised or specially approved data for model evaluation or improvement, the process must establish lawful authority, appropriate privacy safeguards, purpose limitation, and any required consent or other legal basis.

Anonymisation must not be claimed merely because names have been removed. Contextual details may still identify a person.

## 7. Sensitive Pastoral Information and Gemini

The following policy has been explicitly approved for the pilot.

**Identifiable or sensitive pastoral information must not be sent to Gemini.**

This restriction includes information concerning:

- Marital difficulties.
- Abuse.
- Suicidal thoughts.
- Mental health crises.
- Family conflict.
- Confidential pastoral confession.
- Identifiable personal crises.
- Other sensitive personal matters that require confidential human attention.

The restriction applies even when the user is a verified member with permission to use online AI.

Online AI access does not constitute permission to transmit every kind of personal information to an external provider.

### 7.1 Detection before transmission

Sensitive content must be detected or conservatively classified before an external request is sent.

The implementation must not rely solely on a user's self-selected category or a checkbox.

The actual message and relevant conversation context must be considered.

Where the system cannot confidently establish that external transmission is safe, it must block or redirect the request rather than assume the content is non-sensitive.

### 7.2 Local handling

Where appropriate, the application may provide limited local assistance using the offline AI system.

However, local processing does not remove the requirement for safety classification, appropriate human referral, and privacy-conscious handling.

The local model must not be presented as a confidential professional counsellor or as a substitute for human pastoral care.

### 7.3 Redaction

Removing a name alone may not make a disclosure safe to send externally.

A message may identify a person through combinations of location, family relationships, church position, events, dates, and other contextual details.

Redaction must therefore consider contextual identifiability.

If the system cannot reliably remove identifying and sensitive information, it must not send the content to Gemini.

### 7.4 Emergency situations

Emergency safety handling must not be delayed while the system attempts redaction or external model processing.

The application must provide immediate safety guidance and an appropriate human-support route.

It must not claim that the pilot administrator or emergency services have been notified unless an actual notification has been confirmed.

## 8. Pastoral Escalation Data

Pastoral escalation cases are separate from ordinary AI conversations.

A case may be retained for the specific purpose of human follow-up after the user has been informed and has consented to the relevant submission method, subject to applicable legal requirements and any necessary emergency or safeguarding obligations.

### 8.1 Identified submissions

Where a user chooses identified follow-up, the authorised recipient may receive the identity and necessary contact information required for the requested follow-up.

Only the minimum necessary information should be exposed.

### 8.2 Anonymous submissions

Where a user chooses anonymous follow-up:

- The ordinary case interface must not expose the user's identity or contact details.
- The case must receive a unique identifier.
- The system should remove obvious identifying details from the submitted content where practical.
- A protected identity-to-case mapping may be maintained only where necessary to support a permitted response workflow.
- The system must not promise absolute anonymity.

Free-text content may identify a person even when the account identity is hidden.

The application must explain this limitation.

### 8.3 Separation of records

Pastoral escalation records must be stored separately from:

- Ordinary AI history.
- Shared AI caches.
- General knowledge documents.
- Attendance records.
- Public application content.
- Unrestricted administrative reports.

Only explicitly authorised users may access case information.

### 8.4 No automatic knowledge publication

A pastoral case must never automatically become a knowledge-base document.

If a general lesson or teaching resource is later developed from a case, it must undergo a separate review and approval process. Identifying details must be removed, and the source must be handled in accordance with applicable privacy requirements.

The fact that a case was submitted does not automatically authorise its use for teaching, research, or publication.

## 9. Membership and Verification Data

Membership verification information must be retained separately from ordinary AI data.

The system may retain the membership records and verification history necessary to administer church membership and enforce application access, subject to an approved retention schedule.

The following controls apply:

- The full membership register must not be available to AI models.
- Membership information must not be included in Gemini prompts.
- Membership information must not be included in general offline packages.
- Verification records must be accessible only to authorised administrators.
- Membership matching must use only authorised identifying information.
- Verification decisions must be auditable.
- Corrections and revocations must preserve appropriate history.
- Account registration must not automatically create a membership record.
- Membership status must not be inferred from attendance or AI conversations.

Where a user corrects personal information, the system must provide an appropriate process for reviewing and correcting the relevant record.

Retention and deletion of membership records must also respect the church's legitimate institutional recordkeeping obligations and applicable legal requirements.

## 10. Attendance Data

Attendance information must be stored separately from AI data.

It may include:

- Member identifier.
- Attendance session identifier.
- Event or service type.
- Check-in event.
- Second verification event.
- Validated server timestamps.
- Attendance status.
- Synchronisation status.
- Correction history.
- Authorised administrative actions.

The system must not collect unnecessary information such as continuous location history.

Attendance data must not be:

- Sent to Gemini.
- Added to Qwen context.
- Added to ordinary AI history.
- Used in shared AI caches.
- Published in general knowledge packages.
- Used by the AI to infer a person's spiritual condition.
- Exposed to members who lack the appropriate permission.

Attendance records must be retained according to an approved church administrative retention schedule.

The project must not invent a retention period before the church has established the relevant administrative and legal requirements.

## 11. Authentication and Security Data

Authentication records may be retained for the purposes of account security, access management, fraud prevention, and audit.

Examples include:

- Account identifiers.
- Authentication credential hashes.
- Token identifiers and expiry information.
- Revocation records.
- Security events.
- Failed authentication counts.
- Relevant account recovery events.
- Permission and access changes.

Passwords and PINs must never be stored in plaintext.

Authentication secrets must not appear in application logs or ordinary administrative reports.

Access and security records must be protected against unauthorised modification.

Retention must be limited to what is necessary for the documented security and accountability purposes.

## 12. AI Usage and Cost Records

The system may retain the minimum information necessary to enforce quotas, account for provider costs, and monitor service reliability.

Examples include:

- Request identifier.
- Account identifier or pseudonymous reference.
- Model/provider identifier.
- Request timestamp.
- Input and output token counts.
- Estimated or settled provider cost.
- Quota reservation status.
- Settlement status.
- Request outcome.
- Error category.
- Retry or idempotency information.

The system should not retain the complete question and answer merely to calculate token usage.

Usage records must not be used to create behavioural or spiritual profiles of members.

The system must distinguish operational usage monitoring from pastoral assessment.

## 13. Administrative Audit Records

Administrative audit records are necessary to establish accountability for sensitive actions.

They may include:

- Actor identifier.
- Action type.
- Affected record identifier.
- Timestamp.
- Previous and new state where relevant.
- Outcome.
- Reason or administrative reference.
- Relevant permission or approval information.

Audit records must not contain unnecessary copies of sensitive personal information.

For example, an audit entry recording that a membership verification was revoked should identify the affected record and the administrative action without reproducing the complete membership register entry.

Audit records must be protected against unauthorised modification or deletion.

## 14. Data Retention Schedule

Every category of retained information must have a documented retention rule.

The following table establishes the required treatment. Exact time periods must be confirmed through the appropriate church, operational, and legal review rather than invented by the implementation agent.

| Data category | Permitted retention purpose | Required treatment |
|---|---|---|
| Ordinary AI conversation content | Temporary conversational context | No permanent conversation history |
| AI temporary local context | Active interaction | Clear when the interaction ends, subject to technical lifecycle limitations |
| AI operational request records | Security, reliability, and quota accounting | Retain minimal metadata under an approved schedule |
| AI usage and cost records | Quota enforcement and provider accounting | Retain necessary accounting records under an approved schedule |
| Membership register | Membership administration and verification | Retain under an approved institutional and legal schedule |
| Membership verification history | Accountability and correction | Preserve necessary decision history under an approved schedule |
| Permission assignments | Access management and audit | Retain current assignments and necessary history |
| Pastoral escalation cases | Consented human follow-up and authorised safeguarding purposes | Restricted retention under an approved pastoral and legal schedule |
| Attendance records | Church attendance administration | Retain under an approved institutional and legal schedule |
| Attendance correction history | Administrative accountability | Preserve necessary correction audit history |
| Security logs | Security monitoring and incident investigation | Retain minimal necessary records under an approved schedule |
| Knowledge source documents | Approved institutional and teaching resources | Retain according to authority, revision, withdrawal, and licensing rules |
| Offline knowledge packages | Local approved AI retrieval | Replace or expire according to package and access policy |
| Application preferences | User-configured application behaviour | Retain while needed; allow appropriate user changes or reset |
| Authentication records | Account security and access control | Retain according to security and legal requirements |

### 14.1 Retention periods must not be invented

The final numerical retention periods for sensitive and institutional records remain an explicit dependency.

The church must approve a schedule covering, at minimum:

- Membership records.
- Attendance records.
- Pastoral escalation cases.
- Administrative audit records.
- Security logs.
- AI usage and cost records.
- Consent records.
- Data breach and incident records.
- Knowledge revision history.

Codex must not choose arbitrary retention periods simply to complete the implementation.

Until a period has been approved, the system should use a configurable retention mechanism and must not silently introduce indefinite retention.

Where legal obligations require a particular record to be retained, the relevant legal requirement must be documented.

Where the purpose for retaining a record has ended, the system must support secure deletion or an appropriately justified archival process.

## 15. Deletion and Correction

The system must support appropriate correction and deletion processes.

### 15.1 Personal information correction

Where a user identifies inaccurate personal information, the system must provide an appropriate process for reviewing and correcting the record.

Corrections must preserve necessary audit history without unnecessarily retaining inaccurate personal information in ordinary operational systems.

### 15.2 Account deletion

Account deletion must be designed to distinguish:

- Application account credentials.
- Personal profile information.
- Membership verification records.
- Attendance records.
- Pastoral escalation cases.
- Financial or institutional records where applicable.
- Security and audit records.
- AI usage and quota records.

Deleting an application account must not automatically destroy institutional records that must lawfully be retained.

Equally, an institutional recordkeeping requirement must not be used as a reason to retain unrelated personal information indefinitely.

The deletion process must document what is deleted, what is retained, why it is retained, and the relevant access restrictions.

### 15.3 AI conversations

Ordinary AI conversation content must not be stored as permanent history in the first place.

The system should therefore avoid creating a permanent conversation-deletion problem through unnecessary collection.

### 15.4 Pastoral cases

A request to delete or withdraw a pastoral submission must be handled according to the applicable legal requirements, safeguarding considerations, consent arrangements, and approved retention schedule.

The system must not promise deletion where a legitimate legal or safeguarding obligation requires retention.

Any retained record must remain access-restricted and must not be repurposed.

## 16. Offline Data Protection

Offline functionality introduces additional privacy and security considerations.

Information stored on an Android device may remain accessible if the device is lost, stolen, shared, or compromised.

The implementation must therefore:

- Store only the information necessary for offline operation.
- Use appropriate Android secure-storage facilities for sensitive credentials.
- Protect sensitive local database contents where appropriate.
- Avoid storing the complete membership register on ordinary devices.
- Avoid placing confidential pastoral cases in general offline knowledge packages.
- Avoid placing attendance history in AI retrieval.
- Protect pending escalation and attendance outboxes.
- Prevent unauthorised access through modified client interfaces.
- Clear or restrict local information after logout where appropriate.
- Consider backups and device-transfer behaviour.
- Define the treatment of locally cached restricted knowledge after access expiry.

The system must recognise that a disconnected device cannot receive an immediate server-side revocation.

Restricted offline content must therefore use bounded access and expiry rules appropriate to its sensitivity.

The application must not claim that a server-side permission revocation immediately removes information already stored on a disconnected device.

## 17. Offline Knowledge Package Privacy

Offline knowledge packages must be audience-specific.

A package may contain approved public or appropriately restricted church teaching resources.

It must not contain:

- The complete membership register.
- Private member profiles.
- Pastoral escalation cases.
- Confidential attendance records.
- Private messages.
- Provider credentials.
- Private signing keys.
- Unapproved documents.
- Sensitive personal information without a separately authorised and justified purpose.

Each package must have a manifest identifying its audience, content categories, revision, issue date, expiry where applicable, and integrity information.

The application must validate the package before activation.

Withdrawal of a source must result in its removal from future packages and appropriate invalidation of cached or locally active material according to the offline revocation policy.

## 18. External AI Providers and Third-Party Services

The project uses external services, including Gemini through the Cloudflare Worker.

Before production use, the project must establish and document:

- What information is transmitted.
- Why the information is transmitted.
- Which service receives it.
- Whether the service acts as a processor or in another legal capacity.
- Applicable data-processing terms.
- Provider retention arrangements.
- Provider use of submitted information.
- Applicable cross-border processing arrangements.
- Security and access controls.
- Incident notification procedures.
- Any applicable transfer safeguards.

The project must not assume that using an API automatically means submitted information is not retained or processed beyond the immediate response.

The relevant provider terms and account configuration must be verified.

During the pilot, sensitive or identifiable pastoral information must not be transmitted to Gemini regardless of whether a provider claims to offer privacy protections.

### 18.1 External research

Where online AI performs authorised external research, the request must contain only the information necessary to conduct that research.

The system must not disclose a member's identity or private circumstances merely to improve search relevance.

External research results must not automatically become approved Perazim knowledge.

## 19. Logs, Analytics, and Error Reporting

Logging must be designed to support troubleshooting and security without becoming an unintended archive of personal information.

Ordinary logs should record:

- Request identifiers.
- Component names.
- Timestamps.
- Error codes.
- Operational status.
- Relevant performance measurements.
- Non-sensitive diagnostic metadata.

Ordinary logs must not record:

- Full AI questions and answers.
- Pastoral confessions.
- Sensitive member disclosures.
- Authentication secrets.
- Provider credentials.
- Full membership records.
- Private escalation content.
- Confidential attendance details.

Where diagnostic investigation genuinely requires sensitive information, the project must use a separate, explicitly authorised process with appropriate access restrictions and retention controls.

Analytics must not be used to infer religious commitment, spiritual maturity, pastoral vulnerability, or other sensitive characteristics from application behaviour.

## 20. Notifications and Privacy

Notifications must not reveal confidential information on a lock screen or shared device.

The system must avoid placing the following in notification previews:

- Pastoral escalation details.
- Sensitive AI questions or answers.
- Membership verification details.
- Attendance corrections.
- Confidential administrative actions.
- Private personal circumstances.

Notifications should use neutral wording and direct the authenticated user into the application when protected information needs to be viewed.

## 21. Children's Information

The system must apply heightened care where information concerns children.

It must not collect children's personal information unnecessarily.

Children's catechism and educational resources should be designed to work without creating unnecessary individual child profiles or storing sensitive personal disclosures.

Where children's information is required for a legitimate church function, access must be restricted and the relevant safeguarding and legal requirements must be followed.

The AI must not conduct safeguarding investigations, request unnecessary intimate details, or treat a child's disclosure as an ordinary AI conversation.

Any formal safeguarding workflow must follow the church's authorised safeguarding policy and applicable law.

## 22. Data Breach and Security Incident Handling

The project must establish a documented procedure for responding to suspected or confirmed data breaches.

Potential incidents include:

- Unauthorised access to membership records.
- Disclosure of pastoral escalation content.
- Exposure of provider credentials.
- Theft or compromise of signing keys.
- Unauthorised access to attendance information.
- Loss of an unprotected device containing sensitive data.
- Exposure of private information through logs.
- Cross-account data leakage.
- Accidental publication of restricted knowledge.

The response process must include:

1. Detect and record the incident.
2. Contain the exposure where possible.
3. Preserve necessary security evidence.
4. Assess affected information and individuals.
5. Identify the cause and affected systems.
6. Revoke compromised credentials or permissions where appropriate.
7. Notify the authorised responsible persons.
8. Determine applicable legal and regulatory notification obligations.
9. Take corrective action.
10. Document the incident and lessons learned.

The system must not allow the AI to independently decide whether a legal data breach notification is required.

The responsible church authority must obtain appropriate legal guidance and meet applicable reporting obligations.

## 23. Data Protection Impact Assessment

Before production deployment, the project should conduct and document an appropriate data protection impact assessment, particularly because the system may process information relating to religious affiliation, membership, pastoral disclosures, and other sensitive personal circumstances.

The assessment should consider:

- Categories of personal information processed.
- Purposes of processing.
- Data subjects affected.
- Data flows between Android, Express, PostgreSQL, Cloudflare, Gemini, and other providers.
- Risks to individuals.
- Access and permission controls.
- External provider arrangements.
- Cross-border processing.
- Retention and deletion.
- Offline device risks.
- Data breach consequences.
- Safeguards and residual risks.

The assessment must be based on actual implementation and intended deployment, not only on planned architecture.

## 24. User Transparency

The application must provide clear privacy information appropriate to its functions.

Users should be informed about:

- What information is collected.
- Why it is collected.
- Which features require account or membership information.
- What information may be processed by external AI services.
- The restriction on sensitive pastoral information being sent to Gemini during the pilot.
- The temporary nature of ordinary AI conversation context.
- The separate treatment of pastoral escalation cases.
- Attendance record processing.
- Relevant retention and deletion arrangements.
- Available rights and how to exercise them.
- Appropriate contact channels for privacy concerns.

The application must not use vague statements such as "your information is always private" where actual technical or legal limitations exist.

It must not promise absolute anonymity, immediate deletion from every system, or complete prevention of all data exposure.

## 25. Access Control and Administrative Oversight

Privacy controls must be enforced through the backend and supporting storage systems.

The system must ensure that:

- Membership administrators access only authorised membership information.
- Attendance administrators access only authorised attendance information.
- Pastoral case reviewers access only assigned or otherwise authorised cases.
- AI administrators access only the operational information necessary for their responsibilities.
- General application administrators do not automatically gain unrestricted access to every sensitive record.
- Ordinary AI models cannot retrieve confidential administrative information.
- Audit records are protected from unauthorised modification.

During the pilot, the user is the sole authorised technical administrator for the AI system.

This does not automatically authorise access to every pastoral case or other confidential record unless the relevant access is explicitly established and consistent with the approved workflow and applicable requirements.

## 26. Retention and Privacy Configuration

The implementation should support configurable retention policies rather than hardcoding arbitrary periods throughout the application.

A retention configuration should identify:

- Data category.
- Purpose.
- Responsible owner.
- Approved retention period.
- Start point for calculating retention.
- Legal or institutional basis.
- Expiry action.
- Deletion or archival method.
- Exception conditions.
- Last policy review date.

Changes to retention policy must be authorised and audited.

Codex must not silently change retention settings to simplify database management.

The project should support controlled deletion, expiry, archival, and review operations without requiring destructive database migrations.

## 27. Testing Requirements

The following tests are mandatory.

### 27.1 Ordinary AI conversation

Verify that:

- Conversation content is temporary.
- Conversation content is not stored as permanent history.
- Conversation content does not appear in ordinary logs.
- Conversation content is not automatically added to knowledge.
- Conversation content is not used for training.
- Dialog recreation does not accidentally retrieve old private conversation content.

### 27.2 Sensitive pastoral information

Verify that:

- Sensitive disclosures are detected before external transmission.
- Sensitive identifiable content is blocked from Gemini.
- A user cannot bypass the restriction by changing a category selection.
- Redaction does not create a false assumption of anonymity.
- Pastoral escalation content is stored separately.
- Unauthorised users cannot retrieve cases.
- Notification previews do not expose case details.

### 27.3 Membership and attendance privacy

Verify that:

- The full membership register is not exposed to AI.
- Membership information is not included in general offline packages.
- Attendance records are not included in AI context.
- Attendance records are not exposed through shared caches.
- Unauthorised users cannot retrieve other members' records.
- Administrative access is audited.

### 27.4 Retention and deletion

Verify that:

- Configured expiry rules execute correctly.
- Expired offline packages are handled appropriately.
- Deletion does not accidentally remove unrelated records.
- Required audit history remains available where authorised and justified.
- Account deletion follows the documented data-category rules.
- Backups and restored databases are subject to appropriate access and retention controls.

### 27.5 Security and provider handling

Verify that:

- Provider credentials are not logged.
- Sensitive content is not sent to Gemini.
- Production network communication is protected.
- Logs do not expose private personal information.
- Cross-account access is rejected.
- Restricted records cannot be retrieved through modified client requests.

## 28. Unresolved Dependencies Requiring Approval

The following decisions must be completed before production deployment:

1. Numerical retention periods for membership records.
2. Numerical retention periods for attendance records.
3. Numerical retention periods for pastoral escalation cases.
4. Numerical retention periods for administrative and security audit records.
5. Numerical retention periods for AI usage and cost records.
6. The approved process for account deletion and institutional record preservation.
7. The church's applicable privacy notice and user rights process.
8. The responsible person or office for privacy oversight.
9. The applicable legal basis and provider arrangements for each category of processing.
10. Any required data protection impact assessment and regulatory compliance steps.
11. The formal safeguarding and pastoral record-retention policy.
12. The treatment of backups and archived records when a deletion request is received.

These are not minor coding choices. They require appropriate institutional and legal review.

Codex may build configurable technical mechanisms, but it must not invent the underlying retention or governance policy.

## 29. Implementation Boundaries

This document defines privacy and data-retention requirements.

It does not authorise Codex to:

- Store ordinary AI conversation history permanently.
- Send sensitive pastoral information to Gemini.
- Use member conversations for model training.
- Expose membership registers to AI.
- Include attendance or pastoral records in general knowledge packages.
- Introduce indefinite retention by default.
- Promise absolute anonymity.
- Promise immediate deletion where it cannot be guaranteed.
- Treat removal of names as sufficient anonymisation.
- Change approved institutional retention policy without authorisation.

Any feature that requires additional personal information must identify its purpose, necessity, access rules, and retention treatment before implementation.

## 30. Final Privacy Principle

The Perazim AI system exists to serve the church and its people. It must not achieve convenience by collecting unnecessary personal information or turning private spiritual conversations into permanent institutional records.

The system should retain what it legitimately needs, protect what it must retain, and avoid collecting what it does not need.

Pastoral trust, personal dignity, institutional accountability, and the lawful protection of information must remain central to the design.

**End of Document 15.**
