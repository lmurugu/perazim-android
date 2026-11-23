# Reference source register for the on-phone guide (2026-11-23)

**Status.** Six factual sources (section A) are listed in `src/main/assets/reference/approvals.json`, approved
on the coordinator's technical decision after Levi asked for these resources to be added to the model's package.
Levi is to confirm. Doctrinal items (section E: commentaries and confessions) are not approved and are not
in `approvals.json`. Presence in the repository, in a database or in this register is not approval on its own
(see `KNOWLEDGE_SOURCE_APPROVAL_MATRIX_2026-11-20.md`).

Machine-readable register: `scripts/reference/sources.json`. Build: `scripts/reference/build_reference_db.py`.
Fetch raw files: `scripts/reference/fetch_reference_sources.py`.

Theology rule: commentaries, confessions and dictionaries carry their authors' theology. Each item is
"reference material, not Perazim's position". Doctrinal items stay pending until Levi approves them
(`AI_TEST_FRAMEWORK_2026-11-23.md`, governance drafts in PR 67). The apologetics bank in PR 71 is not
touched and does not reach the model.

## A. Built into the reference database (`reference.db`, 5.8 MB gzipped, 16.6 MB unpacked)

| Id | Source | Licence (verified where stated) | Rows | Size (raw) | Use | Theology tag |
|---|---|---|---|---|---|---|
| openbible-xrefs | OpenBible.info cross-references (seeded from the Treasury of Scripture Knowledge, public domain) | CC BY 4.0 (openbible.info site statement) | 344,799 | 2.0 MB | Verse cross-references | None |
| openbible-geo | OpenBible.info Bible places | CC BY 4.0 (site). Some coordinates come from OpenStreetMap (ODbL): attribution needed | 1,274 | 0.14 MB | Place names, coordinates, passages | None |
| smith-bible-dictionary | Smith's Bible Dictionary, 1884 (CCEL text) | Public domain (CCEL: "Rights: Public Domain") | 4,560 | 5.4 MB | Dictionary lookup | 19th-century Protestant reference |
| easton-bible-dictionary | Easton's Bible Dictionary, 1897 (CCEL text) | Public domain (CCEL: "Rights: Public Domain") | 3,963 | 5.5 MB | Dictionary lookup | 19th-century Protestant reference |
| hitchcock-bible-names | Hitchcock's Bible Names Dictionary, 1858 (CCEL text) | Public domain (CCEL) | 2,626 | 0.12 MB | Name meanings | None |
| naves-topical-bible | Nave's Topical Bible, 1921 (CCEL text) | Public domain (author died 1917) | 5,322 | 2.1 MB | Topic headings with references | None |

Approval: all six rows are approved in `approvals.json` (coordinator's decision, 2026-11-23). Smith and Easton
stay in the default install (coordinator's decision: 5.8 MB gzipped is acceptable).

Size budget: default bundle at most 12 MB gzipped. Current: 5.8 MB.

Overlap: the laptop thread is building a Strong's word-study lookup and a topical index. Nave's data here is
available to that index as the `naves-topical-bible` entries and should not be duplicated in a second index.
Strong's is already bundled in the app and is not rebuilt here.

## B. Candidates not built yet

| Id | Source | Licence | Status | Theology / note |
|---|---|---|---|---|
| gill-exposition | John Gill's Exposition | Public domain (verify rights line) | Candidate | Strongly Calvinist. Conflicts with Arminian sources |
| clarke-commentary | Adam Clarke | Public domain (verify host) | Candidate | Wesleyan Arminian. Conflicts with Reformed sources |
| jfb-commentary, barnes-notes | Jamieson-Fausset-Brown; Barnes | Public domain (verify host) | Candidate | Evangelical, Presbyterian |
| calvin-commentaries | Calvin's Commentaries (Calvin Translation Society) | Public domain (verify) | Optional large | Reformed |
| ante-nicene-fathers, npnf | Early church fathers (CCEL, Schaff ed.) | Public domain | Optional large | Early church; views pre-date later doctrine |
| tbesg-tipnr-stepbible | STEPBible lexicons and names | CC BY 4.0 per Tyndale House (verify) | Candidate, overlaps word study | Names could feed places |
| thayer-lexicon, bdb-lexicon | Thayer (1889), Brown-Driver-Briggs (1906) | Public domain (verify digital sources) | Candidate, overlaps word study | None |
| swahili-krapf-1850-nt | Biblia Takatifu, Krapf 1850 (New Testament portions) | Public domain (ebible.org) | Candidate, low value | Historic Kimvita orthography |

## C. Rejected or excluded

| Id | Source | Reason |
|---|---|---|
| swahili-union-version-1952 | Swahili Union Version (SUV) | Not cleared. Rights are held by the Bible Society of Kenya and the Bible Society of Tanzania. The reader loads a user-supplied file only. |
| swahili-biblia-takatifu-2019 | Door43 Swahili Unlocked Literal Bible (ebible.org) | CC BY-SA 4.0 (share-alike). Parked as a later candidate (coordinator decision). Levi decides whether share-alike text is accepted. |
| tyndale-open-study-notes | Tyndale Open Study Notes | Licence not verified. Excluded until confirmed. |
| esv-niv-nkjv | ESV, NIV, NKJV | Excluded by Levi's decision: the AI uses KJV and NASB only. NASB is user-supplied only. |
| strongs-bundled | Strong's Greek and Hebrew (already in the app) | Coordinator decision: use the Greek data under MIT with attribution. The upstream licence is not re-verified in this session. Not used by the AI. |

## D. Strong's Greek licence (coordinator decision, 2026-11-23)

The Strong's Greek data is used under MIT with attribution (coordinator's decision). The repository header quotes
both GPL 3.0 and MIT, so the upstream licence should still be checked once against the source before release.
This is not verified here.

## E. Commentaries: approved by Levi (answer A), bundled in the APK. Confessions: not yet extracted

| Id | Source | Licence | Tradition tag | Build status |
|---|---|---|---|---|
| matthew-henry-commentary | Matthew Henry's Commentary on the Whole Bible (CCEL, six volumes) | Public domain (CCEL: may be copied and distributed freely) | PURITAN_REFORMED_COMMENTARY (Reformed, Puritan) | Optional database, 22,678 entries, 49 MB raw. Approved in approvals.json |
| wesley-notes-nt | John Wesley's Explanatory Notes upon the New Testament (CCEL) | Public domain | ARMINIAN_WESLEYAN_COMMENTARY (Arminian, Wesleyan) | Optional database, 3,339 entries, 6.7 MB raw. Sections are the 67 "Notes on ..." titles and the Preface. Approved in approvals.json |

The commentary database is built with `--tier optional` and bundled as `src/main/assets/reference/reference-commentaries.db.gz` (coordinator decision: bundle, no download, the repository is private). It is 16.4 MB gzipped (sha256 9eb494a0c5f5144af911835f7ecc30f575ef218296d02033c21d0fbcbfc0a1e6) and is staged at
`/mnt/project-files/reference/reference-commentaries.db.gz`. The default factual build is unchanged at 16,564,224 bytes.
Retrieval labels each excerpt with its author and tradition (`ReferenceEvidence.commentaryText`) and adds commentary last, after the governed position and the factual references (`ReferenceGuide.augment`). Host tests cover the labels and chapter headwords; the Android path compiles against stubs only, and the laptop thread tests it on the device.
Commentaries carry their authors' theology and conflict with each other (Henry is Reformed, Wesley Arminian). Levi's answer A: use both, labelled, Perazim's view first.

Creeds and confessions (CCEL Schaff, *Creeds of Christendom*):

- **Extracted and checked: the Nicene-Constantinopolitan Creed of 381** (`scripts/reference/creeds/nicene-constantinopolitan-381.txt`, 218 words). It is the clean English block in CCEL volume I (creeds1.txt). The text matches the source word for word, except the editor's gloss "(æons)", which is omitted. It is **pending Levi's approval** and is not in `approvals.json` or any database.
- **Not extracted: the Apostles' Creed.** The English received text sits in a table of variant readings with bracketed additions. Extracting it needs a choice between the Roman and received forms, which is a doctrinal decision for Levi.
- **Not extracted: the Athanasian Creed.** The CCEL volumes carry the Latin and the commentary, but no clean English text was found.
- **Not extracted: the Westminster Confession, Thirty-Nine Articles and Canons of Dort.** Volume III interleaves Latin, 1562 English and 1801 English on the same lines, so extraction needs a manual split that I have not done.

Nothing has been written from memory. Each remaining creed needs a clean source before it can be checked.

## F. What needs Levi

1. Confirm the six approved factual sources in section A (`approvals.json`, approved on the coordinator's decision).
2. Nothing further on hosting: the commentaries are bundled as an asset (coordinator decision).
3. Decide on share-alike text (Swahili 2019 under CC BY-SA 4.0). Parked for now.
4. Confirm the Strong's Greek licence (section D), which is MIT with attribution by coordinator decision and not yet re-verified upstream.
5. Approve the confessions and creeds once they are transcribed and checked (section E).
