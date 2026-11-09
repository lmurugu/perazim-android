# Governed knowledge and Scripture foundation

**Status:** Development implementation; not deployed or operationally enabled.

**Verified repository baseline:** `phase-B-community` at `4301dc9` before this change.

**Scope:** Backend source registry/retrieval foundation, relay evidence boundary, and Scripture integrity contracts.

## What is implemented

- PostgreSQL migration `0013_governed_ai_knowledge.sql` defines versioned sources, provenance-preserving chunks, and append-only lifecycle events. It intentionally seeds no sources and was not applied to any database.
- `server/src/phase-b/governedKnowledge.js` validates required metadata and approval evidence; hashes source text and chunks; excludes sensitive source classes and possible direct identifiers; chunks extracted plain text by heading and paragraph; and ranks only eligible, intact chunks deterministically.
- Ingestion always records a source as `UNVERIFIED`, regardless of an incoming status or approval claim. It writes source, chunks and an ingestion event using a caller-provided transaction client. No public ingestion or source-approval endpoint exists.
- Institutional retrieval reads only `APPROVED`, effective, public sources explicitly permitted for `PUBLIC` and `INSTITUTIONAL` use. It excludes personal theology and checks chunk hashes again before building a bounded context package. No eligible source produces an explicit insufficiency result.
- Sources that an authorized reviewer explicitly assigns to the same conflict group remain separate. When at least two such sources are retrieved, the backend returns their provenance, marks the result `CONFLICTED`, requires escalation, and does not call the model to resolve the disagreement. The system does not infer church authority from recency or from a project-owner role.
- The authenticated online route passes that bounded evidence package to the existing Worker only after the existing authentication, membership, permission, privacy and activation gates. When no approved source is found, the route returns a deterministic limitation without calling the model.
- The Worker accepts a bounded, source-attributed context only when its digest matches the short-lived Express-signed grant. It validates approval status, provenance fields, text hash, size and authority class. Source text is placed in the user-content field as JSON evidence; the fixed system instructions say retrieved text is untrusted data and cannot change policy.
- Scripture reference parsing and exact-text lookup are implemented as an injected-corpus contract. It requires a corpus identifier, version, verified integrity, an explicitly authorized translation, contiguous exact verses and matching per-verse hashes. It returns the stored text verbatim and fails with a reason code when any requirement is missing.
- The online Bible context helper no longer supplies hard-coded fallback quotations when search returns nothing.
- A response-contract helper restricts categories, filters citations to approved references, records source sufficiency and escalation, and refuses unsupported opinion categories.

## Source classification in this checkout

No church constitution PDF or searchable constitution text was found in the `app` repository. The task-referenced `PERAZIM CONTITUTION.pdf`, Perazim AI App Note 0.2, and the listed personal theological profile/specification pack files were not found in the repository inventory. Their approval, exact content and document hashes therefore cannot be verified here. No source was seeded or marked approved on their behalf.

The files under `docs/PERAZIM_AI_PROJECT/` are treated as technical requirements and design references, not adopted church doctrine. The repository data-protection package explicitly describes its Word originals as draft-for-review material; it is not an approved church policy and is not ingested.

The Android asset database contains 66 books and complete-looking verse counts for KJV (31,102), SUV (31,102), NIV (31,102), NKJV (31,102) and ESV (31,103). The JSON Bible files in `src/main/assets/bible/` are only a 179-verse starter sample. Translation/license columns and `docs/bible-source.md` are repository assertions, not independent licensing evidence; source release identifiers and a corpus integrity/version manifest suitable for backend use were not established. No Bible text was copied into the backend, and ESV was not selected. The backend lookup therefore remains unavailable until an authorized, hash-identified corpus is supplied. No model-generated or memorized quote is used as a fallback.

## Approval, withdrawal and supersession

The registry stores approval authority, approval reference, approval/effective times, supersession links, withdrawal reason and append-only events. The application has no source-management UI or safely established church-authority permission for approving theological sources. No public approval route was added. An authorized source administrator workflow, with church approval evidence distinct from ordinary project-owner/admin access, remains to be implemented before any source can become `APPROVED`. Database migration/source updates are not run by the application in this stage. Once a source's registry status is withdrawn or superseded, retrieval excludes its chunks immediately.

## Confidentiality and provider boundary

Membership lists, profiles, pastoral records, prayer requests, disciplinary records, credentials, administrative correspondence and uncertain personal-identifier content are rejected by the ingestion helper or excluded from retrieval. Retrieval is public-only; no restricted audience package is sent to the model. No questions, personal records or documents were sent to an external service. The Worker/provider remains disabled by the existing independent gates and was tested only with a mocked provider.

## Remaining dependencies and limits

1. Provide the actual constitution and any other candidate documents through a controlled channel, with their issuing authority, version and approval evidence. Classify every source before ingesting it.
2. Provide an authorized Scripture corpus and rights evidence, plus a stable source/version identifier and integrity manifest. ESV remains unavailable until its rights are verified; another translation must not be labelled ESV.
3. Add offline document extraction tooling or a reviewed extraction workflow. The current ingestion API accepts extracted text; it does not parse PDF or DOCX files.
4. Build a permission-checked source administration workflow for approval, withdrawal, supersession and audit. Do not infer church authority from a project administrator role.
5. Connect an authorized corpus loader to the Scripture contract and connect the response-contract validator to a structured model response before activation. The current source registry is empty, and no model request is made when it is empty.
6. Define the Android online request/response contract and replace the existing `AiRelayClient` stub in a later stage. The current Android client remains unavailable; this work does not claim end-to-end Android AI operation.
7. Review and approve the existing privacy/provider dependencies before turning on the online route or Worker. No deployment, provider activation, migration, or live provider request occurred in this implementation.

## Safe future ingestion

Use an isolated database transaction, extract text locally from a specifically classified source, and call the internal `ingestSource` function with complete provenance and permitted-use metadata. The function computes hashes and always records `UNVERIFIED`; user-supplied approval fields are ignored. A separate, permission-checked governance workflow must verify actual authority and record approval evidence before a source is eligible for retrieval. Never ingest membership or personal pastoral records.
