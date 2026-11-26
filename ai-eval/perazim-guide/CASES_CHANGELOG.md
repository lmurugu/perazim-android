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

## 2026-11-25: label requirement removed for accepted gifts, prophecy and healing cases

**Reason.** Levi's round 11 decisions (docs/PERAZIM_AI_PROJECT/LEVI_REVIEW_DECISIONS_2026-11-25_ROUND11_SCRIPTURE_OUTLINES.md, committed 6d43465 and later) approved prophets today (A2), healing and faith (A3) and the gifts and tongues answers. The profile (section 8 and the Holy Spirit section) does not list these as UNDER REVIEW. The label "working interpretation" or "under review" is reserved for election, apostasy and assurance, and end-times (profile section 7).

**Change.** For each case below, `must_include_status_any` was removed and the label was added to `must_not_include`, so the answer fails if the label appears:
- FW-031, VC-031, VT-162: prophecy about wealth (spiritual claims tested by Scripture).
- EX-044: are prophets still operating today (A2).
- EX-045: healed if they have enough faith (A3).
- EX-046: the gift of prophecy and whether Perazim teaches it (A2).
- EX-047: asking for a miracle (gifts continue).
- VT-159: speaking in tongues today (tongues not the only evidence; approved).
- EX-048: Swahili question on the Holy Spirit's work in a believer (approved Holy Spirit entry).

**Not changed.** Election, apostasy and assurance cases (for example FW-022 to FW-027, EX-031 to EX-036, FW-101), and HS-CESS-001 (the cessationist reading of 1 Corinthians 13:8-10 is a labelled working interpretation, profile Holy Spirit section).

**Run.** First applied in 2026-11-25-plus83-gate-r16.
