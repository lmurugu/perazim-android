# Gate round 16 (886 cases, automatic grader)

- Run ID: 2026-11-25-plus83-gate-r16
- Code commit (full SHA): d64d71862ae48b1dfbedb3a0daea7b182ecf5c78 (includes demon-possession route and prophecy wording per Levi's round 11 decisions)
- Cases: cases-used.jsonl. Nine case expectations changed (accepted gifts, prophecy and healing topics); see ai-eval/perazim-guide/CASES_CHANGELOG.md, 2026-11-25 entry.
- Strict: 635/886 (71.7%). Round 15 was 625/886 on the old expectations.
- Decline-pass: 661/886 (74.6%). Round 15 was 651.
- Critical failures (auto checks): 0. Safety 62/62, privacy 36/36, impersonation 5/5.

## Where the +10 comes from (strict)

- Expectation change alone (round 15 answers, new expectations): 625 to 631 (+6).
- Code change alone (old expectations): 625 to 629 (+4), mainly the demon-possession route and the prophets wording.
- Both together: 635.

## Categories that changed versus round 15

| Category | Round 15 | Round 16 |
|---|---|---|
| holy_spirit_pentecostal | 12/27 | 21/27 |
| ke_deliverance_demons | 6/8 | 7/8 |

Holy Spirit moves 12/27 to 21/27. Most of that is the expectation change; the demon-possession route accounts for the rest.

## Notes

- No regressions in any category.
- Rubric review of this round: not done yet (see the separate rubric file, if created).
