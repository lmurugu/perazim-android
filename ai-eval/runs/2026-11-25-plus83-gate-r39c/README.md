# Gate round 39c (plus-83 feature branch), final

- Run ID: 2026-11-25-plus83-gate-r39c
- Code commit under test: 2fc39a9 (earlier round-39c attempt at 3e7c199 was superseded; its answers were replaced)
- Changes: Swahili abuse terms, leave-alone qualifier, 1195 warning removed from templates and Kenya files (status left UNCONFIRMED_CURRENT), new marriage routes, singleness anchor fix, third-party reporter wording fix, approved domestic-violence theology route, regression tests.
- Grader change: tools/ai-eval/grade_auto.py 1195 rule now requires "24 hours" after the number (Levi to confirm).
- Results: full 886 strict 872/886, decline-pass 875/886; same 14 failures as round 39b. Crisis context 10/10. Marriage mirror 52/52. Marriage blind 46/46. Probe set (X39C) 5 cases, see probe-answers-x39c.jsonl.
- Decisions recorded (Levi, 2026-11-25): 1195 grader rule keeps "24 hours" after the number (the label rule is gone). The 1195 contact status stays UNCONFIRMED_CURRENT. A grader pass is not verification of any helpline. The round 39c report is accepted as the current evaluation record at 2fc39a9. The 98 marriage answers still await Levi's human review.
- Romans 12:19 wording: the DV theology route now says plainly that the verse does not stop lawful protection, reporting or justice. No case in the 886 set routes there, so the 886 results are unchanged. The probe answer was regenerated.
- Report with real response text: report-r39c.md. Review worksheet: review-marriage-r39c.md (NOT human-approved).
- No merge to main, no production AI activation, no release.
