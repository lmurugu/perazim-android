# 10 — PYTHON TOOLKIT SPECIFICATION

## 1. Purpose

This document defines the Python-based supporting toolkit for the Perazim Missions Church International AI project.

The toolkit will assist with the preparation, validation, processing, evaluation and packaging of approved AI knowledge.

It will also provide controlled utilities for developers and the authorised technical administrator.

Python is intended to make knowledge management and AI evaluation more efficient without introducing an unnecessary production technology stack.

The toolkit must integrate with the existing architecture rather than replace it.

## 2. Architectural Position

### 2.1 Python is not the production backend

The production application architecture remains:

- Android: Native Java.
- Local database: Room and SQLite.
- Backend: Node.js and Express.
- Server database: PostgreSQL.
- Offline generative model: Qwen2.5 0.5B Instruct Q4_K_M GGUF through the existing llama.cpp integration.
- Online generative model: Gemini through the authorised Cloudflare Worker integration.
- Python: Development, knowledge preparation, evaluation and administrative utilities.

The project must not introduce a Python/FastAPI production backend merely to support AI knowledge processing.

Python scripts must not become an alternative source of authority for membership, permissions, online AI access, quotas or administrative decisions.

### 2.2 Intended uses

The Python toolkit may support:

1. Source document ingestion.
2. Text extraction.
3. Document validation.
4. Metadata validation.
5. Knowledge preparation.
6. Chunk generation.
7. Search-index preparation.
8. Offline package construction.
9. Package integrity verification.
10. AI response evaluation.
11. Retrieval-quality evaluation.
12. Dataset preparation using authorised material.
13. Development diagnostics.
14. Migration and data-validation utilities.
15. Controlled administrative reporting.

### 2.3 Existing Python utilities

Codex has identified existing Python scripts in the repository, including:

- `compile_bible_database.py`
- `gdrive_catalog_builder.py`
- `generate_background_assets.py`
- `capture_full_app.py`

These scripts must be inspected before new tools are created.

Existing scripts should be reused or improved where appropriate.

The project must not create duplicate utilities that perform the same function without a demonstrated need.

## 3. Toolkit Design Principles

The Python toolkit must follow these principles.

### 3.1 Reproducibility

The same approved source documents, configuration and toolkit version should produce reproducible outputs.

The toolkit should record:

- Tool version.
- Input files.
- Input hashes.
- Processing configuration.
- Output hashes.
- Processing timestamp.
- Validation results.
- Any warnings or errors.

### 3.2 Traceability

Every generated knowledge item must remain traceable to its original source.

A generated chunk must not lose its document identity, source location, authority level or approval status.

### 3.3 Validation before publication

The toolkit must not silently publish unverified material.

Generated output must pass structural and integrity checks before it can be submitted for publication.

Technical validation does not itself establish institutional or doctrinal approval.

### 3.4 Fail safely

When a source cannot be read, a document is malformed, a hash does not match or a required field is missing, the toolkit must report the problem.

It must not silently omit content and produce a package that appears complete.

### 3.5 Human review

AI-generated extraction, summarisation, classification or chunking must not automatically establish authoritative church knowledge.

Human review and the appropriate approval process remain mandatory.

## 4. Proposed Toolkit Structure

Codex should inspect the existing repository structure and select a location that fits its conventions.

A proposed organisation is:

```text
tools/
└── perazim_ai/
    ├── README.md
    ├── requirements.txt
    ├── pyproject.toml
    ├── config/
    │   ├── toolkit.yaml
    │   └── schemas/
    ├── ingestion/
    │   ├── document_loader.py
    │   ├── pdf_extractor.py
    │   ├── docx_extractor.py
    │   ├── text_extractor.py
    │   └── metadata_extractor.py
    ├── validation/
    │   ├── document_validator.py
    │   ├── metadata_validator.py
    │   ├── source_validator.py
    │   └── package_validator.py
    ├── processing/
    │   ├── normalizer.py
    │   ├── chunker.py
    │   ├── deduplicator.py
    │   └── reference_mapper.py
    ├── packaging/
    │   ├── manifest_builder.py
    │   ├── package_builder.py
    │   ├── package_verifier.py
    │   └── package_report.py
    ├── evaluation/
    │   ├── retrieval_evaluator.py
    │   ├── answer_evaluator.py
    │   ├── citation_evaluator.py
    │   ├── safety_evaluator.py
    │   └── regression_runner.py
    ├── datasets/
    │   ├── schema/
    │   ├── retrieval/
    │   ├── response/
    │   └── safety/
    ├── migrations/
    ├── reports/
    └── tests/
```

This is a proposed logical structure, not an instruction to create every file regardless of need.

Codex should reuse existing directories and scripts where appropriate.

## 5. Source Document Ingestion

### 5.1 Supported sources

The toolkit should support the controlled ingestion of:

- PDF documents.
- DOCX documents.
- Plain-text files.
- Markdown documents.
- Structured JSON.
- CSV files.
- Approved spreadsheet sources where required.

Additional formats may be added when an actual project requirement justifies them.

### 5.2 Source identification

Every ingested document must have a traceable identity.

The ingestion process should capture:

- Original filename.
- Document identifier, where available.
- Document title.
- Source location or repository reference.
- File type.
- File size.
- File hash.
- Ingestion timestamp.
- Extraction tool version.
- Extraction result.

The original source must remain distinguishable from the extracted text.

### 5.3 PDF extraction

PDF extraction must preserve document structure as far as practical.

The toolkit should identify:

- Page numbers.
- Headings.
- Paragraphs.
- Tables.
- Footnotes.
- References.
- Headers and footers.
- Embedded images where relevant.

Scanned PDFs may require OCR.

OCR-derived content must be marked as extracted through OCR and must not be treated as perfectly accurate.

### 5.4 Image and scanned-document handling

Where a document contains scanned pages, the toolkit may use OCR or an appropriate extraction process.

It must flag:

- Low-confidence text.
- Missing pages.
- Illegible passages.
- Uncertain table structure.
- Incorrect character recognition.
- Unextracted embedded content.

The system must not silently convert uncertain OCR output into approved doctrinal content.

### 5.5 Source preservation

The original file should remain available for reference according to the approved document-retention policy.

Extracted text must retain references to its source page or location.

Where a document is replaced, the old revision must remain identifiable in the history.

## 6. Document Normalisation

### 6.1 Purpose

Different document formats may represent the same information differently.

Normalisation prepares extracted content for indexing without changing its meaning.

### 6.2 Permitted normalisation

The toolkit may:

- Standardise line endings.
- Remove extraction artefacts.
- Correct repeated whitespace.
- Standardise document metadata formats.
- Identify repeated headers and footers.
- Preserve paragraph boundaries.
- Preserve headings and section numbering.
- Standardise references where this does not alter their meaning.

### 6.3 Prohibited silent alteration

The toolkit must not silently:

- Rewrite doctrinal statements.
- Change biblical quotations.
- Remove qualifying language.
- Change the meaning of a constitutional provision.
- Convert an opinion into an official church position.
- Remove exceptions or conditions.
- Alter dates or names.
- Resolve conflicting documents through automatic selection.

Potentially substantive corrections must be presented for human review.

### 6.4 Original and processed text

The system should retain a distinction between:

1. Original source content.
2. Extracted text.
3. Normalised text.
4. Indexed chunks.

Each representation must remain traceable to the source.

## 7. Metadata and Validation

### 7.1 Required metadata

Every knowledge document must be checked against the metadata requirements in `04_KNOWLEDGE_ARCHITECTURE.md`.

At minimum, the validation process should check:

- Unique document ID.
- Title.
- Source.
- Category.
- Authority classification.
- Approval status.
- Revision.
- Intended audience.
- Offline eligibility.
- Licence or usage status.
- Creation or ingestion timestamp.
- Effective date where applicable.
- Supersession information where applicable.
- Withdrawal or expiry information where applicable.

### 7.2 Authority validation

The toolkit must distinguish institutional authority from ordinary document metadata.

A file named `Official_Doctrine.pdf` does not become official merely because of its filename.

The toolkit may validate that the required authority fields exist and contain permitted values.

The authorised human approval process must establish the actual authority.

### 7.3 Approval validation

The toolkit must distinguish:

- Draft.
- Under review.
- Approved.
- Published.
- Withdrawn.
- Superseded.
- Rejected.

Only content that has completed the required approval process may be included in an approved knowledge package.

A generated document or AI-produced summary must not automatically receive approved status.

### 7.4 Conflict detection

The toolkit should identify potential conflicts between documents using:

- Shared document references.
- Repeated policy identifiers.
- Contradictory revision metadata.
- Overlapping subject categories.
- Explicit supersession declarations.
- Relevant text comparison.

Automated conflict detection is advisory.

It must not decide that one church document legally or doctrinally overrides another.

Where a conflict is identified, the system should produce a review report.

## 8. Chunking and Index Preparation

### 8.1 Purpose

Documents must be divided into useful retrieval units without losing their original context.

A chunk should contain enough information to answer a relevant question while remaining small enough for efficient retrieval and model context use.

### 8.2 Chunking principles

The chunking process should:

- Respect document headings.
- Preserve paragraph relationships.
- Keep related statements together.
- Preserve Bible references.
- Preserve section and page references.
- Avoid cutting a definition away from its qualification.
- Avoid separating a rule from its exceptions.
- Preserve relevant table relationships.
- Record the parent document and revision.

### 8.3 Chunk metadata

Every chunk should contain:

- Unique chunk ID.
- Parent document ID.
- Document revision.
- Chunk sequence.
- Heading or section.
- Source page or location.
- Authority classification.
- Approval status.
- Audience.
- Offline eligibility.
- Content hash.

### 8.4 Chunk size

Chunk size must be configurable according to the retrieval method and model context limits.

Codex should evaluate actual document types and model constraints before selecting default limits.

There must not be one arbitrary fixed chunk size applied to every document.

### 8.5 Overlap

Limited overlap may be used where it improves context preservation.

The toolkit should avoid excessive duplication that wastes storage and model context.

### 8.6 Bible reference mapping

Where a chunk contains Bible references, the toolkit should preserve them as structured references where possible.

A reference must not be silently changed to a different book, chapter or verse during normalisation.

Bible retrieval must continue to distinguish the actual translation supplied.

## 9. Deduplication and Revision Management

### 9.1 Duplicate detection

The toolkit should identify:

- Identical source files.
- Identical extracted content.
- Repeated document IDs.
- Duplicate chunks.
- Near-duplicate documents.
- Documents that appear to be revised versions of earlier material.

### 9.2 No automatic deletion

Detected duplicates must not automatically cause an approved source to be deleted.

The toolkit should produce a report showing the suspected duplication and relevant source references.

The authorised administrator or relevant reviewer determines the correct action.

### 9.3 Revision handling

A revised document must receive a new revision identity or version according to the knowledge architecture.

The system must preserve:

- Previous revision.
- New revision.
- Change description.
- Source.
- Approval history.
- Effective date.
- Supersession relationship.

### 9.4 Withdrawal

When a document is withdrawn, the toolkit must support generating an updated index and package that excludes it from active approved retrieval.

The historical record may be retained according to policy, but withdrawn material must not remain available as current approved guidance.

## 10. Offline Knowledge Package Generation

### 10.1 Purpose

The toolkit must support building offline knowledge packages for the Android application.

Packages must be compatible with the package architecture defined in:

- `04_KNOWLEDGE_ARCHITECTURE.md`
- `05_OFFLINE_AI_SPECIFICATION.md`
- `08_SECURITY_AND_ACCESS_CONTROL.md`

### 10.2 Package contents

Depending on the package audience, a package may contain:

- Approved document metadata.
- Approved document chunks.
- Search-index data.
- Bible reference mappings where licensed.
- Package manifest.
- Revision information.
- Required integrity metadata.

The package must contain only content authorised for its intended audience.

### 10.3 Audience-specific packaging

The toolkit must support different package audiences.

Examples include:

- Public knowledge package.
- Registered-user package, where approved.
- Verified-member package.
- Ministry-worker package.
- Minister or clergy package.
- Specifically authorised restricted package.

The actual audience categories must match the server's permission model.

A package must not receive broader access merely because a developer selects a more permissive configuration.

The backend must validate the publication authorisation.

### 10.4 Package manifest

Each package must contain a manifest describing:

- Package identifier.
- Package version.
- Schema version.
- Creation timestamp.
- Expiry timestamp where applicable.
- Intended audience.
- Included knowledge revisions.
- Included document identifiers.
- Content hashes.
- Signing key identifier.
- Required compatibility information.

### 10.5 Signing

The package must be signed using an approved asymmetric signing mechanism.

The toolkit may prepare the package and invoke an authorised signing process.

The private signing key must not be embedded in ordinary Python scripts, source-control repositories or distributed developer environments.

The signing operation must be protected and restricted to authorised publication processes.

### 10.6 Verification

Before publication, the toolkit must verify:

- Manifest structure.
- Required metadata.
- Package hashes.
- Content integrity.
- Audience restrictions.
- Schema compatibility.
- Expiry information.
- Approval status.
- Signing result.

A failed validation must block publication.

### 10.7 Atomic publication

The toolkit must not directly overwrite the only valid published package.

The process should support:

1. Build a new candidate package.
2. Validate the candidate.
3. Obtain required approval.
4. Sign the approved package.
5. Publish the new package.
6. Retain the previous valid version for rollback.

The Android application must independently verify the package before activation.

## 11. Bible Database Utilities

### 11.1 Existing compiler

The existing `compile_bible_database.py` must be inspected and preserved where appropriate.

The toolkit should reuse it rather than introducing a separate Bible database compiler without justification.

### 11.2 Translation integrity

The toolkit should validate:

- Bible book count.
- Book ordering.
- Chapter ordering.
- Verse numbering.
- Missing references.
- Duplicate references.
- Translation identifiers.
- Text integrity.
- Database schema compatibility.

A row count alone does not establish Bible corpus completeness or accuracy.

### 11.3 Translation identification

Every verse must retain its correct translation identity.

The application must never present KJV text as ESV or imply that one translation was consulted when another was used.

### 11.4 Licensing

The toolkit must record the licence or permitted-use status of each Bible translation.

ESV is the preferred translation for the project's teaching and quotation requirements, subject to verified licensing rights.

The toolkit must not package or redistribute ESV content merely because it is available in an existing database.

The same licensing review must be applied to NASB and other translations.

### 11.5 ESV and NASB handling

Before offline redistribution or AI processing, the project must verify the applicable permissions for:

- Storage.
- Quotation.
- Search indexing.
- Offline distribution.
- Caching.
- AI processing.
- Display in the application.

Where permission is insufficient or unclear, the toolkit must block the restricted distribution operation and report the licensing dependency.

## 12. AI Evaluation Toolkit

### 12.1 Purpose

The toolkit must provide a repeatable way to test the quality, safety and reliability of Perazim AI.

Testing must not depend solely on informal conversations or subjective impressions.

### 12.2 Evaluation categories

The toolkit should support:

1. Scope compliance.
2. Theological consistency.
3. Retrieval accuracy.
4. Source attribution.
5. Citation accuracy.
6. Biblical reference accuracy.
7. Institutional policy accuracy.
8. Doctrinal representation.
9. Sensitive-information protection.
10. Emergency handling.
11. Offline response quality.
12. Online response quality.
13. Hallucination detection.
14. Cache correctness.
15. Permission enforcement.

### 12.3 Evaluation datasets

The project should maintain version-controlled test datasets containing representative questions and expected behaviour.

Examples include:

- Questions about church identity.
- Questions about the church Constitution.
- Questions about official doctrine.
- Questions where church documents are silent.
- Questions involving conflicting documents.
- Bible interpretation questions.
- Questions requiring historical context.
- Questions outside the AI's authorised scope.
- Sensitive pastoral disclosures.
- Emergency scenarios.
- Questions requiring human escalation.
- Questions involving restricted information.

Datasets must use approved or synthetic examples.

Real confidential pastoral conversations must not be copied into ordinary development datasets.

### 12.4 Expected outcomes

A test case should define the expected behaviour, not necessarily demand identical wording.

For example:

**Question:** "Does Perazim officially teach X?"

Expected behaviour:

- Retrieve the relevant approved source.
- State the official position accurately if established.
- Identify the source.
- Distinguish personal interpretation from institutional doctrine.
- Admit uncertainty where the official documents do not establish an answer.

### 12.5 Theological evaluation

The toolkit must test whether the AI:

- Represents approved doctrine faithfully.
- Avoids inventing institutional positions.
- Distinguishes Scripture from external scholarship.
- Distinguishes approved church teaching from personal theological interpretation.
- Identifies unresolved disagreements.
- Avoids changing doctrine to accommodate a user's preference.
- Does not use unsupported historical claims.

Automated evaluation may identify likely errors, but final theological approval remains a human responsibility.

### 12.6 Retrieval evaluation

The toolkit should measure retrieval quality using appropriate metrics.

Possible metrics include:

- Recall at K.
- Precision at K.
- Mean reciprocal rank.
- nDCG.
- Citation coverage.
- Source correctness.

The chosen metrics must be documented and interpreted appropriately.

A high retrieval score does not prove that a generated theological answer is correct.

### 12.7 Answer evaluation

The toolkit should assess:

- Factual accuracy.
- Relevance.
- Completeness.
- Source grounding.
- Citation validity.
- Instruction compliance.
- Appropriate uncertainty.
- Privacy protection.
- Correct escalation behaviour.

Automated model-based evaluation may be used as a supporting tool, but must not be the sole measure of theological or pastoral correctness.

### 12.8 Regression testing

Whenever prompts, models, retrieval rules, knowledge packages or response policies change, the relevant evaluation suite should be rerun.

Results should be compared against a recorded baseline.

A change that improves one category while damaging another must be reported.

## 13. Security and Privacy of the Toolkit

### 13.1 Source access

The toolkit must access only files and databases required for its authorised task.

It must not automatically scan unrelated user directories or private application data.

### 13.2 Secrets

The toolkit must not contain:

- Gemini API keys.
- Production database passwords.
- JWT signing secrets.
- Offline package signing private keys.
- Administrative credentials.

Secrets must be supplied through an appropriate secure operational mechanism when required.

They must not be committed to source control.

### 13.3 Logging

Toolkit logs must not unnecessarily reproduce confidential source content.

Processing reports should prefer document identifiers, filenames, hashes, error categories and line or page references.

### 13.4 Sensitive datasets

Test datasets must not contain identifiable pastoral disclosures or private membership information unless a separately authorised, protected process explicitly permits their use.

Synthetic examples should be preferred.

### 13.5 Dependencies

Python dependencies must be documented and controlled.

The project should:

- Pin or constrain dependency versions.
- Avoid unnecessary libraries.
- Review security advisories.
- Avoid installing packages dynamically during normal execution.
- Separate development and production dependencies.
- Provide reproducible setup instructions.

## 14. Command-Line Interface

The toolkit should provide a consistent command-line interface where useful.

Illustrative commands include:

```bash
python -m perazim_ai ingest --source ./sources
python -m perazim_ai validate --document-id DOC-001
python -m perazim_ai process --document-id DOC-001
python -m perazim_ai build-package --audience public
python -m perazim_ai verify-package --file ./output/package.zip
python -m perazim_ai evaluate --suite theological
python -m perazim_ai evaluate --suite retrieval
python -m perazim_ai report --type knowledge-gaps
```

These are proposed command examples, not a requirement to implement every command immediately.

Codex should establish a practical command structure consistent with the existing repository.

Commands must clearly distinguish between:

- Inspection.
- Validation.
- Candidate generation.
- Approval.
- Signing.
- Publication.

A command that builds a candidate package must not silently publish it.

## 15. Reporting

The toolkit should produce useful reports for the administrator and development team.

Possible reports include:

- Ingestion report.
- Extraction-quality report.
- Missing metadata report.
- Approval-status report.
- Document conflict report.
- Duplicate-source report.
- Chunking report.
- Package integrity report.
- Licence compliance report.
- Retrieval evaluation report.
- AI response evaluation report.
- Knowledge-gap report.
- Regression comparison report.

Reports must distinguish errors, warnings and informational messages.

A report must not imply that an unverified item is approved merely because processing succeeded.

## 16. Testing and Acceptance Criteria

### 16.1 Ingestion

- Supported files are extracted correctly.
- Original source identity is preserved.
- Extraction errors are reported.
- OCR uncertainty is identified.
- Page and section references are retained where available.
- Unsupported formats fail safely.

### 16.2 Metadata

- Required metadata is validated.
- Invalid authority values are rejected.
- Approval status is preserved.
- Missing licensing information is flagged.
- Conflicting revision information is reported.
- Duplicate identifiers are detected.

### 16.3 Processing

- Normalisation does not silently alter meaning.
- Chunk references remain traceable.
- Headings and relevant qualifications are preserved.
- Bible references remain accurate.
- Duplicate detection does not automatically delete sources.
- Revision history is preserved.

### 16.4 Packaging

- Only authorised approved content is included.
- Audience restrictions are enforced.
- Manifest information is complete.
- Content hashes are correct.
- Invalid packages fail verification.
- Signing uses an authorised key process.
- Failed builds do not replace the previous valid package.
- Published packages can be independently verified by Android.

### 16.5 Evaluation

- Test suites are repeatable.
- Results are recorded.
- Regression changes are reported.
- Theological tests distinguish institutional doctrine from general theology.
- Sensitive examples do not leak into external provider requests.
- Citation and retrieval tests detect unsupported answers.
- Evaluation does not automatically approve doctrine.

### 16.6 Security

- No production secrets are stored in source code.
- Sensitive source data is not exposed in ordinary reports.
- Dependencies are documented.
- Toolkit access is limited to authorised files and services.
- Candidate package generation cannot bypass approval and publication controls.

## 17. Implementation Priorities

The Python toolkit should be developed incrementally.

Recommended order:

**Priority 1 — Source ingestion and validation**

Build reliable document loading, extraction, metadata validation and source traceability.

**Priority 2 — Processing and chunking**

Implement controlled normalisation, chunking, reference mapping and duplicate detection.

**Priority 3 — Package generation**

Implement manifests, package validation, integrity checks and controlled signing integration.

**Priority 4 — AI evaluation**

Build repeatable theological, retrieval, citation, scope and safety test suites.

**Priority 5 — Reporting and maintenance**

Add knowledge-gap reports, revision comparisons and operational diagnostics.

Existing Bible database tooling should be improved in parallel where necessary, with special attention to translation integrity and licensing.

## 18. Implementation Constraints

Codex must:

- Inspect existing Python scripts before creating replacements.
- Reuse existing tooling where appropriate.
- Avoid unnecessary dependencies.
- Avoid introducing Python as a production backend.
- Keep production authority in the existing Express/PostgreSQL architecture.
- Preserve source-document traceability.
- Enforce the approval workflow.
- Prevent unauthorised package publication.
- Protect secrets and confidential data.
- Provide repeatable tests.
- Document toolkit setup and usage.

The toolkit must not become an independent doctrinal authority or an alternative permission system.

## 19. Implementation Authority

This document establishes the requirements for the Python support toolkit.

Codex must inspect the repository and determine which existing scripts can be reused, which need improvement and which genuinely need to be created.

The proposed directory structure and commands are implementation guidance, not permission to create unnecessary duplicate files.

Any unresolved licensing, security or institutional approval dependency must be documented.

This document does not independently authorise implementation.

Code changes may begin only after the complete documentation set has been reviewed and the project owner has supplied the final master implementation instruction.
