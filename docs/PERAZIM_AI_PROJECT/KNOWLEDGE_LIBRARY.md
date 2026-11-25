# Perazim Guide: Knowledge Library

Status: DRAFT for Levi's review. Branch `claude/knowledge-library`, base `eval/integrated-2026-11-23`.
Machine-readable map: `src/main/assets/ai/library/INDEX.json` (generated).

## 1. What the guide is made of

The offline guide is four parts. The model is only one of them.

| Part | What it is | Where it lives | Status |
| --- | --- | --- | --- |
| 1. Rules and guideline documents | Locked operating rules, status, playbooks | `src/main/assets/ai/guide/` (Agents.md, Status.md, Skills.md, playbooks/) | In the app. Loaded by `GuideDocs`. Unchanged in this PR |
| 2. Skills | Skill index with triggers, self-check (SH), self-learning (SL) | `guide/Skills.md` | In the app. Unchanged in this PR |
| 3. The model | Qwen2.5 0.5B Instruct Q4_K_M, 491,400,032 bytes, pinned SHA-256 | Model download (not in APK) | Same size only. No retraining |
| 4. Knowledge library | Every checked material the model may use | `src/main/assets/ai/library/` (this PR) and the bundled reference databases | Started here. See section 3 |

Rule the library enforces: the model only words answers. Facts, verses and positions come from checked material
(verbatim retrieval, approved positions, fixed answers). If the material is not there, the guide declines or says so.

## 2. Inventory of what already exists (before this PR)

| Asset | Used by | Size | What it covers |
| --- | --- | --- | --- |
| `assets/bible/text/KJV.tsv.gz` | Bible reader, `OfflineScriptureLookup` | 31,102 verses | Verbatim KJV text (public domain). Also WEB, ASV, BSB, not used by the guide |
| `assets/bible/kjv_verses.json`, `starter_verses.json`, `starter_chapters.json` | Seeding, starter verses | small | Starter passages |
| `assets/reference/reference.db.gz` | `ReferenceIndex` | 5.8 MB gz | Cross-references (OpenBible, CC BY 4.0), places, Smith's, Easton's, Hitchcock's, Nave's Topical, Nicene Creed 381 |
| `assets/reference/reference-commentaries.db.gz` | `ReferenceIndex` (optional) | 16.4 MB gz | Matthew Henry and Wesley NT notes, labelled by tradition. Approved by Levi (answer A), optional download |
| `assets/reference/approvals.json` | `ReferenceApprovals` | small | The 9 approved source ids |
| `assets/ai/guide/drift/scripture-anchors.json` | `DriftEvidence` | Bible references only, 27 concepts | Replaces the comparative knowledge base, which was removed 2026-11-25 (see SOURCE_RESTRICTION_2026-11-25.md) |
| `assets/ai/guide/playbooks/*.md` | `GuideDocs` | 3.7 KB total for 11 files | One short playbook per skill |
| `ai-eval/perazim-guide/cases.jsonl` | Laptop benchmark | 532 cases, 129 scripture references | Eval set |
| `scripts/reference/sources.json` | Reference builder | 35 rows | Every candidate and approved source, with licence |

The playbooks are the thinnest part: the whole pastoral playbook is 404 bytes. Most of the eval gaps come from
having almost no checked material for the model to work from.

## 3. The library (new in this PR)

```
src/main/assets/ai/library/
  INDEX.json                      manifest: parts, files, SHA-256, what is not yet moved
  scripture/
    kjv-verse-counts.json         verse count of every chapter of all 66 books, built from the bundled KJV
  doctrine/
    positions.json                topic status only: APPROVED, UNDER_REVIEW, UNDECIDED, DRAFT_PENDING_LEVI
  kenya/
    crisis-contacts.json          Kenyan numbers with their recorded verification status
  sources/
    register.json                 35 sources: licence, URL, APPROVED_FOR_GUIDE or NOT_APPROVED
  creeds/
    apostles-creed.txt            DRAFT_PENDING_LEVI. Public domain text, source in SOURCE.md
    SOURCE.md                     where the text came from and its status
```

Scripts (run from the repo root):

- `scripts/knowledge/build_scripture_index.py` rebuilds `scripture/kjv-verse-counts.json` from `KJV.tsv.gz`.
- `scripts/knowledge/build_library_index.py` rebuilds `sources/register.json` and `INDEX.json`.
- `scripts/knowledge/check_eval_references.py` checks each eval case's `scripture_refs` against the index.
- `scripts/knowledge/test_knowledge_library.py` runs Python checks (`python3 -m unittest scripts/knowledge/test_knowledge_library.py`).

Host tests (Java, run by Gradle CI):

- `ScriptureRangeTableTest`: non-existent chapters and verses are rejected (Ezekiel 50:4, Ezekiel 48:36, Jude 2:1).
- `KnowledgeLibraryTest`: every indexed file exists, parses and matches its SHA-256; the doctrine register holds
  no position text (`contentFile` is null everywhere, so it fails closed); only approved sources are marked usable;
  Kenyan rows use only the recorded statuses.

Nothing in the library is read by the runtime yet. The existing loaders are unchanged, so the app behaves exactly as
before this PR. The engine owner wires each part in (section 6).

### Why these parts

- **scripture/**: the model cannot count chapters. The range table lets the engine reject a reference before it is
  quoted or explained. This closes the "invented verse" failures (eval `FW-079`, `Ezekiel 50:4`).
- **doctrine/**: stops the guide stating a contested position as settled. Each topic carries Levi's recorded status.
  The text of positions is not in this file. Content enters only when Levi approves it.
- **kenya/**: the crisis numbers, each with the status it was recorded with. A number not listed here is never shown.
- **sources/**: one register for licence and approval, so a source cannot be used by accident.

## 4. Gap list by eval category

Numbers are the laptop partial rerun of 2026-11-24 (`ai-eval/runs/2026-11-24/partial_table.md`). Automatic term
checks only, not final. Case counts are from `cases.jsonl`.

| Category | Cases | Now pass | What is missing | Closed by this PR? |
| --- | ---: | ---: | --- | --- |
| biblical_accuracy_hallucination | 26 | 0/26 | Runtime must check every reference against the range table and quote only verbatim KJV | Partly. Index added. Engine must use it |
| research_source_verification | 18 | 0/18 | Citable research sources. Kenya Law summaries not added (site unreachable from this session) | No |
| soteriology_apostasy | 32 | 4/32 | Approved position text for apostasy and assurance. Levi's view is UNDER_REVIEW | No. Needs Levi (A1) |
| israel_church_eschatology | 24 | 3/24 | Approved text for pre-trib premillennialism and Israel. UNDER_REVIEW | No. Needs Levi (A1) |
| historical_apologetics | 24 | 10/24 | Approved apologetics. PR 71 bank is isolated and not used here | No. Needs Levi |
| holy_spirit_pentecostal | 26 | 7/26 | No topic in the doctrine register yet. Positions not recorded | Register entry needed. Needs Levi (A2) |
| denominational_fairness | 16 | 5/16 | Comparative KB here is 1.0.0 (draft). The 1.5.22 set in PR 76 is not on this base and is pending Levi | No. Needs Levi |
| scripture_exegesis | 42 | 10/42 | Passage context. Approved commentaries exist (optional DB) but are not wired for every book | No. Engine wiring |
| safety_crisis_safeguarding | 36 | 23/36 | Must reach 100%. Crisis answers are fixed code. Numbers 922 (partly verified), 1195 (unconfirmed) | Numbers recorded. Release blocked until 100% |
| privacy_confidentiality | 30 | 6/30 | No knowledge asset. Enforced in code. Privacy playbook is 323 bytes | No. Engine and playbook |
| language_pastoral_tone | 27 | 12/27 | Only 7 Swahili cases. Warm Kenyan phrasing has no material. Pastoral playbook is 404 bytes | No. Waiting for the Kenya research thread |
| pastoral_struggle | 45 | 24/45 | Scripture for pastoral topics. Nave's Topical (approved) can supply verse lists, not yet indexed | No. Next step |
| church_authority_boundaries | 21 | 12/21 | Church roles and office contacts not approved | No. Needs Levi |
| role_boundaries_refusal | 24 | 16/24 | Refusal playbook is 279 bytes | No |
| general_intelligence_scope | 51 | 40/51 | Scope gate is code. Refusal wording | No |
| contradiction_consistency | 17 | 2/17 | Needs approved positions to stay consistent with | No. Depends on doctrine |
| reasoning_critical_analysis | 13 | 6/13 | Argument guidance in the doctrine playbook only | No |
| spiritual_manipulation | 18 | 10/18 | Playbook coverage | No |
| tool_use_execution | 22 | 18/22 | App help and app context playbooks | No |
| memory_continuity_privacy | 4 | 1/4 | Privacy rules are code | No |

Other gaps:

- Kenyan question types. The eval set has 11 `kenya`-tagged cases and 7 Swahili cases out of 532. The parallel thread
  "Research the questions Kenyan believers ask" will add topics when it reports.
- Kenya legal summaries (Constitution, Children Act, Sexual Offences Act, Mental Health Act). Not added: Kenya Law
  was unreachable from this session (HTTP 503 and 202). Needs a retry from a working network, with citations and a
  licence check before entry.
- Westminster Confession (1647). Not added yet. The CCEL text is interleaved with Latin and apparatus, so it needs a clean English-only pass before it can be checked. Nothing written from memory.
- Assemblies of God Statement of Fundamental Truths. Not added. The publisher's page states "All rights reserved", so it is excluded.
- The 0.5B model drifts in quotation wording. The library lowers the risk; the self-check still has to catch the rest.

## 5. Decisions needed from Levi

Each question has a recommendation. Decide with the number.

1. **Apostasy, unconditional election and pre-trib views (soteriology and eschatology gaps).**
   - A. Approve Levi's developing view as the stated position, marked under review, with the Perazim view first. (Recommended.)
   - B. Keep them UNDECIDED. The guide only explains the question and gives no view.
   - C. Other. Please say what you want.
2. **Holy Spirit and tongues topics.** These are not in the register yet.
   - A. Add them as UNDECIDED until you record a position. (Recommended.)
   - B. Add them with a position you give me now.
   - C. Other.
3. **Kenya Law summaries.** Needed for the research category.
   - A. Retry the fetch when the network allows, then add summaries marked DRAFT_PENDING_LEVI. (Recommended.)
   - B. Skip Kenya legal content for this release.
   - C. Other.
4. **Commentaries and creeds beyond Nicene 381.**
   - A. Keep as they are. Commentaries stay optional. No new creeds. (Recommended.)
   - B. Extract the Apostles' Creed and the Westminster Confession now (both public domain), pending approval.
   - C. Other.
5. **Crisis number 1195 (gender-based violence line) and 922.** Both are not fully verified.
   - A. Keep both shown with their status, and ask you to confirm before the release gate. (Recommended.)
   - B. Remove 1195 until confirmed.
   - C. Other.

## 6. Hand-off to the engine thread

The laptop thread "Benchmark the offline AI guide against similar models" owns routing and the runtime. It can wire these
in, in this order:

1. `ScriptureRangeTable` in the reference path, before any verse is quoted or explained (closes FW-079 class failures).
2. `doctrine/positions.json` status in routing: no position text is shown for any topic that is not APPROVED.
3. `kenya/crisis-contacts.json` as the only source of Kenyan numbers in crisis text (replace the hard-coded list in
   `PerazimAiEngine` once the statuses are agreed).
4. `sources/register.json` as the approval check, replacing the list in `approvals.json` once both agree.

## 7. Source register additions in this PR

No new external content was added in this PR. The 35 rows in `sources/register.json` are the existing candidates and
approved sources from `scripts/reference/sources.json`, unchanged. Each row keeps its licence and URL. Any new source
will need a row here with licence, URL and the date it was checked before it is used.

## 8. This round (after the coordinator's follow-up)

- **Crisis contacts** carry `approvedByLevi: 2026-11-23` (Levi's answer 9A). The verification column stays as recorded, so 922 is still partly verified and 1195 is still unconfirmed.
- **Doctrine register**: each of the 15 topics now has a `draft` block. Wording comes only from Levi's recorded decisions. Every draft is DRAFT_PENDING_LEVI. Undecided topics say that no position is set. The Holy Spirit and tongues topics stay UNDECIDED while Levi decides.
- **Apostles' Creed** (received form, Schaff 1877, public domain via CCEL) is in `creeds/`, marked DRAFT_PENDING_LEVI. No loader reads it.
- **Treasury of Scripture Knowledge** cross-references are already in the approved `openbible-xrefs` source (OpenBible.info, CC BY 4.0, seeded from the public-domain TSK). No second copy is added.
- **Easton's Bible Dictionary** is already approved and in `reference.db`. No change.
- **Westminster Confession (1646, English)**: added to `creeds/` as DRAFT_PENDING_LEVI. The text is the 1946 Carruthers transcription on Wikisource, which states the work is public domain. The transcription is under CC BY-SA 4.0 page terms. It is a historical confession, not a Perazim position. Its chapters cover divorce, the Sabbath and civil magistrates, which are UNDECIDED topics here, so it must never be shown without that label.
- **Assemblies of God Statement**: not added. The publisher's page says "All rights reserved".
- **Constitution of Kenya 2010 summaries** (Articles 26, 28, 29, 43(1), 53(1)) are in `kenya/legal-summaries.json`, DRAFT_PENDING_LEVI. The text comes from the Constitute Project mirror. Sub-paragraph letters need checking against Kenya Law.
- **Kenya Act summaries** (Children Act 2022, FGM Act 2011, and others) are still not added. Kenya Law's Act PDFs return HTTP 403. I did not get around the block.
- **Kenya research thread** (draft PR 81, `docs/PERAZIM_AI_PROJECT/KENYA_QUESTION_LANDSCAPE.md`) has been received. Its topics are not added as content here. Its verified-resource list conflicts with the 1195 status. See `reviewNote` in `kenya/crisis-contacts.json`.


## 9. Round four (coordinator decisions, 2026-11-24)

- **1195 (gender-based violence line)** keeps Levi's 2026-11-23 decision. It is shown only as a secondary line, after 999 and 112, labelled "may not be current". It is never the only number. The rule is in `displayRule` in `kenya/crisis-contacts.json`. It is on the pre-release confirmation list in the same file. Levi is not asked again now.
- **Act summaries** (Children Act 2022, FGM Act 2011 and others) stay an open gap. Kenya Law's Act PDFs return HTTP 403.
- **Kenyan pastoral topics** (`pastoral/kenya-topics.json`, DRAFT_PENDING_LEVI, fail-closed):
  - Debt, loans and betting.
  - Fear of witchcraft and curses.
  - False prophets and miracle claims.
  - Depression, anxiety and the stigma around them.
  Every verse is taken from the bundled KJV by `scripts/knowledge/build_pastoral_topics.py`. The build stops on any reference the range table rejects, and the Python test checks the committed file against a fresh build. The text is written neutrally, with no verdict on any person and no position on any contested doctrine.
- **Gap**: no verified helpline for debt or betting is recorded. The topic says so and refers the reader to a trusted church leader. A gambling or debt-counselling number needs a source check before it can be shown.
- **Waiting on Levi's four doctrine answers** (prosperity and tithing, polygamy, the Sabbath, dowry). The coordinator will relay them. No position is added until then.
