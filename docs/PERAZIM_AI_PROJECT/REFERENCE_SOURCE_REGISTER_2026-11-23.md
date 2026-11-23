# Reference source register for the on-phone guide (2026-11-23)

**Status: pending Levi's approval. Nothing here is used by the guide until Levi lists a source in
`src/main/assets/reference/approvals.json`.** Presence in the repository, in the reference database or in
this register is not approval (see `KNOWLEDGE_SOURCE_APPROVAL_MATRIX_2026-11-20.md`).

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

Size budget: default bundle at most 12 MB gzipped. Current: 5.8 MB. Smith and Easton (about 2.4 MB gzipped
together) can move to an optional download if Levi prefers a smaller install.

Overlap: the laptop thread is building a Strong's word-study lookup and a topical index. Nave's data here is
available to that index as the `naves-topical-bible` entries and should not be duplicated in a second index.
Strong's is already bundled and is not rebuilt here.

## B. Candidates not built yet

| Id | Source | Licence | Status | Theology / note |
|---|---|---|---|---|
| creed-apostles, creed-nicene, creed-chalcedon, creed-athanasian | Ecumenical creeds | Public domain text | Next step: clean transcription | Ecumenical. Levi's bank decisions: Nicene and Chalcedonian orthodoxy established |
| creed-wcf-1646, creed-canons-of-dort, creed-heidelberg, creed-thirty-nine-articles | Reformed and Anglican confessions | Public domain text | Next step: transcription from CCEL Schaff's *Creeds of Christendom* vol. III, not Wikisource | Tradition-specific. Pending Levi |
| matthew-henry | Matthew Henry's Commentary (CCEL, six volumes) | Public domain ("may be copied and distributed freely") | Optional large download | Puritan, Reformed |
| gill-exposition | John Gill's Exposition | Public domain (verify rights line) | Candidate | Strongly Calvinist, hyper-Calvinist tendencies. Sharp conflict with Arminian sources |
| wesley-notes | Wesley's Explanatory Notes (NT) | Public domain (verify) | Candidate | Arminian. Conflicts with Reformed sources |
| clarke-commentary | Adam Clarke | Public domain (verify host) | Candidate | Wesleyan Arminian. Conflicts with Reformed sources |
| jfb-commentary, barnes-notes | Jamieson-Fausset-Brown; Barnes | Public domain (verify host) | Candidate | Evangelical, Presbyterian |
| calvin-commentaries | Calvin's Commentaries (Calvin Translation Society) | Public domain (verify) | Optional large | Reformed |
| ante-nicene-fathers, npnf | Early church fathers (CCEL, Schaff ed.) | Public domain | Optional large | Early church; views pre-date later doctrine |
| tbesg-tipnr-stepbible | STEPBible lexicons and names | CC BY 4.0 per Tyndale House (verify) | Candidate, overlaps word study | Names could feed places |
| thayer-lexicon, bdb-lexicon | Thayer (1889), Brown-Driver-Briggs (1906) | Public domain (verify digital sources) | Candidate, overlaps word study | None |
| swahili-krapf-1850-nt | Biblia Takatifu, Krapf 1850 (New Testament portions) | Public domain (ebible.org) | Candidate, low value | Historic Kimvita orthography |

## C. Rejected or pending a decision

| Id | Source | Reason |
|---|---|---|
| swahili-union-version-1952 | Swahili Union Version (SUV) | Not cleared. Rights are held by the Bible Society of Kenya and the Bible Society of Tanzania (Bible.com lists the 2013 revised edition under their copyright). The reader loads a user-supplied file only. |
| swahili-biblia-takatifu-2019 | Door43 Swahili Unlocked Literal Bible (ebible.org) | CC BY-SA 4.0. Share-alike. Levi decides whether to accept share-alike text. |
| tyndale-open-study-notes | Tyndale Open Study Notes | Licence not verified in this session. Excluded until confirmed. |
| esv-niv-nkjv | ESV, NIV, NKJV | Excluded by Levi's decision: the AI uses KJV and NASB only. NASB is user-supplied only. |
| strongs-bundled | Strong's Greek and Hebrew (already bundled) | Greek licence unresolved in repo (header quotes GPL 3.0 and MIT). Levi must confirm. Not used by the AI. |

## D. What needs Levi

1. Approve or reject each source in section A. Approval means adding its id to `approvals.json`.
2. Decide on Smith and Easton: keep them in the default install (about 2.4 MB gzipped) or move them to an optional download.
3. Decide on share-alike text (Swahili 2019 under CC BY-SA 4.0).
4. Confirm the Strong's Greek licence.
5. Approve the confessions and creeds once they are transcribed and checked (section B).
