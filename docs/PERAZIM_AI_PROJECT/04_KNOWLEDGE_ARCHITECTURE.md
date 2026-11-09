# PERAZIM MISSIONS CHURCH INTERNATIONAL
# AI PROJECT

## Document 04: Knowledge Architecture and Management System

**Document number:** 04  
**Document status:** Foundational data and knowledge management specification  
**Project:** Perazim Missions Church International AI Assistant  
**Applies to:** Offline Qwen, Online Gemini and the knowledge administration system  
**Pilot technical administrator:** Project owner  
**Related documents:**
- `00_PROJECT_OVERVIEW.md`
- `01_TECHNICAL_ARCHITECTURE.md`
- `02_AI_IDENTITY_AND_BEHAVIOUR.md`
- `03_THEOLOGICAL_FOUNDATION.md`
- `05_OFFLINE_AI_SPECIFICATION.md`
- `06_ONLINE_AI_SPECIFICATION.md`

---

# 1. PURPOSE OF THIS DOCUMENT

This document defines the complete knowledge management architecture for Perazim AI.

The system must provide a controlled way to introduce church information, theological resources, biblical study materials, ministry documents and approved external research into the AI environment.

It must also preserve the distinction between official church doctrine, institutional records, approved teaching materials, individual interpretations and external scholarship.

The objective is to ensure that the AI does not treat every uploaded document as equally authoritative.

Every knowledge item must have identifiable origins, a classification, an approval status, an intended audience and a revision history.

The system must support both online retrieval and offline knowledge distribution.

# 2. KNOWLEDGE MANAGEMENT PRINCIPLES

The following principles are mandatory.

## 2.1 Source traceability

Every knowledge item must be traceable to its original source.

The system must record where information came from, when it was introduced, who processed it and who authorised its use.

An AI-generated answer must never be treated as an original authoritative source.

## 2.2 Authority classification

Not all documents have the same authority.

A church constitution, an approved doctrinal declaration, an individual sermon and an external theological article must not be treated as interchangeable.

The knowledge system must preserve the authority hierarchy established in Document 03.

## 2.3 Controlled approval

Uploading a document must not automatically make it available to the AI.

Knowledge must pass through the required classification, review and approval process before publication.

## 2.4 Version control

Knowledge must be versioned.

Correcting or replacing a document must not silently destroy the historical record of the previous version.

## 2.5 Audience control

Knowledge must be distributed according to its approved audience and permissions.

Public information must be distinguishable from member-only, ministry, leadership and administrative information.

## 2.6 Offline and online consistency

The offline and online systems must use the same underlying knowledge authority rules.

Where both modes have access to the same approved information, they should produce consistent institutional answers.

## 2.7 Human accountability

The AI cannot approve its own knowledge, establish church policy or independently determine whether an individual document represents official doctrine.

Human approval must remain part of the knowledge lifecycle.

# 3. KNOWLEDGE COLLECTIONS

The knowledge system must organise content into the following principal collections.

These collections provide a logical classification. Codex may implement them through database categories, linked records, storage partitions or another suitable design that preserves the required controls.

## 3.1 Collection A: Church Identity and Institutional Information

This collection contains information about Perazim Missions Church International.

Examples include:

- Official church name.
- Mission and vision.
- Church history.
- Motto.
- Statement of identity.
- Official contacts.
- Congregations and locations.
- Approved leadership information.
- Membership information and procedures.
- Approved ministry structures.
- Official programmes and activities.

Sources may include the Constitution, approved institutional records and authorised administrative documents.

Confidential administrative records must not be placed in the public portion of this collection.

## 3.2 Collection B: Official Doctrine and Theological Positions

This collection contains authoritative church teaching.

Examples include:

- Official Statement of Faith.
- Constitutionally recognised doctrinal declarations.
- Approved doctrinal clarifications.
- Formally adopted theological positions.
- Approved doctrinal teaching documents.
- Authorised responses to recurring theological questions.

Every document must carry an authority classification.

A sermon, personal teaching note or theological research paper must not automatically enter this collection as official doctrine.

## 3.3 Collection C: Biblical and Theological Study Resources

This collection contains resources used to explain and study Scripture.

Examples include:

- Bible translations.
- Biblical reference materials.
- Historical and cultural background.
- Bible study notes.
- Biblical language resources.
- Approved commentaries.
- Theological dictionaries.
- Concordances.
- Interlinear resources.
- Historical theology materials.

The system must identify the source, translation, author, licence and intended use where applicable.

Research resources may support interpretation but must not automatically become official Perazim doctrine.

## 3.4 Collection D: Ministry and Discipleship Resources

This collection contains materials supporting church ministry.

Examples include:

- Evangelism materials.
- Discipleship curricula.
- Membership classes.
- Youth teaching materials.
- Children's catechism.
- Bible study guides.
- Worship ministry resources.
- Prayer materials.
- Fellowship resources.
- Leadership training materials.
- Ministry manuals.
- Approved counselling and pastoral support resources.

Each resource must be classified according to its intended audience.

Children's materials, for example, may require age-specific access and safeguards.

## 3.5 Collection E: External Research and Comparative Theology

This collection contains material from sources outside Perazim.

Examples include:

- Historical Christian scholarship.
- Academic theological research.
- Early Christian writings.
- Church history.
- Reformation sources.
- Pentecostal historical material.
- Comparative religious studies.
- Contemporary Christian scholarship.
- Relevant apologetics research.

External research must be labelled clearly.

It must not be presented as an official Perazim position unless a separate authorised process adopts the relevant conclusion.

## 3.6 Collection F: Operational and Application Knowledge

This collection contains information about using the Perazim application and its supported services.

Examples include:

- Application instructions.
- Account procedures.
- Membership verification instructions.
- Approved service information.
- AI usage instructions.
- Application troubleshooting.
- Supported digital ministry functions.

Information that exposes internal credentials, security procedures or confidential administrative operations must be restricted and must not become general AI knowledge.

# 4. KNOWLEDGE AUTHORITY CLASSIFICATION

Every knowledge item must receive an authority classification.

The classification must be separate from its topical collection.

For example, a sermon belongs to the ministry or theological collection, but its authority may be classified as an individual teaching resource rather than official doctrine.

The system must support at least the following classifications:

| Classification | Meaning |
|---|---|
| SCRIPTURE | Biblical source material, subject to translation and licensing rules |
| OFFICIAL_CONSTITUTION | Official constitutional or governance source |
| OFFICIAL_DOCTRINE | Formally adopted doctrinal position |
| APPROVED_CLARIFICATION | Authorised clarification of an existing position |
| APPROVED_CHURCH_TEACHING | Approved church teaching resource |
| AUTHORISED_INSTITUTIONAL_RECORD | Verified administrative or institutional information |
| INDIVIDUAL_THEOLOGY | Personal or developing theological interpretation |
| HISTORICAL_SOURCE | Primary historical Christian source |
| EXTERNAL_SCHOLARSHIP | External academic or theological research |
| UNVERIFIED | Material not sufficiently verified or approved |

These classifications must be implemented consistently with Document 03.

An unverified item must not be used to answer questions as established institutional fact.

# 5. KNOWLEDGE AUDIENCE CLASSIFICATION

Knowledge authority and access permissions are separate.

An official document may contain information that is not intended for every user.

The system must support the following audience classifications.

## 5.1 Public

Information approved for general access.

Examples include public church identity, mission, vision, approved public doctrine and public ministry information.

## 5.2 Registered

Information available to registered users who have not yet completed membership verification, where specifically authorised.

Registration alone must not grant access to confidential church information.

## 5.3 Verified Member

Information authorised for verified church members.

The verified membership state must be confirmed through the approved server-side membership verification process.

## 5.4 Ministry Worker

Information intended for ministry workers with explicitly assigned permissions.

A person must not receive access merely because they claim to hold a ministry position.

## 5.5 Minister or Clergy

Information intended for authorised ministers or clergy.

Access must be based on explicit permission assignments and appropriate institutional authorisation.

## 5.6 Senior Leadership

Information intended for authorised senior church leadership.

Leadership access must not automatically grant unrestricted technical administration or access to every confidential record.

## 5.7 Administrator

Information and configuration resources required for authorised administration of the AI system.

During the pilot, the project owner is the sole authorised technical administrator.

Future delegation must use explicit permissions and an auditable process.

# 6. REQUIRED KNOWLEDGE METADATA

Every knowledge document must have structured metadata.

At minimum, the system must record:

| Field | Purpose |
|---|---|
| `document_id` | Unique document identifier |
| `title` | Document title |
| `description` | Brief description |
| `collection` | Knowledge collection |
| `authority_classification` | Source authority |
| `source_name` | Original source name |
| `source_location` | Original file, repository or reference |
| `author` | Known author or issuing authority |
| `language` | Document language |
| `revision_number` | Document revision |
| `approval_status` | Draft, pending, approved, withdrawn or archived |
| `approved_by` | Authorised approving administrator |
| `approved_at` | Approval timestamp |
| `effective_from` | Effective date, where applicable |
| `effective_until` | Expiry date, where applicable |
| `audience_classification` | Intended access audience |
| `offline_eligible` | Whether offline distribution is authorised |
| `licence_status` | Copyright and permitted-use status |
| `content_hash` | Integrity verification |
| `supersedes_document_id` | Previous document, where applicable |
| `created_at` | Creation timestamp |
| `updated_at` | Last update timestamp |

Additional metadata may be introduced where needed.

The implementation must not silently omit source, approval or access information because a field is inconvenient to populate.

Where a field is genuinely unknown or not applicable, the system must represent that state explicitly.

# 7. KNOWLEDGE DOCUMENT LIFECYCLE

Every knowledge item must pass through a controlled lifecycle.

The standard lifecycle is:

```text
SOURCE IDENTIFIED
       |
DOCUMENT INGESTED
       |
CONTENT EXTRACTED
       |
SOURCE AND INTEGRITY CHECK
       |
AUTHORITY CLASSIFICATION
       |
AUDIENCE CLASSIFICATION
       |
REVIEW
       |
APPROVAL
       |
VERSION PUBLICATION
       |
INDEXING
       |
OFFLINE / ONLINE DISTRIBUTION
       |
MONITORING AND REVIEW
```

The implementation may include additional technical stages, but must preserve the lifecycle controls.

## 7.1 Source identification

The system must record the original source of the material.

Sources may include:

- PDF documents.
- Word documents.
- Text documents.
- Spreadsheets.
- Approved website content.
- Official institutional records.
- Licensed biblical databases.
- Historical primary sources.
- Approved research materials.

The system must preserve the original source file or an appropriate reference where legally and technically permitted.

## 7.2 Content extraction

The ingestion system may extract text, tables and other relevant content.

It must preserve document structure sufficiently to maintain:

- Headings.
- Sections.
- Paragraphs.
- Tables.
- Page references where available.
- Footnotes and references where relevant.

Scanned documents may require OCR.

OCR output must be checked for extraction errors before authoritative content is published.

## 7.3 Source and integrity verification

The system should verify:

- File integrity.
- Source identity.
- Duplicate documents.
- Extraction completeness.
- Relevant licensing.
- Document version.
- Authority of the issuing source.

A document that cannot be reliably identified must not be treated as an authoritative institutional record.

## 7.4 Classification and approval

The document must receive its authority and audience classifications.

During the pilot, the project owner is the sole technical administrator authorised to approve knowledge for publication.

Formal doctrinal adoption or amendment remains subject to the authorised church governance process.

## 7.5 Publication

Only approved and eligible documents may enter the published knowledge corpus.

Publication must create a traceable revision.

The system must retain the ability to identify exactly which knowledge revision was used for an answer where practical.

# 8. KNOWLEDGE REVISION AND CHANGE MANAGEMENT

The knowledge system must support five primary change operations.

## 8.1 Addition

A new source or document is introduced.

The system must classify and approve it before publication.

## 8.2 Correction

An existing document contains an error or incomplete information.

The correction must create a new revision or an appropriately recorded change.

The previous version must remain identifiable.

## 8.3 Replacement

A document is superseded by a newer or more authoritative document.

The new document must identify the previous version where applicable.

The system must not assume that every newer document has authority to supersede an older constitutional or doctrinal document.

## 8.4 Withdrawal

A document is no longer authorised for active use.

The system must prevent future retrieval of withdrawn content according to its withdrawal status.

Where a withdrawal affects an offline package, the next synchronisation must apply the change.

The system must retain an appropriate audit record.

## 8.5 Historical retention

A previous document may remain available for historical research without remaining an active source for current institutional answers.

The system must distinguish between historical availability and current authority.

Historical documents must not be accidentally retrieved as current policy.

# 9. KNOWLEDGE VERSIONING

Every published document must have a revision identifier.

The system must support:

- Original publication.
- Subsequent revisions.
- Approval history.
- Effective dates.
- Withdrawal.
- Supersession.
- Historical retrieval where authorised.

A change in content must produce an identifiable revision.

The system should maintain publication manifests identifying the exact set of approved documents included in each knowledge package.

A published package must be reproducible from its manifest and source revisions, subject to applicable retention and licensing restrictions.

# 10. KNOWLEDGE CHUNKING AND INDEXING

Large documents must be divided into manageable sections for retrieval.

This process is commonly called chunking.

The objective is to retrieve the relevant passage rather than sending entire documents to an AI model unnecessarily.

## 10.1 Chunking principles

Chunks should preserve the meaning and context of their source material.

The system should:

- Respect headings and section boundaries.
- Preserve relevant paragraph relationships.
- Avoid separating qualifications from the claims they qualify.
- Preserve Bible references and citations.
- Retain document and page references.
- Avoid excessively small fragments that lose context.
- Avoid excessively large fragments that waste model context.

Chunk size and overlap should be configurable and evaluated against actual retrieval performance.

## 10.2 Chunk metadata

Each chunk must retain:

- `chunk_id`
- `document_id`
- `revision_id`
- `section_title`
- `content`
- `page_reference`, where available
- `authority_classification`
- `audience_classification`
- `approval_status`
- `source_reference`
- `content_hash`

The retrieval system must not lose the relationship between a chunk and its original document.

## 10.3 Retrieval indexing

The initial implementation should prioritise:

1. Exact document and reference lookup.
2. Structured metadata filtering.
3. Full-text retrieval.
4. Relevance ranking.
5. Optional semantic retrieval after evaluation.

Semantic search must not bypass authority, approval or audience filters.

The fact that a document is semantically similar to a question does not establish that it is authoritative.

# 11. KNOWLEDGE RETRIEVAL RULES

Retrieval must consider more than textual relevance.

Before a knowledge item is supplied to an AI model, the system must check:

- Whether the item is approved.
- Whether it is current.
- Whether it is within the applicable authority classification.
- Whether the requesting user may access it.
- Whether its licence permits the intended use.
- Whether the document has been withdrawn or superseded.
- Whether the content is appropriate for offline or online use.

The system must filter restricted material before prompt construction.

A model must not be trusted to enforce database permissions after confidential information has already been supplied to it.

## 11.1 Authority-aware retrieval

When answering questions about official church doctrine, retrieval must prioritise relevant approved institutional sources.

When answering biblical questions, Scripture must remain the primary authority.

Historical and external scholarship may support an explanation but must not override Scripture or falsely replace an established Perazim position.

## 11.2 Conflict-aware retrieval

If retrieval identifies conflicting authoritative material, the system must not silently select whichever passage ranks highest.

It should identify the conflict and follow the uncertainty and escalation rules established in Document 03.

## 11.3 Evidence references

Where possible, each generated answer must retain references to the knowledge documents and passages used.

The system should support source attribution at the document, section and page level where available.

It must not invent citations.

# 12. APPROVED ANSWER CACHE

The system may store approved deterministic answers for recurring questions.

Examples include:

- Church identity.
- Official mission and vision.
- Approved service information.
- Established membership procedures.
- Frequently asked institutional questions.
- Approved explanations of established doctrine.

Cache records must retain their source references and approval status.

The system must support three distinct categories:

**Category A: Approved institutional answers**

These are explicitly approved responses based on authoritative sources.

They may be reused within their approved audience and validity period.

**Category B: Verified general Christian information**

These are reliable general explanations that are not necessarily official Perazim positions.

They must remain clearly distinguished from institutional doctrine.

**Category C: Unverified generated responses**

These are ordinary generated answers that have not been approved for reuse as authoritative knowledge.

They must not automatically become approved knowledge or institutional cache entries.

## 12.1 Cache invalidation

When a source is corrected, replaced, withdrawn or expires, dependent cache records must be reviewed or invalidated.

Cache entries must not continue presenting outdated institutional information.

The system should record the source revision on which each approved answer depends.

## 12.2 No private-data caching

Sensitive pastoral information, personal membership information and attendance records must not enter shared AI answer caches.

# 13. OFFLINE KNOWLEDGE PACKAGES

The system must generate knowledge packages for offline devices.

Packages must contain only content approved for the intended audience and offline use.

Each package must include a manifest containing:

- Package identifier.
- Package version.
- Publication timestamp.
- Included document revisions.
- Audience classification.
- Expiry information where applicable.
- Supported application/schema version.
- Integrity hashes.
- Signing key identifier.
- Digital signature.

The package must be validated before activation.

The Android application must reject:

- Invalid signatures.
- Invalid hashes.
- Unsupported package formats.
- Unauthorised audience content.
- Invalid or expired packages.
- Corrupted or incomplete downloads.

Package signing must use an appropriate asymmetric cryptographic design.

Private signing keys must remain outside the Android application.

Key rotation and recovery must be documented.

## 13.1 Atomic package activation

The application must not replace its active knowledge package with a partially downloaded or invalid package.

The process should be:

1. Download the package.
2. Verify its signature.
3. Verify its integrity.
4. Validate its structure and schema.
5. Confirm its audience and expiry.
6. Prepare the new package.
7. Activate it atomically.
8. Retain the previous valid package where appropriate.
9. Record the update result.

A failed update must not unnecessarily destroy the existing working knowledge base.

## 13.2 Offline revocation limitations

A disconnected device cannot receive an immediate server-side withdrawal or permission change.

The system must acknowledge this limitation.

Restricted offline packages must have bounded validity.

Expired or invalid packages must be handled according to their access classification.

The application must not claim immediate revocation when the device has not synchronised.

# 14. AUTOMATIC KNOWLEDGE UPDATES

The project requires a continuous knowledge update mechanism.

When information changes, the system must support the following process:

1. Identify a new document, correction, replacement, withdrawal or knowledge gap.
2. Verify the relevant source.
3. Determine the correct authority classification.
4. Obtain the required human approval.
5. Create a new revision.
6. Generate updated indexes and packages.
7. Validate the publication.
8. Make the new version available to authorised devices.
9. Synchronise devices when connectivity becomes available.
10. Record publication and synchronisation status.

The system may automate ingestion, extraction, classification suggestions, package generation and distribution.

It must not automatically approve AI-generated doctrine or publish unverified information as official church teaching.

A user question that reveals missing information may create a knowledge-gap record, but must not automatically create or approve a new doctrinal answer.

# 15. KNOWLEDGE GAP DETECTION

The system should identify recurring questions that cannot be answered adequately from approved knowledge.

Examples include:

- Repeated questions about a church programme for which no current document exists.
- Questions about a policy that has not been documented.
- Questions about a theological issue not addressed by approved sources.
- Conflicting records about service times or leadership information.
- Frequently requested ministry resources that are missing.

The system may generate a knowledge-gap report containing:

- Topic.
- Question category.
- Reason for the gap.
- Relevant existing sources.
- Frequency information where appropriate.
- Suggested human review.

Reports must not unnecessarily expose private conversation content.

Knowledge-gap detection must not become automatic doctrinal decision-making.

# 16. PILOT KNOWLEDGE ADMINISTRATION

During the pilot, the project owner is the sole authorised technical administrator.

The administrator may:

- Upload knowledge documents.
- Review extracted content.
- Correct extraction errors.
- Classify sources.
- Approve knowledge for AI use.
- Correct published content.
- Replace outdated information.
- Withdraw inappropriate material.
- Manage audience classifications.
- Authorise offline publication.
- Update AI instructions.
- Review knowledge gaps.
- Review publication and audit history.

The system must provide an audit trail for all administrative actions.

The audit should identify:

- Administrator.
- Action.
- Affected document or configuration.
- Previous revision.
- New revision.
- Timestamp.
- Reason or explanatory note where appropriate.
- Publication result.

Technical approval must not be presented as ecclesiastical authority to amend doctrine.

Formal doctrinal amendments must follow the church's authorised governance process.

# 17. KNOWLEDGE SECURITY AND DATA PROTECTION

The knowledge system must protect confidential information.

The following must not be placed in unrestricted general AI knowledge:

- Full membership registers.
- Private member contact information.
- Confidential pastoral case records.
- Attendance histories.
- Private financial records.
- Authentication credentials.
- Administrative secrets.
- Private escalation identity mappings.

Restricted documents must be subject to server-side access checks.

Offline distribution must be restricted to authorised audiences and appropriate packages.

The system must minimise the amount of personal information stored in knowledge records.

Knowledge documents must not be used to train external models without separate explicit authorisation and an appropriate legal and privacy review.

# 18. BIBLE DATABASE AND LICENSING

The existing application contains a local Bible database with multiple translations.

The inspected implementation currently defaults to KJV for retrieval.

The preferred product behaviour is ESV-first, subject to licensing.

Before redistributing or packaging Bible translations, Codex must verify:

- Exact translation identity.
- Dataset completeness.
- Source provenance.
- Licence terms.
- Offline storage permission.
- Quotation limits.
- Redistribution permission.
- AI processing permission.
- Required attribution.

The existence of a translation in the current database does not establish that the church has permission to redistribute it.

Where ESV cannot legally be packaged, the system must use an appropriately licensed alternative and identify it accurately.

The architecture must remain capable of integrating a licensed ESV source later.

# 19. TECHNICAL DATA MODEL REQUIREMENTS

The final database implementation should support the following logical entities.

| Entity | Purpose |
|---|---|
| Knowledge collection | Organises knowledge by subject |
| Knowledge document | Represents an imported source |
| Knowledge revision | Preserves document versions |
| Knowledge chunk | Stores retrievable sections |
| Source record | Identifies original provenance |
| Approval record | Records authorised approval |
| Audience classification | Defines permitted access |
| Knowledge package | Represents a published package |
| Package document | Identifies included revisions |
| Package manifest | Defines package integrity and contents |
| Publication record | Tracks publication history |
| Knowledge gap | Records missing or insufficient information |
| Cache entry | Stores approved reusable answers |
| Audit record | Tracks administrative actions |

These are logical requirements, not a demand to create duplicate tables if suitable existing structures can be extended.

Codex must map the logical model to the existing PostgreSQL and Room architecture.

The final schema and API contracts will be detailed in Document 13.

# 20. ACCEPTANCE CRITERIA

The knowledge architecture is acceptable only when testing demonstrates that:

1. Documents can be ingested without automatically becoming approved knowledge.
2. Every published document has traceable source information.
3. Authority classifications are enforced.
4. Audience restrictions are enforced.
5. Unapproved content is excluded from ordinary retrieval.
6. Withdrawn documents are excluded from current answers.
7. Replaced documents preserve historical revision information.
8. Conflicting authoritative sources are detected and handled appropriately.
9. Knowledge chunks retain their source references.
10. Approved answers can be traced to their source revisions.
11. Cache entries are invalidated when their supporting sources change.
12. Private membership, attendance and pastoral records cannot enter shared knowledge retrieval.
13. Offline packages are signed and validated.
14. Invalid or corrupted packages cannot replace valid active packages.
15. Restricted knowledge is distributed only to authorised audiences.
16. Offline package expiry and revocation limitations are communicated honestly.
17. Knowledge updates preserve publication history.
18. AI-generated answers do not automatically become approved knowledge.
19. The pilot administrator's actions are audited.
20. Technical approval remains distinct from formal doctrinal amendment.
21. ESV and other Bible translation content is used only within verified licence permissions.
22. Offline and online AI apply the same knowledge authority rules.

---

**END OF DOCUMENT 04**
