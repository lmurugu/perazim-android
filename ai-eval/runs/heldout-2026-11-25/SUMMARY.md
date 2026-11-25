# Held-out doctrine set, 2026-11-25 (60 questions, sealed, never tuned on)

Set: ai-eval/heldout/cases-heldout-2026-11-25.jsonl (committed 6f810f8 before any run).
Files: before.jsonl (baseline), after.jsonl (retrieval passages only), after-final.jsonl (all changes).

| Status group | Before | Retrieval only | Final |
|---|---|---|---|
| refused:no-verified-source | 23 | 22 | 26 |
| declined (no-go zones, scope, sensitive) | 15 | 15 | 10 |
| routed to approved/doctrine/verse answers | 18 | 18 | 18 |
| other-faiths description (identify-refute) | 2 | 2 | 0 |
| model answer (evidence-only or own knowledge) | 2 | 3 | 6 |
| answers citing 2+ KJV references | 20 | 21 | 22 |

Changes in the final run:
- Retrieval passages from the KJV for the own-knowledge prompt (OwnKnowledge.passagesFor).
- 12 DRAFT library entries (doctrine/drafts.json) used as retrieval passages only, never shown as approved.
- Christian-doctrine questions with no other group named skip the other-faiths and history zones and identify-refute.
- Christian-doctrine questions get no dictionary or name evidence (the Hitchcock "Mary: same as Miriam" answer).
- Eval harness evidence (PreSigningEvidence) matches production for these questions.

Honest reading: the final run declines fewer questions, but refuses more with the "no verified source" wording. Refusals and
declines are not improvements by themselves. Answers are not scored for correctness; Levi reviews each model answer.
Model answers still include a wrong or weak one (HO-004 on Jesus' brothers, HO-034 on hell). Those need review before release.
