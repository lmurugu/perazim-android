# Stage 3 — Source Governance, Scripture Corpus, and Response Validation

**Status:** Implemented in source; not deployed, migrated, or operationally enabled.
**Repository baseline:** `phase-B-community` at `2c36bba707b940b8b7ef8f40ae76a9b593fdd6ac` before this work.
**Scope:** Backend source lifecycle, exact Scripture import and lookup, structured answer validation, relay evidence contract.

## Source documents and authority

The named September 2017 Perazim constitution and the referenced `PERAZIM CONTITUTION.pdf` were not present in the known project locations searched. No searchable constitution text, church approval reference, adoption minute, or delegated doctrinal approver was found. The repository does contain Perazim AI design and requirements documents, including `03_THEOLOGICAL_FOUNDATION.md`, `04_KNOWLEDGE_ARCHITECTURE.md`, `06_ONLINE_AI_SPECIFICATION.md`, and `OPEN_DEPENDENCIES.md`. These describe project requirements; they do not establish that the church adopted those requirements as doctrine. Document 03 itself identifies project-owner specification authority as subject to church governance. None was ingested or marked approved.

| Candidate | Identity/version and issuer evidence | SHA-256 | Approval, purpose, and confidentiality result |
|---|---|---|---|
| September 2017 church constitution | Not found in the repository or named project locations searched; issuer, version, date, and exact contents cannot be verified. | Not available | No approval or retrieval-purpose evidence; confidentiality unknown. Not ingested. |
| `03_THEOLOGICAL_FOUNDATION.md` | File labels itself a foundational theological specification; names project owner as specification authority, subject to church governance; no document version/date or adoption record found. | `19dabeaca37a5e3753aee29fdcb3d1fe03d84e027c6ad0a2cba9b6512214196f` | Technical/design requirement only; no church adoption evidence or confidentiality classification. Not an authoritative retrieval source. |
| `04_KNOWLEDGE_ARCHITECTURE.md` | File labels itself a foundational data and knowledge-management specification; no independent issuer, version/date, or adoption record found. | `925f1db1f20085bb7958df3b0a1e95f00e452fb2553f3e467cf51d8bafc76b31` | Technical design reference only; no adoption evidence or confidentiality classification. Not ingested. |
| `06_ONLINE_AI_SPECIFICATION.md` | Online AI product/technical specification; no independent issuer, version/date, or adoption record found. | `9464ed45543922523ef1f15cab650513f5fbc222d045057f2f0dd6319cb4e6cd` | Product requirements only; no church adoption evidence or confidentiality classification. Not ingested. |
| `OPEN_DEPENDENCIES.md` | Project dependency register; no version/date or formal approval record found. | `832f71fe55ed8a3fdfffcf3ddaf7e465535197cfa84fa8979caafe919ec9b080` | Operational dependency reference only; no church adoption or confidentiality classification. Not ingested. |
| `docs/bible-source.md` | `DOC-ENG-BIBLE-001`, states verification date 2026-09-12 and custodian `Bible Engine Lead`; cited upstream archives are not present in the repository. | `bc64163230c98d699f4664bceef0ab236844ae9c95e938510959a3399cfd1bb9` | Repository rights assertion, not independently reviewed license evidence or a publisher permission grant. Not used to authorize backend redistribution. |

The Stage 2 report's statement that a constitution and source workflow were missing is now partly superseded: the guarded administrative workflow is implemented below. Its findings that the constitution and verified authority are absent remain true.

## Source lifecycle

Migration `0014_ai_source_workflow_and_scripture.sql` extends the Stage 2 registry with publication state, reviewer and approver attribution, conflict groups, append-only review records, and transition constraints. It also prevents source identity/content edits, chunk mutation, and source deletion. A partial unique index allows one currently published version per source ID.

Authenticated administrative routes are mounted at `/api/admin/ai-sources`. Every operation requires fresh administrator step-up plus a current backend permission. Registration hashes and chunks extracted text inside a transaction and always creates an `UNVERIFIED` source; submitted approval claims and conflict classifications are ignored. A source manager or reviewer can inspect metadata, chunk hashes, and append-only history; those reads are audited. The helper rejects the already-defined sensitive record classes and obvious direct identifiers; it is not a general personal-data classifier.

The state sequence is:

1. A technical source manager registers a version. Its content and provenance are immutable thereafter.
2. A separate source reviewer records an append-only `RECOMMEND_APPROVAL` or `REJECT` decision. A rejected version is not re-reviewed in place; a replacement version can be submitted.
3. A separate institutional approver must provide an authority name and approval reference. The capability is intentionally inactive in the migration, so this transition remains impossible until a verified church delegation is established and the permission is deliberately enabled.
4. A separate publication executor publishes an approved source. Supersession locks both versions and changes the prior version to `SUPERSEDED` in the same transaction. Withdrawal records an enumerated reason. Retrieval filters to `PUBLISHED` sources, so withdrawn and superseded content is excluded on the next lookup.
5. A reviewer can classify two to six published sources into an unresolved conflict group. Retrieval includes the other published members of a matched group even when only one member matches query terms, then returns an escalation without calling the model.

All state changes use database transactions, row locks, replay IDs, an append-only source event log, and the existing admin audit log. There is no unauthenticated approval route. No governance permission grants are created by the migration. The technical permission codes are included in the existing administrative grant allowlist, whose current pilot-only safeguards still apply.

## Scripture corpus and licensing

The Android asset `src/main/assets/database/perazim_database.db` was opened read-only. SQLite `quick_check` returned `ok`; the 36,667,392-byte file SHA-256 is `77d83a182fbb6e8a8fdabe3e83f4c2071ca2308f7a8f5d11b1e2b5571436dcf9`. The Bible tables contain five translation IDs, 66 book rows, 1,189 chapter rows, and 155,511 verse rows:

| Translation | Verse rows | Books | Chapters | Database rights metadata |
|---|---:|---:|---:|---|
| KJV | 31,102 | 66 | 1,189 | Public Domain |
| SUV | 31,102 | 66 | 1,189 | Public Domain |
| NIV | 31,102 | 66 | 1,189 | Biblica |
| NKJV | 31,102 | 66 | 1,189 | Thomas Nelson |
| ESV | 31,103 | 66 | 1,189 | Crossway |

The rows are contiguous within their stored chapter sequences. The generic chapter metadata differs by one verse for 3 John 1 in KJV/NIV/NKJV and John 7 in SUV; ESV had no such mismatch. No translation corpus has an in-database content hash or stable upstream release identifier. The local JSON assets are a 179-verse starter sample, not a complete import source.

`docs/bible-source.md` asserts public-domain status for KJV and SUV and names upstream sources, but the stated source archives are outside this repository and no independent rights record or redistributor permission is present here. The database's `licenseInfo` strings are metadata, not proof of rights. NIV, NKJV and ESV have publisher names recorded, with no redistribution permission evidence. Therefore no real translation is approved for backend import. The import and publish endpoints stay disabled unless `PERAZIM_AI_SCRIPTURE_CORPUS_IMPORT_ENABLED=true`; the migration creates no grants. No Bible text was copied into the backend, downloaded, reconstructed, or sent to a model.

The loader in `scriptureCorpus.js` accepts a versioned manifest with translation, source/version, rights-evidence reference, SHA-256, book/chapter/verse counts, import time, and generated integrity result. It checks canonical book order and chapter totals for a real 66-book corpus, contiguous verse numbering, per-verse content hashes, row counts, and a canonical corpus hash. Synthetic Bible-like fixtures are accepted only when an explicit test-only option is passed; the HTTP import route never enables that option. Imports are staged, and a different authorized publisher must publish them. Database triggers prevent writing corpus books or verses after staging and prevent edits or deletion.

## Exact Scripture lookup

The backend accepts only explicit book/chapter/verse references, up to three passages and 50 verses per passage. It looks up a single published, authorized, integrity-validated corpus for one of the supported translation IDs (`KJV`, `SUV`, `NIV`, `NKJV`, `ESV`), checks each returned verse hash and sequence, and returns canonical book, chapter/range, translation, exact text, per-verse hashes, corpus version, and corpus hash. Missing or mismatched records return a limitation code; no model quotation is used as fallback. Topical Scripture search without an explicit reference is not implemented, so those questions do not reach the model through this route.

## Structured response validation

The online route requires both an eligible published Perazim source and exact Scripture evidence before generation. It requests JSON output using a provider response schema. The server validator then checks the three permitted categories, exact source/version/location citations, exact Scripture/corpus citations, quote substrings, Scripture references mentioned in the prose, institutional claim support from an official source class and matching evidence terms, prohibited medical/legal/financial advice, and fabricated personal testimony patterns. A failed response is replaced with a fixed limitation and escalation. Crisis terms take a deterministic safety path before theological retrieval and provider gates.

These deterministic checks establish provenance and enforce coarse boundaries; they do not prove that every theological inference is entailed by a cited passage. Formal source approval, human review, and the existing provider/privacy gates remain necessary. No source or authorized corpus is currently available, so normal online theological generation remains fail-closed.

The relay keeps the question in user content and signed evidence in a separate JSON evidence object. It verifies the short-lived grant, request and context hashes, source publication/hash fields, and Scripture verse hashes/sequences. It returns parsed structured output to the API for server validation. Android relay code was not changed.

## Verification and database status

Passed:

- `npm run test:governed-ai` — governed retrieval, authorization/step-up checks, transition/replay/withdrawal/supersession service behavior, synthetic corpus validation/import/lookup, structured response validation, online contract, and Worker authentication tests.
- `node relay/test/conversation.test.mjs` — signed grants, context tamper rejection, structured evidence, crisis path, provider failure behavior, and eight prompt-boundary cases.
- `npm run test:unit` — all 19 backend API contract integration tests, run against the test's own transient in-memory store and synthetic data.
- `node --check` on the changed JavaScript files and `git diff --check`.

PostgreSQL integration is **not verified**. No `psql`, `postgres`, `initdb`, or `pg_ctl` executable is available. Podman is present, but cannot initialize its configuration because `/run/user/1000/libpod` is on a read-only filesystem (`chmod`/sticky-bit setup fails). Migration `0013` and `0014` were not run against any database; no existing or production database was contacted. Real rollback, foreign-key, index, migration, and concurrent publication behavior therefore remain pending a disposable PostgreSQL environment. Service-level locks and SQL constraints are covered by synthetic tests and inspection only.

## Operational dependencies and next stage

1. Supply the September 2017 constitution, official adopted doctrine, and verifiable approval/delegation evidence through a controlled channel.
2. Verify redistribution rights and source/version provenance for an authorized Scripture corpus. Then build its manifest and enable import only after a separate rights review.
3. Run migrations `0013` and `0014` and execute persistence, rollback, and concurrent publication tests against a disposable local PostgreSQL cluster.
4. Assign technical source, review, and publication grants only under the established administrator safeguards. Keep institutional approval disabled until the actual responsible church authority is documented.
5. In a later stage, add topic-based exact Scripture retrieval and Android request/response integration only after the corpus, source authority, privacy, quota, and provider gates are approved.

No production deployment, migration, live provider call, member/pastoral data use, doctrine change, or Git push occurred.
