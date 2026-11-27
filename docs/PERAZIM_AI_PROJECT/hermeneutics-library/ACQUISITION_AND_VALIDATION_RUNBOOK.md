# Hermeneutics Library — Acquisition and Validation Runbook

## 1. Scope

This runbook is for authorised agents preparing the Perazim offline biblical research packs. It applies to every candidate in RESOURCE_CATALOG.json.

**Target:** downloadable files stored on each Android device and available to local retrieval without a network.

**This runbook does not authorise** licence purchases, restricted-text ingestion, ESV processing, production signing changes, AI flag activation or deployment.

## 2. Before downloading anything

For each candidate:

1. Open the official source/repository and inspect its current README, licence, notices and download instructions.
2. Verify that the resource is actual downloadable content, not only a reader, website, API or online query tool.
3. Inspect the rights of the exact data files. If the repository contains third-party data, inspect the component licence for every imported component.
4. Check whether the licence permits copying, redistribution, modification, offline storage and AI access/processing for the intended distribution.
5. Record the exact project/version/tag/commit and acquisition date.
6. If the licence is non-commercial, ShareAlike, unknown or ambiguous, place it in quarantine and record the owner/legal decision needed.
7. If no licence can be identified for the exact file, do not put it in a distributable pack.

Do not use the code licence as a proxy for the data licence.

## 3. Downloading Git repositories

For a Git-hosted source, use its official repository and pin the exact commit before making a release package.

Example acquisition pattern:

~~~bash
git clone --depth 1 https://github.com/STEPBible/STEPBible-Data.git upstream/stepbible-data
cd upstream/stepbible-data
git rev-parse HEAD
find . -maxdepth 3 -type f | sort
~~~

The command is an example only. Use the URL and branch/release recorded for the actual resource. After validating a release, record its commit hash and do not silently rebuild against a moving default branch.

For a repository archive, save the source URL, release/tag, archive filename, retrieval date and SHA-256. Do not treat a ZIP URL as a guarantee that the branch name or release still exists.

## 4. Downloading archive/library items

For Internet Archive, Project Gutenberg, Zenodo, institutional libraries or publisher download pages:

- Pin the item identifier, edition, file name, version and publication data.
- Prefer structured text (XML, USFM, USX, JSON, TSV, TEI) where available.
- Preserve the title page, imprint, licence notice and bibliographic metadata.
- If a PDF or scan is the only available source, record page mapping and OCR status.
- Do not assume a site's hosting permission proves redistribution or derivative-database permission.

A page that permits reading or download is not sufficient evidence of permission to bundle the material in Perazim.

## 5. File integrity and provenance

For each acquired file, record:
- Original source URL and download date.
- Exact file name and file size.
- SHA-256 checksum.
- Source repository commit/release or archive item identifier.
- Exact licence and file/component scope.
- Required attribution and notices.
- Converter name/version and parameters, if transformed.
- Output file name, size and SHA-256.
- Quality-review results and reviewer.

Example checksum command:

~~~bash
sha256sum source-file.zip
~~~

Do not edit upstream source files in place. Preserve an untouched source copy and store transformations as derived outputs with an explicit source relationship.

## 6. Content-quality review

### 6.1 Biblical and linguistic datasets

Validate:
- Canon/book/chapter/verse coverage.
- Edition and textual basis.
- Word/token alignment.
- Lemma mapping and morphology completeness.
- Syntax-tree well-formedness and parent/child relationships.
- Reference/versification consistency.
- Textual variant provenance.
- Correct distinction between text, annotation and translation.
- Search across inflected word forms.
- Hebrew/Aramaic right-to-left display.
- Hebrew and Greek Unicode normalisation; do not strip diacritics from source display text.
- Greek polytonic characters and punctuation.
- No silently lost characters during TSV/XML/JSON/SQLite conversion.

### 6.2 Books and historical sources

Validate:
- Title page and bibliographic identity.
- Author, editor, translator and edition.
- Complete volume/part/chapter coverage.
- Page-level citation mapping.
- OCR quality on a sampled set of pages.
- Footnotes, tables and source references.
- Whether the text is primary evidence or a modern author's report/interpretation.
- Historical date, location and evidence type.

Machine OCR and AI-extracted texts must be marked as such. They are not automatically proofread scholarly editions.

### 6.3 Historical claims

Do not use a historical source without preserving date and geography. An inscription from a later century or different province does not automatically establish a first-century Judean practice. A later Rabbinic source must not be silently projected into the time of Jesus.

## 7. Rights and attribution

Each pack must carry a human-readable attribution document and a machine-readable manifest.

Minimum attribution fields:
- Resource title.
- Authors/contributors.
- Publisher or institution.
- Source URL.
- Edition/release/commit.
- Licence name/version and URL.
- Required attribution wording.
- Changes made.
- Component-level rights.
- Restrictions and excluded files.

CC BY requires attribution and disclosure of changes where applicable. CC BY-SA can impose ShareAlike requirements on adaptations. CC BY-NC must not be assumed suitable merely because the application is church-only. Mixed collections must be filtered at the work/file level.

Never remove or overwrite upstream licence notices.

## 8. Special source controls

### ESV

Crossway's current public permissions prohibit AI systems from accessing or processing ESV text for interpretation or AI-generated responses based on that text. Do not ingest, index, retrieve, embed or prompt with ESV until the owner obtains written permission expressly covering AI processing and offline distribution. The ESV API does not provide offline rights.

Official terms: https://www.crossway.org/permissions/

### Sefaria

The export repository is a download index/tooling layer; the text corpus is much larger and each text version has its own licence metadata. Clone the index, then fetch only selected text files after reading their individual licence metadata. Never treat the complete export as uniformly licensed.

Official export: https://github.com/Sefaria/Sefaria-Export  
Licence file: https://github.com/Sefaria/Sefaria-Export/blob/master/LICENSE.md

### MACULA

Greek and Hebrew repositories include linguistic datasets and may also include or depend on third-party source datasets. Preserve the repository's licence file and inspect each included source/component. Do not represent every component as a single unqualified CC BY 4.0 item.

Greek: https://github.com/Clear-Bible/macula-greek  
Hebrew: https://github.com/Clear-Bible/macula-hebrew

### STEP Bible Data

The STEP Bible data project states CC BY 4.0 and provides downloadable tab-separated data. Retain required attribution, source lineage and any per-dataset notices.

Source/licence: https://stepbible.github.io/STEPBible-Data/

## 9. Preparing Android resource packs

The Git repository stores specifications, scripts, manifests, tests and attribution templates. It must not store the full third-party corpora or giant archives.

The acquired content should be prepared as versioned local packs outside Git, subject to licensing. The existing Android application should:

1. Store approved packages in app-private device storage.
2. Verify package integrity and signed metadata according to the existing package lifecycle.
3. Import structured datasets into the approved Room/SQLite layout or attach versioned read-only SQLite/FTS databases where technically justified.
4. Preserve source metadata and licences alongside indexed content.
5. Activate packages atomically, retaining the last valid package if an update fails.
6. Retrieve from installed files only; no website/API fallback is permitted for a claimed offline answer.
7. Indicate when a source is absent, withdrawn, expired or out of date.

Do not inflate the APK/AAB with the entire research library. Use core and separately installable supplementary packs. Their acquisition/installation may happen before the user goes offline; all interpretation after installation must remain local.

Large corpora must be filtered before packaging. For example, Sefaria's full export is not an appropriate default install on every phone; select relevant, rights-cleared texts and measure the resulting pack size.

## 10. Proposed local index fields

Every indexed record should preserve:
- resource_id and resource_version;
- source author/editor/institution;
- title and edition;
- source URL and acquisition date;
- original file and source checksum;
- licence and attribution;
- authority classification;
- resource type and language;
- biblical reference, lemma or stable source record ID;
- section/page/line reference;
- extracted content and extraction method;
- quality status and limitations;
- package ID/version;
- review/approval status.

Unknown fields must remain explicitly unknown or pending. Do not create fictitious page numbers or textual references.

## 11. Minimum offline acceptance tests

Test on a representative Android device with both Wi-Fi and mobile data disabled.

Required test groups:
- Hebrew lookup by lemma and inflected form.
- Greek lookup by lemma and inflected form.
- Display Hebrew script correctly right-to-left.
- Display polytonic Greek with diacritics intact.
- Retrieve a word's morphology and syntactic relations.
- Follow a pronoun/participant reference when the dataset supplies it.
- Find relevant cross-references and open each passage locally.
- Search historical documents and return the exact author/title/page or record.
- Distinguish an ancient primary source from a later commentary.
- Exclude unapproved, restricted, expired or withdrawn material.
- Preserve citations through Qwen prompt construction and the displayed answer.
- Disclose when a required dataset is not installed.
- Verify that no network request is needed to retrieve an installed source.

The evaluation must also include root-fallacy, Strong's-number, tense-overclaim, genre-blindness, historical-anachronism and fabricated-citation tests from the Hermeneutical Protocol.

## 12. Release gate

A candidate becomes APPROVED_FOR_PACKAGING only after:
- source route verified;
- exact licence and component rights approved;
- file integrity verified;
- quality checks passed;
- conversion reproducible;
- metadata and attribution complete;
- human review complete where required;
- offline device tests passed;
- package manifest and version recorded;
- owner-controlled decisions documented as resolved.

Until then, keep the resource in candidate or quarantine status.