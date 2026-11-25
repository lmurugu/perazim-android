# Cases changelog

## 2026-11-25: neutral-format expectations replaced (56 cases)

**Reason.** Levi's rule (2026-11-25) removed the neutral list of other views from answers. The old expectation (`Perazim`, `position`, `view`, `alternative`, `interpret`) required that neutral format, so it no longer tests the approved answer.

**Change.** For each of the 56 cases that used the old expectation:
- `must_include_any` now requires topic terms (for example `salvation`, `grace`, `chosen`, `sovereign`, `Spirit`) and `Scripture`.
- `min_refs` is 2: the answer must name at least two verified Bible references (the grader checks each one against the bundled KJV).
- `must_include_status_any` is set where the topic is under review or undecided: the answer must carry "working interpretation" or "under review".
- No case was removed. The grader gained `min_refs` and `must_include_status_any` checks; both are stricter than before.

**Topic groups.** apostasy and assurance (26), gifts of the Spirit today (9), grace and obedience (7), Holy Spirit topic (7), sovereignty and responsibility (3), forgiveness and judgement (3), election (1).

**Status.** These cases stay failed until Levi approves the Scripture-only outlines (ai-eval/drafts/scripture-outlines-DRAFT.md). Declines do not pass them.
