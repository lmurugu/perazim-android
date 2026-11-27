# 17 — OFFLINE BIBLICAL HERMENEUTICS LIBRARY SPECIFICATION

**Document status:** Owner-requested research and acquisition specification; not a corpus-use or doctrinal approval  
**Applies to:** Perazim offline AI, local Bible retrieval, local knowledge retrieval and knowledge-package preparation  
**Distribution:** Perazim church ministry  
**Storage:** Individual Android devices  
**Runtime:** Fully offline after local installation  
**Library objective:** The largest academically credible collection that can be lawfully acquired, quality-checked, packaged and used within Android storage and performance limits  
**Architecture:** Existing Java Android application, Room/SQLite, Qwen2.5 0.5B GGUF and llama.cpp runtime  
**Related specifications:** Documents 03, 04, 05, 11, 12 and 16

---

## 1. Purpose

This document establishes the requirements for building a substantial, downloadable, locally stored biblical research library for Perazim AI. Its purpose is to help the assistant interpret Scripture responsibly by retrieving biblical text, original-language evidence, morphology, syntax, lexical information, literary-genre guidance, historical and cultural sources, geography, textual evidence and carefully classified historical interpretation.

The target is an offline research capability, not a collection of links to websites. A resource is not integrated merely because a browser can display it. The approved content files, their indexes and the metadata needed to interpret them must be present on the device before the assistant can use them offline.

This specification describes candidate resources and the process for admitting them. It does not state that all candidates have already been downloaded, legally cleared, quality-checked, converted, installed or approved.

## 2. Owner decisions and fixed constraints

| Decision | Required value |
|---|---|
| Intended distribution | Perazim church ministry |
| Primary storage | Each user's Android device |
| Offline operation | Required after the resource pack has been installed |
| Collection ambition | Largest practical and academically credible collection |
| Primary local database | Existing Room/SQLite |
| Existing model/runtime | Preserve Qwen2.5 0.5B GGUF and llama.cpp unless a separately authorised, evidence-based change is approved |
| New production backend | Not authorised by this specification |
| Internet dependency during interpretation | None |
| Source authority | Scripture remains the supreme and final authority for Christian doctrine |

A church-only distribution does not automatically make every use non-commercial for copyright purposes. A CC BY-NC or other restricted resource must remain quarantined until the precise intended use and applicable law have been reviewed and approved.

## 3. Relationship to existing specifications

This document extends, and does not replace, the following requirements:

- Document 03 — Scripture is supreme for Christian doctrine; linguistic, historical, ecclesiastical and academic sources are supporting evidence, not rival doctrinal authorities.
- Document 04 — every knowledge item must retain provenance, authority classification, audience, licence, approval state, revision history and withdrawal status.
- Document 05 — offline AI must retrieve approved local knowledge and Scripture, preserve citations, respect access rules, and disclose local knowledge limitations.
- Document 11 — biblical accuracy, source authority, citation integrity, offline operation and human theological review require testing.
- Document 12 — agents must inspect the existing application and extend it incrementally without replacing the established stack.
- Document 16 — local storage, package and retrieval contracts must follow the approved Room/SQLite and knowledge-package architecture.

If any older document conflicts with this specification, agents must report the exact conflict. They must not silently change an owner decision, Bible translation decision, licensing decision or theological position.

## 4. Source-authority model

Every item must have both a topical collection and an authority classification. A source's academic reputation or technical sophistication must never cause it to be presented as Scripture or official Perazim doctrine.

| Classification | Permitted role |
|---|---|
| SCRIPTURE | Biblical text in an identified edition or translation, subject to textual and licensing metadata |
| ORIGINAL_LANGUAGE_TEXT | Hebrew, Aramaic or Greek textual witness or edition |
| LINGUISTIC_DATA | Lemmas, morphology, syntax, semantic roles, word senses and corpus evidence |
| LEXICAL_REFERENCE | Dictionary and lexicon evidence; not an independent doctrinal authority |
| HERMENEUTICS_METHOD | Methods for reading text, genre, context, discourse and figures of speech |
| HISTORICAL_PRIMARY_SOURCE | Ancient testimony, documentary papyri, inscriptions and other primary evidence |
| ARCHAEOLOGICAL_OR_GEOGRAPHICAL_DATA | Material, geographical and gazetteer evidence, with uncertainty retained |
| HISTORICAL_REFERENCE | Historical dictionaries, encyclopaedias and secondary reference works |
| HISTORICAL_THEOLOGY | Patristic, Reformation, Methodist and other historical Christian interpretation |
| COMMENTARY_OR_INTERPRETATION | A named author's interpretation, never a direct statement of what Scripture says |
| UNVERIFIED_CANDIDATE | Not eligible for normal production retrieval |

An item classified as historical or academic scholarship may inform what a source reports or what a scholar argues. It cannot establish Christian doctrine. A commentary must be attributed to its author and must not be converted into an anonymous Bible answer.

## 5. Mandatory interpretive safeguards

The assistant must follow the accompanying Hermeneutical Protocol whenever a question asks what Scripture says, what a passage means, or how a biblical teaching should be understood.

The following are non-negotiable:

1. Establish the passage's literary and paragraph boundaries before interpreting an isolated verse.
2. Identify the author, speaker, intended audience, historical setting and place in the biblical book where the evidence permits.
3. Identify the literary genre and any demonstrable subgenre or embedded literary form.
4. Establish the text being examined and distinguish the base text from editorial punctuation, translation choices, annotations and textual variants.
5. Examine grammar, morphology, syntax, clause relationships, discourse markers, conditions, purpose, result, concession, negation, commands, quotations and referents where relevant.
6. Determine lexical sense from contextual usage, not from a root, Strong's number or dictionary gloss alone.
7. Treat historical and cultural background as a claim requiring dated, located and attributable evidence.
8. Examine the immediate context and relevant canonical relationships before stating a theological conclusion.
9. Separate explicit textual statements, well-supported grammatical observations, historical inferences, interpretive conclusions and disputed hypotheses.
10. Do not manufacture certainty where data is absent, annotations disagree or scholarship is contested.

### 5.1 Lexical safeguards

The assistant must not commit the root fallacy: a word's etymology or historical origin does not, by itself, determine its meaning in a particular occurrence. It must consider the lemma, morphology, syntax, collocation, genre, author, date, corpus usage and immediate context.

Strong's numbers are identifiers and concordance aids. They are not semantic definitions. A lexicon's list of possible senses must not be copied wholesale into a verse explanation.

### 5.2 Grammar and syntax safeguards

A grammatical label must not be converted automatically into a doctrinal conclusion. In Greek, tense-form, aspect, voice, mood, case, participial function and clause structure must be considered together. In Hebrew, verbal forms, clause sequence, aspect, information structure, construct relationships, particles, conjunctions and discourse context must be considered.

Where two credible analyses exist, the assistant must state the alternatives and the evidence rather than presenting an annotation as infallible.

### 5.3 Genre and literary safeguards

The assistant must identify the dominant literary form while recognising embedded forms, such as a poem within narrative or a quotation within an epistle. It must not treat poetry as ordinary prose, proverbial generalisations as unconditional promises, apocalyptic imagery as ordinary historical description, or every prophetic expression as a prediction of the same kind.

A genre label is a reasoned classification, not a licence to disregard what the text actually says.

### 5.4 Historical and cultural safeguards

Historical claims must retain source, author or institution, date, geographical setting, type of evidence and uncertainty. A parallel from another culture or century does not prove borrowing, direct dependence or identical meaning. The assistant must not project a practice from one period, location or Jewish community onto another without evidence.

Primary sources are preferred for claims about what an ancient writer or document says. Modern reference works may guide discovery but should not replace the underlying evidence where available.

## 6. Resource families to acquire

The human- and machine-readable catalogue is maintained in the adjacent hermeneutics-library directory.

The collection should cover:

1. Biblical base texts and textual witnesses: Hebrew, Aramaic, Greek New Testament and Septuagint.
2. Morphology, lemmatization and word-level annotations.
3. Syntax trees, dependency relations, clause structures, discourse and participant reference.
4. Hebrew, Aramaic and Greek lexicons.
5. Biblical-language grammars and syntax references.
6. Genre, poetry, literary structures and figures of speech.
7. Jewish literature and Second Temple historical background.
8. Ancient Near Eastern, Greek and Roman primary sources.
9. Documentary papyri, inscriptions and archaeological evidence.
10. Ancient geography and historical gazetteers.
11. Biblical cross-references, quotations and allusions.
12. Historical Christian writings, Protestant commentaries and historical reference works.

The catalogue contains more than fifty candidate resources gathered during research. It is a discovery and acquisition register, not a blanket approval list. It must be updated when a repository moves, a licence changes, a release is superseded, a download route fails or an item is rejected.

## 7. Licence and rights gate

No content may be downloaded into a distributable Perazim package until its source and rights have been assessed at the actual file and component level.

### 7.1 Licence classes

| Class | Examples | Rule |
|---|---|---|
| OPEN_REUSE | CC0, CC BY, verified public-domain content | Eligible for review and integration after attribution, provenance and technical checks |
| SHAREALIKE | CC BY-SA | Quarantine until obligations for intended distribution and adaptations are reviewed |
| NONCOMMERCIAL | CC BY-NC, CC BY-NC-SA | Quarantine until owner/legal review confirms the intended church use is permitted |
| MIXED_COMPONENTS | A dataset combining material from several projects | Inspect each component's rights; a repository-level badge is insufficient |
| PERMISSION_REQUIRED | Proprietary text, images, apparatus or restricted edition | Do not ingest or distribute without written permission |
| UNKNOWN_OR_CONFLICTING | Missing, ambiguous or contradictory rights information | Reject from production packages until resolved |

The labels free, open access, downloadable, public on GitHub and available on Internet Archive do not prove redistribution rights. A software repository's code licence does not automatically license its dataset, documentation, images or third-party text.

### 7.2 Required rights evidence

For each file or component, record:

- Exact title and creator/contributor.
- Publisher or source institution.
- Edition, release, commit or version.
- Original download location and acquisition date.
- Actual file names and formats.
- Exact licence and version, or documented public-domain basis.
- Link to the licence statement and any notices included with the source.
- Required attribution and modification notices.
- ShareAlike or non-commercial obligations.
- Third-party components, exclusions and image/translation restrictions.
- Owner/legal review state where needed.

Keep the upstream licence file and notices with the acquired data. A transformed SQLite database must preserve the provenance and rights of every incorporated source.

### 7.3 ESV restriction — mandatory exclusion pending written permission

Crossway's current published ESV permissions state that ESV text may not be used, accessed or processed by an AI system for interpreting the ESV text or producing AI-generated responses based on it. Therefore:

- Do not download, ingest, index, embed, prompt with, train on, fine-tune on or otherwise process ESV text for Perazim AI interpretation without a written licence that expressly permits the intended AI use.
- The ESV preference recorded in the Decisions Register remains a preference subject to rights; this specification does not cancel or replace that decision.
- The ESV API is not an offline substitute and does not override the AI restriction.
- Until written permission is obtained, use a separately licensed Bible text for AI retrieval. The Berean Standard Bible's publisher has dedicated its text to the public domain. The World English Bible is another candidate whose actual edition/file terms must be retained.
- If no permitted translation is available for a particular language, the assistant must use approved source text that is lawfully available or disclose the limitation.

Official terms must be rechecked at each acquisition: https://www.crossway.org/permissions/

### 7.4 Existing Bible rights records

The existing docs/bible-source.md contains rights assertions for KJV and Swahili Union Version. This specification does not certify those assertions. Agents must verify the actual source files, edition, rights holder, jurisdiction and redistribution status before changing or repackaging the texts. Do not infer worldwide public-domain status from publication year alone.

## 8. Download and acquisition requirements

The source must be acquired from the publisher, institutional project, official Git repository, official release archive or documented archival record.

For each resource:

1. Inspect the official README, licence, notices and file-level metadata.
2. Pin a release, tag, commit or dated archival snapshot. Do not build reproducible packages from a moving branch without recording its exact commit.
3. Download the actual content files. A web reader or API endpoint alone does not satisfy offline availability.
4. Verify archive integrity and record SHA-256 checksums.
5. Preserve original file names, directory structure, licence notices and source metadata.
6. Inspect sample records and check completeness, encoding, verse coverage, Unicode handling and malformed content.
7. Record any OCR, machine-generated annotation, editorial uncertainty or known incompleteness.
8. Convert only after the source and rights gates pass.
9. Keep a reproducible conversion script and record input/output hashes.
10. Test retrieval with the device disconnected from all networks.

Never download an entire mixed-licence corpus merely because the archive is public. Sefaria, ORACC, Perseus, CDLI and similar aggregations require item-, project- or component-level rights filtering. Do not bulk-import restricted manuscript photographs or translations.

## 9. On-device architecture

Preserve the existing Room/SQLite architecture and offline model runtime. Do not introduce a replacement database, a new production service or a compulsory cloud retrieval endpoint.

Use separate logical stores or versioned tables for:

- Bible editions and verse text.
- Original-language tokens and lemmas.
- Morphology and syntactic relations.
- Lexical entries and senses.
- Cross-references and canonical relationships.
- Historical source records and excerpts.
- Book/chapter/verse and source-location indexes.
- Resource and package manifests.
- Licence/attribution records.
- Quality and verification status.

Use full-text search for prose documents and structured queries for verse, lemma, morphology, syntax and cross-reference data. Semantic/vector retrieval is optional and must not replace deterministic queries or authority filters.

Store large source packs in app-private on-device storage with Room/SQLite indexes. Do not place a very large corpus in Git or inflate every APK/AAB with every historical source. Core and supplementary packs may be separately installed from a locally supplied package, but after installation their retrieval must not require a network. The precise pack sizes and minimum-device storage budget must be measured before rollout.

A large upstream export, such as Sefaria's multi-gigabyte corpus, must be filtered to rights-cleared and ministry-relevant documents before any device pack is produced. Largest library means the largest lawful, verified and usable library, not the largest indiscriminate download.

## 10. Required resource metadata

Every source and derived resource must retain, at minimum:

- resource_id
- title
- creator_or_editor
- publisher_or_institution
- resource_type
- language
- edition_or_release
- source_repository
- source_url
- download_method
- acquired_at
- upstream_commit_or_version
- file_names
- file_formats
- sha256
- licence_name
- licence_url
- licence_scope
- attribution_text
- component_licences
- redistribution_status
- ai_processing_status
- quality_status
- reviewer
- reviewed_at
- limitations
- package_id
- package_version

Unknown values must be explicitly represented as unknown or pending. Agents must not invent an author, date, licence or checksum.

## 11. Retrieval and answer-grounding behaviour

For a Bible-related question, the offline retrieval pipeline should select evidence according to the question:

- Passage explanation: requested passage, paragraph boundaries and surrounding context.
- Word study: original-language occurrences, morphology, syntax, lexicons and contextually relevant corpus examples.
- Grammar question: token-level morphology, clause/syntax data and relevant grammar sections.
- Genre question: passage structure, literary features and an approved genre-method reference.
- Historical/cultural question: geographically and chronologically relevant primary sources, inscriptions, papyri and reputable secondary references.
- Cross-reference question: relevant canonical connections, with each target passage independently examined.
- Commentary/history question: named historical author, work, edition and page/section, clearly marked as that author's interpretation.

Each answer must distinguish:
- What the biblical text explicitly states.
- What the original-language evidence supports.
- What the historical source reports.
- What an interpreter infers.
- What is disputed or uncertain.

Citations must point to content actually retrieved from installed sources. The assistant must not cite an uninstalled catalogue link as though it had read the resource.

## 12. Quality assurance

Before a resource is marked production-eligible, validate:

- Textual coverage and verse alignment.
- Hebrew and Aramaic right-to-left rendering and Unicode normalization.
- Greek diacritics and Unicode normalization.
- Lemma and morphology field completeness.
- Parser/syntax-tree integrity.
- Reference and versification mapping.
- Search behaviour for inflected forms.
- OCR accuracy where applicable.
- Source-to-excerpt and source-to-page traceability.
- Duplicate and corrupted records.
- Attribution display.
- Licence filtering and package manifest.
- Device storage, query latency, memory use and battery impact.
- Offline retrieval with Wi-Fi and mobile data disabled.

Human review is required for theological claims, disputed grammatical analyses, historical claims that materially affect interpretation and any resource whose licence or source provenance is unclear.

## 13. Acceptance criteria

The library work is not complete until:

1. Every production resource has a rights-cleared, source-level record.
2. Every installed file is pinned to a version, release or commit and has a verified checksum.
3. All required licence and attribution notices are preserved.
4. No ESV text is processed by AI without written permission expressly covering that use.
5. No non-commercial, ShareAlike, mixed-licence or unknown-rights item is included without the required review.
6. The actual data—not merely a web link—is installed on the test device.
7. Retrieval functions with the device disconnected.
8. The assistant distinguishes textual evidence from lexical, grammatical, historical, commentary and doctrinal claims.
9. It does not infer meaning from etymology alone.
10. It does not treat Strong's numbers or annotations as authoritative interpretation.
11. It preserves source citations and uncertainty.
12. It passes representative Hebrew, Greek, genre, discourse, historical-background and cross-reference evaluation cases.
13. The existing app, Room migrations, Bible reader, local model and knowledge-package lifecycle remain intact.
14. Results are documented separately for unit, integration, device and disconnected-device tests.

## 14. Work that remains separately authorised

This document authorises preparation of the research catalogue and specification. It does not by itself authorise:

- Changing official doctrine or Perazim's approved theological positions.
- Replacing the preferred Bible translation decision.
- Redistributing ESV text or any other restricted translation.
- Bulk-importing non-commercial or mixed-licence corpora.
- Purchasing licences or services.
- Replacing Qwen, llama.cpp, Room or SQLite.
- Changing production signing keys, package trust or release configuration.
- Activating online/offline AI flags or deploying the AI.
- Adding large binary datasets to the Git repository.
- Treating every item in the candidate catalogue as approved.

Agents may perform reversible research, verify sources and prepare proposed acquisition manifests within current authorisation. Any rights ambiguity or reserved owner decision must be recorded explicitly.

---

**End of Document 17.**