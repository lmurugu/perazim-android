# Church identity comparison (2026-11-20)

**Status: for leadership review. No statement below is approved except where marked. Nothing has been merged or selected.**

## A. Name

Owner decision recorded 2026-11-20: the AI-facing name is **Perazim Missions Church International (PMCI)**. The abbreviation PMCI is approved for AI use. Legal registration name is not established by any document in the repository.

| Wording | Where it appears (repo, approximate count) | Classification |
|---|---|---|
| Perazim Missions Church International | 23 files (docs and code) | Owner-approved AI-facing name |
| PMCI | 13 files | Owner-approved abbreviation |
| Perazim Mission Church | 23 files; app UI and share texts, e.g. `MainActivity.java` (pastor attribution, share text, lines ~1480–1534, ~2731–2747) | Historical or local alternative wording; usage to be documented. Not changed by this register |
| Perazim Church | 7 files | Informal or abbreviated reference; source classification needed |

**Discrepancy for the owner:** the app's own user-facing text uses "Perazim Mission Church". The approved AI name differs. Changing app UI text is not part of this task and needs an owner decision.

## B. Motto, slogan, vision, mission and values (hard-coded in the app; no approval record found)

| Element | Wording found | Location(s) | Notes |
|---|---|---|---|
| Motto | "The Place of Great Breakthrough" | `MainActivity.java` 2105, 2540, 3288; `ProfileUiBinder.java` 1402; `HomeUiBinder.java` 329; `OnboardingDialog.java` 119; `SheetSyncValidationTest.java` 158 (sheet key `church_motto`) | Consistent across the app. Repetition is not approval. Source of the wording and approval date not recorded |
| Slogan | "A place where everybody is somebody, and no body is a nobody" | `MainActivity.java` 834, 2117 (approx.) | Only one location found |
| Vision | "To be a Center for Missions" | `MainActivity.java` 2129 | Appears in one location. A separate vision reference exists in `OnboardingDialog.java` 32 (comment only) |
| Mission (version 1) | "Perazim exists to draw people to Christ, to disciple them to belong to His family, and to have them glorify GOD with their lives and their services." | `MainActivity.java` ~2137 | Full mission-style statement |
| Mission (version 2) | "Spreading the Gospel of Christ across Kenya & Beyond" | `ProfileUiBinder.java` 867 (profile dialog) | **Conflicts with version 1** (different wording and scope) |
| Core values | "Evangelism • Discipleship • Fellowship • Worship • Ministry" | `MainActivity.java` ~2153 | One location |
| Leadership line | Bishop name and "Embu Headquarters, Kenya" | `ProfileUiBinder.java` 864–866 | Leadership and location to be verified by the owner (see missing-documents register) |

## C. Contradictions and open questions
1. Two different mission statements exist in the app (versions 1 and 2). Neither is approved.
2. The slogan, vision and core values each appear in only one place. Their source and date are unknown.
3. The motto appears in several places, but no record shows who approved it.
4. Headquarters location and church leadership are hard-coded; no approved record was found.

## D. Decisions reserved for leadership
- Choose one motto, one mission, one vision, one slogan and one set of core values, or confirm that none is official yet.
- Decide whether "Perazim Mission Church" in app UI should change to the approved AI-facing name.

## E. Interim rule for the assistant
Until leadership decides, the assistant must not state a motto, mission, vision, slogan or core values as official Perazim positions. It should say the official statement has not been established in its approved sources.
