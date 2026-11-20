# Knowledge source approval matrix (2026-11-20)

**Status: awaiting owner decision for every row. No source is approved. Approval of one source does not approve any other.**

Allowlist principle: a document enters the official retrieval collection only when it has an explicit "Approved" entry here. Presence in the repository is not approval.

| # | Source (repo path) | Version / date | Origin / author | Authority | Category | Currency | Conflicts | Intended use if approved | Approval | Restrictions |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `docs/PERAZIM_AI_PROJECT/02_AI_IDENTITY_AND_BEHAVIOUR.md` | Section 21 added 2026-11-20 | Owner-authored spec | Owner | Standing instructions (not church information) | Current | None | Governs assistant behaviour only | **Approved** (instructions only, owner 2026-11-20) | Not a source of church information |
| 2 | `docs/PERAZIM_AI_PROJECT/*` (other specs 00–20) | Undated | Owner-authored | Owner | Design documents | Mixed | Not checked | Not for retrieval | Awaiting decision | Design material, not church policy |
| 3 | `src/main/java/.../MainActivity.java` identity strings (motto, slogan, vision, mission, values) | Undated | Developer-entered | Unknown | Institutional information (unverified) | Unknown | Yes (two mission statements) | None until decided | Awaiting leadership decision | See identity comparison |
| 4 | `src/main/assets/seed/sermons.json` (6 items, Bishop-attributed) | Seed data | Developer seed | Unknown | Ministry material | Unknown | None found | Possibly explanation of sermon topics, once rights confirmed | Awaiting decision; **excluded pending rights** | See content-rights register |
| 5 | `src/main/assets/seed/hymns.json` (12 hymns) | Seed data | Developer seed | Unknown | Worship material | Unknown | None found | Only if rights confirmed | Awaiting decision; **excluded pending rights** | See content-rights register |
| 6 | `src/main/assets/seed/reflections.json` (5 items) | Seed data | Developer seed | Unknown | Supplementary | Unknown | Not checked | Not official policy | Awaiting decision | Not official church teaching |
| 7 | `src/main/assets/seed/announcements.json` (5 items) | Seed data | Developer seed | Unknown | Operational | Possibly obsolete | Not checked | None until dates checked | Awaiting decision; **excluded: may be obsolete** | Dates to verify |
| 8 | `src/main/assets/seed/events.json` (6 items) | Seed data | Developer seed | Unknown | Operational | Possibly obsolete | Not checked | None until dates checked | Awaiting decision; **excluded: may be obsolete** | Dates to verify |
| 9 | `src/main/assets/seed/jokes.json`, `riddles.json` | Seed data | Developer seed | None | Entertainment | N/A | N/A | Not in knowledge package | Excluded | N/A |
| 10 | `src/main/assets/bible/kjv_verses.json`, `starter_verses.json`, `starter_chapters.json`, `kjv_manifest.json` | Manifest present; edition details to confirm | Unknown | Bible text | Biblical reference | Current | None | Scripture quotation and explanation | Awaiting decision on edition and source | See content-rights register |
| 11 | Remediation plan Bible decision D4 (NIV, ESV, NKJV) | 2026-11-18 | Plan | Owner | Decision record | Current | Owner prefers ESV for teaching | Pending licence | Awaiting decision | ESV licence needed |

## Summary
- Approved: 1 (instruction set only). Not approved as church information: all others.
- Excluded pending rights or currency: 4, 5, 7, 8.
- Leadership conflicts blocking identity sources: 3.
