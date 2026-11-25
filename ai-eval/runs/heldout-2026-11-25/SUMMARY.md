# Held-out doctrine set, 2026-11-25 (60 questions, never tuned on)

Set: ai-eval/heldout/cases-heldout-2026-11-25.jsonl (committed 6f810f8 before any run).
Change scored: own-knowledge prompt now carries up to six ranked KJV passages (OwnKnowledge.passagesFor).
Acceptance checks unchanged (real KJV references, verbatim KJV quotes, no official-position claims).

| Status group | Before | After |
|---|---|---|
| refused:no-verified-source | 23 | 22 |
| declined (no-go zones, scope, sensitive) | 15 | 15 |
| routed to approved/doctrine/verse answers | 18 | 18 |
| other-faiths description (identify-refute) | 2 | 2 |
| model answer (evidence-only or own knowledge) | 2 | 3 |
| answers citing 2+ KJV references | 20 | 21 |

Proxy only: these counts are not a correctness score. A human (Levi) still reviews each answer.

Reading the numbers: most held-out questions have no approved material in the library (doctrine/entries.json has 4
entries). Retrieval can only quote what is there. Material gaps and zone-scope issues are listed in the report.
