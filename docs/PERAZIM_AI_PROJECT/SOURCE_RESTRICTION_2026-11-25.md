# Source restriction for Perazim Guide (2026-11-25)

**Instruction (Levi, 2026-11-25):** the app's Christian teaching must come from a Bible-centred Protestant, Pentecostal and Evangelical position. Other religious or Christian traditions must not be presented as equally valid alternatives. Remove every source the AI can use as answering or resource material that does not come from that position. Answers must use everyday language a church member can follow.

**Update (Levi's answer, 2026-11-25, supersedes the removal below):** other faiths, churches and groups are kept only as false-doctrine examples, to be refuted from Scripture and never credible sources. The comparative knowledge base 1.5.22 is restored for that purpose. Creeds and early Church Fathers are Tier 2, used only where they agree with the Protestant, Pentecostal and Evangelical position. Every doctrine needs two Scripture passages. Coordinator decisions: Wesley's Notes kept, labelled Protestant (2A); Westminster Confession stays unloaded (3A); Schaff vol. III and the group-question policy stay out of answering material for now.

**Original decision (historical):** sources that describe other faiths, churches or groups were removed. Bible text, Bible-centred Protestant, Pentecostal and Evangelical reference material and the ecumenical Nicene Creed remain. Levi's approved doctrine positions are unchanged in substance.

## KEEP / REMOVE / BORDERLINE

### Scripture and Bible tools

| Source | Where | Class | Reason |
| --- | --- | --- | --- |
| King James Version (KJV) | assets/bible, kjv-verse-counts | KEEP | Bible text. Only translation the guide quotes, with NASB. |
| NASB | user-supplied file, not in git | KEEP | Bible text, Levi's approved translation. |
| ASV, BSB, WEB text files | assets/bible/text | KEEP | Bible text for the reader. Not used by the guide for quotations. |
| Interlinear Greek and Hebrew, Strong's | assets/bible/interlinear, strongs | KEEP | Bible word study. Not used for answers. Greek Strong's licence still open (register). |
| ESV, NIV, NKJV | none | REJECTED (already) | Levi's decision. |
| Swahili Biblia Takatifu 2019, Krapf 1850 NT | candidates | KEEP (candidate) | Bible text. Not loaded. |
| Swahili Union Version 1952 | candidate | REJECTED (already) | Rights not cleared. |

### Reference material

| Source | Where | Class | Reason |
| --- | --- | --- | --- |
| OpenBible cross-references and places | reference.db.gz | KEEP | Bible data, no teaching. |
| Smith's Bible Dictionary | reference.db.gz | KEEP | 19th-century Protestant reference. |
| Easton's Bible Dictionary | reference.db.gz | KEEP | 19th-century Protestant reference. |
| Hitchcock's Bible Names | reference.db.gz | KEEP | Names only. |
| Nave's Topical Bible | reference.db.gz | KEEP | Bible topics. |
| Nicene-Constantinopolitan Creed (381) | scripts/reference/creeds | KEEP | Ecumenical; accepted by Protestants. Approved by Levi 2026-11-23. |
| Apostles' Creed | library/creeds (not loaded) | KEEP | Ecumenical. Not loaded until Levi approves. |
| Matthew Henry commentary | reference-commentaries.db.gz (optional) | KEEP, labelled | Reformed Protestant. Approved by Levi; excerpts labelled with tradition. |
| John Wesley's Notes on the New Testament | reference-commentaries.db.gz (optional) | BORDERLINE | Protestant (Methodist) but Arminian. Approved by Levi. See decision 2. |
| Westminster Confession 1647 | library/creeds (not loaded) | BORDERLINE | Reformed Protestant confession, not approved. Stays unloaded until Levi approves. |
| Heidelberg, Dort, Thirty-Nine Articles, WCF, Athanasian, Chalcedon (candidates) | sources.json | KEEP (candidate) | Protestant or ecumenical. Not approved, not loaded. |
| Gill, Jamieson-Fausset-Brown, Barnes, Clarke, Calvin (candidates) | sources.json | KEEP (candidate) | Protestant commentaries. Not approved, not loaded. |
| Schaff, Creeds of Christendom vol. III (candidate) | sources.json | BORDERLINE | Contains Catholic and Orthodox creeds as well as Protestant ones. Use only Protestant chapters, if approved. |
| Thayer's Greek lexicon, BDB Hebrew lexicon, STEPBible lexicons (candidates) | sources.json | KEEP (candidate) | Neutral word tools. |
| Tyndale Open Study Notes (candidate) | sources.json | KEEP (excluded) | Evangelical, but licence not verified. Stays excluded. |
| **Ante-Nicene Fathers** (candidate) | sources.json | **REMOVED** | Early Christian writings used as teaching sources by Catholic and Orthodox churches. |
| **Nicene and Post-Nicene Fathers** (candidate) | sources.json | **REMOVED** | Same reason. |

### Comparative and drift material

| Source | Where | Class | Reason |
| --- | --- | --- | --- |
| Comparative knowledge base 1.0.0 (27 entries, draft) | assets/ai/guide/comparative (deleted) | **REMOVED** | Summaries, origins and sources describe Hindu, Buddhist, Jain, Catholic, Orthodox, Islamic, LDS, Jehovah's Witness, Adventist, Christian Science, New Age, secular and African traditional teaching. |
| Comparative knowledge base 1.5.22 (PR 76, 27 approved entries) | ai-eval/knowledge-package-unsigned/source (deleted) | **REMOVED** | Same. The approved entries used Catechism, Trent, Orthodox, Qur'an and other non-Protestant sources. |
| Scripture anchors for drift | assets/ai/guide/drift/scripture-anchors.json (new) | KEEP | Bible references only, taken from the Levi-approved entries. Each drift question quotes these KJV passages and nothing else about the other faith. |
| Canon entry (66 books) | comparative set | **REMOVED as entry** | Described Catholic and Orthodox canons. The Protestant 66-book canon stays as the draft doctrine topic canon-66-books (DRAFT_PENDING_LEVI). |
| Policy entries: church identity and group questions | comparative set | BORDERLINE | Levi approved a rule to describe a group's official teaching from its own sources. That conflicts with this instruction. See decision 1. Replaced by the drift playbook (decline to describe, point to Scripture). |
| Qur'an paraphrase (O-1 rules, PR 76) | comparative set | **REMOVED** | Qur'an text is not a Bible-centred source. |

### Approved doctrine and pastoral material

| Source | Where | Class | Reason |
| --- | --- | --- | --- |
| Approved doctrine entries (4) | library/doctrine/entries.json | KEEP | Positions unchanged. Other Christian views (Pentecostal, cessationist, others) are shown only when a question asks for them, and are weighed against Scripture. |
| Positions register | library/doctrine/positions.json | KEEP | Unchanged. |
| Kenya crisis contacts, legal summaries, pastoral topics | library/kenya, library/pastoral | KEEP | Not theological teaching. Kenyan law and contacts. Still pending Levi's release review. |

### Model

| Item | Class | Note |
| --- | --- | --- |
| Qwen2.5 0.5B weights | LIMIT | The model learned from general text. Removing files cannot remove that knowledge. The guard is retrieval only, the Bible-only rules, and fixed declines in no-go zones. The safety rerun must test whether other-faith material still leaks. |

## What changed in code and rules

- Deleted: comparative knowledge base (both versions) and its package source.
- Drift: DriftEvidence now sends KJV anchors only. The comparative note is gone. DriftDetector keeps its trigger words.
- Doctrine: other Christian views are added only when the question asks for them (DoctrineLibrary, OTHER_VIEWS_CUE). They are labelled "to be weighed against Scripture".
- Locked rules (Agents.md CORE, 1.1.0): rule 9 sets the source limit. The core stays within the 750-character guide cap.
- Prompt (LocalPrompt): Bible-only teaching, Bible view first, other views only when asked, plain English.
- Playbooks: doctrine.md and drift.md rewritten (1.1.0). Skills.md 1.3.0. Status.md updated.
- Unsigned package: other-faith documents and other-view text removed. Regenerated, 0 matches for "comparative".
- Source register: removed sources listed under `removedSources` with reasons.
- Tests: ScriptureAnchorsTest, RemovedSourcesTest (new), DoctrineLibraryTest (other views), ComparativeKbTest (removed).

## Not done here (needs a decision or another branch)

- Eval cases (ai-eval/perazim-guide, build_stress_cases.py) still list "comparative_kb" as an expected source. They are Levi's test set and were not edited. The next run will need the expectations updated.
- PR 76 (claude/project-thread-xc6xsh) and PR 80 (claude/knowledge-library) still contain the removed files on their own branches. This branch is based on the engine branch (PR 82 head). PR 76 must be rebased or regenerated on top of this work before it can carry the restriction.
- Safety and privacy rerun on the new drift and doctrine behaviour has not been run. PR 76 stays draft.

## Decisions for Levi

1. **Questions about another faith or group (for example "What do Catholics believe?")**
   - A. Decline to describe it and point to Scripture and the pastor. *Recommended. It follows the instruction most strictly.*
   - B. Give a short, labelled "what they believe" from general knowledge, then weigh it against Scripture. Not from any source.
   - C. Restore the approved comparative descriptions. *Not recommended. It conflicts with your instruction.*
2. **John Wesley's Notes (Arminian Methodist)**
   - A. Keep, labelled Protestant. *Recommended.*
   - B. Keep only Matthew Henry (Reformed).
   - C. Remove both commentaries.
3. **Westminster Confession 1647**
   - A. Keep unloaded until you approve it. *Recommended.*
   - B. Approve it now for answers.
