# Knowledge package approval dossier (2026-11-20)

**Status: NOT APPROVED. This dossier is for owner review only. Nothing here is official church policy until the owner approves it.**

## 1. Proposed sources and classification

| # | Source (repo location) | Category | Authority | Status / notes |
|---|---|---|---|---|
| 1 | `src/main/assets/seed/sermons.json` | 2. Ministry material (sermons) | Unknown until owner confirms | Synthetic/seed content in app; provenance not recorded. Unapproved. |
| 2 | `src/main/assets/seed/announcements.json` | 5. Unapproved, may be obsolete | None | Dates and content need owner review. |
| 3 | `src/main/assets/seed/events.json` | 5. Unapproved, may be obsolete | None | Event dates need review. |
| 4 | `src/main/assets/seed/reflections.json` | 4. Supplementary | None | Not church policy. |
| 5 | `src/main/assets/seed/hymns.json` | 2. Ministry material | Hymn rights not recorded | Copyright status unknown. Needs owner confirmation. |
| 6 | `src/main/assets/seed/jokes.json`, `riddles.json` | Not knowledge (entertainment) | None | Excluded from the assistant's knowledge package. |
| 7 | `docs/PERAZIM_AI_PROJECT/*` (AI specs 00–20) | Standing instructions and design, not church information | Owner-authored specs | Doc 02 approved as instructions (separate from knowledge). Others are design docs. |
| 8 | `docs/internal/remediation-plan.md`, `docs/SETUP_FOR_LEVI.md` | Project records | Internal | Not for the assistant. |
| 9 | Bible texts (KJV, Swahili Union Version, NIV, ESV, NKJV; `src/main/assets/bible`) | 3. Biblical reference | Text licensing | Owner decision D4: plan recommends removing NIV, ESV and NKJV; Swahili Union Version rights to confirm. |
| 10 | Constitution, official mission/vision statement, motto, official contacts | 1. Official church documents | Owner | **Not located in the repository.** Must be supplied by the owner. |
| 11 | Membership procedures, leadership structure | 1 or 2 | Owner | **Not located in the repository.** |

## 2. Conflicting or unresolved institutional information (do not resolve silently)

- **Church name:** The repository uses "Perazim Missions Church International" (23 files), "Perazim Mission Church" (23 files), "PMCI" (13 files) and "Perazim Church" (7 files). The remediation plan lists the church name as an owner decision. **Leadership decision required.**
- **Motto, mission, vision and values:** Sources disagree or are missing. Document 04 lists motto and mission/vision under Collection A, but no approved version is present. **Leadership decision required.** Until resolved, the assistant must not state a motto, mission, vision or values as official.
- **Minimum age, Bible text licences and legal documents:** Decided or pending as recorded in the remediation plan (18+ decided; Bible licences pending; legal approval pending). Not part of the assistant's knowledge until approved.

## 3. Missing material
- Constitution and any approved institutional statements.
- Official contacts and leadership roles (the assistant must not invent contacts).
- Approved doctrinal statement (if one exists). Without it, the assistant says no official position is established.
- Provenance and rights for the sermon, hymn and reflection content.

## 4. Decisions reserved for the owner
1. Approve or reject each source in section 1.
2. Resolve the church name and motto/mission/vision conflicts.
3. Supply the Constitution and official statements, or confirm none exist.
4. Confirm rights for hymns, Bible translations and sermons.
5. Approve the knowledge package as a whole.

## 5. Recommendation
Approve nothing yet. Start with sources 7 and 9 only if you want the assistant to answer app and Bible questions. Keep sources 1–5 out until provenance is confirmed.
