# Perazim AI Evaluation — Round 10

**Reported:** 25 November 2026  
**Branch:** `claude/ai-guide-final-plus-83`  
**Code commit tested:** `b776bed` (short SHA supplied by coordinator; full SHA and external run identifier were not included in the report).  
**Cases:** 886, using `cases-used.jsonl` with HS-CESS-001 updated.  
**Grader:** automatic `tools/ai-eval/grade_auto.py`. Human rubric review was not completed at the time of the report.

## Results

- Strict: **590/886 (66.6%)**
- Decline-pass: **616/886 (69.5%)**
- Critical failures: **0 strict; 0 decline-pass**
- Safety/safeguarding: **62/62**
- Privacy/confidentiality: **36/36**
- Impersonation: **5/5**
- Regression comparison: coordinator reports 0 regressions and 19 gains compared with round 9 (commit `d01309b`, 571/886).
- Earlier round 7: 515/886 at commit `1d00b67`.

## Category results

| Category | Strict | Decline-pass |
|---|---:|---:|
| biblical_accuracy_hallucination | 23/26 | 23/26 |
| catholic_orthodox | 4/10 | 6/10 |
| church_authority_boundaries | 13/21 | 13/21 |
| contradiction_consistency | 10/17 | 10/17 |
| cult_group | 5/8 | 5/8 |
| denominational_fairness | 5/16 | 5/16 |
| drift | 9/21 | 9/21 |
| general_intelligence_scope | 46/61 | 51/61 |
| governance | 2/6 | 2/6 |
| historical_apologetics | 9/24 | 9/24 |
| holy_spirit_pentecostal | 12/27 | 12/27 |
| human_decision | 0/2 | 2/2 |
| impersonation | 5/5 | 5/5 |
| israel_church_eschatology | 22/24 | 22/24 |
| ke_alcohol_drugs | 6/8 | 6/8 |
| ke_church_governance_abuse | 5/10 | 5/10 |
| ke_culture_rites | 9/13 | 9/13 |
| ke_deliverance_demons | 6/8 | 6/8 |
| ke_economics_debt_betting | 9/12 | 11/12 |
| ke_elections_tribe | 1/6 | 2/6 |
| ke_false_prophets_miracles | 3/10 | 3/10 |
| ke_health_hiv_medicine | 7/10 | 7/10 |
| ke_islam_coast | 5/8 | 5/8 |
| ke_language_tone | 4/12 | 4/12 |
| ke_marriage_family | 9/12 | 9/12 |
| ke_mental_health_stigma | 8/10 | 8/10 |
| ke_prosperity_seed_tithing | 6/14 | 6/14 |
| ke_sabbath_sda | 6/8 | 6/8 |
| ke_witchcraft_curses | 11/14 | 11/14 |
| language_pastoral_tone | 15/27 | 15/27 |
| memory_continuity_privacy | 5/5 | 5/5 |
| multi_turn | 3/4 | 3/4 |
| pastoral | 6/7 | 6/7 |
| pastoral_struggle | 34/45 | 34/45 |
| privacy | 3/3 | 3/3 |
| privacy_confidentiality | 36/36 | 36/36 |
| proof_text | 8/18 | 8/18 |
| reasoning_critical_analysis | 0/18 | 0/18 |
| research_source_verification | 12/18 | 12/18 |
| resource_use | 15/31 | 16/31 |
| role_boundaries_refusal | 10/24 | 19/24 |
| safety_crisis_safeguarding | 62/62 | 62/62 |
| scope | 10/13 | 10/13 |
| scripture_exegesis | 27/42 | 27/42 |
| soteriology_apostasy | 23/32 | 23/32 |
| spiritual_manipulation | 23/24 | 23/24 |
| tool_use_execution | 22/32 | 26/32 |
| trick_fake_verse | 5/5 | 5/5 |
| trick_misquote | 5/5 | 5/5 |
| trick_premise | 1/6 | 1/6 |
| variant | 5/6 | 5/6 |

## Interpretation and remaining failures

The reported result is a significant improvement, but it is **not a passed overall gate**. Automatic grading only was used; human rubric review remains outstanding.

High-priority areas still include:

- Reasoning/critical analysis: 0/18.
- Historical apologetics: 9/24.
- Holy Spirit/Pentecostal: 12/27.
- Catholic/Orthodox: 4/10.
- Proof-text handling: 8/18.
- Scripture exegesis: 27/42.
- Research/source verification: 12/18.
- Language/pastoral tone: 15/27.
- Kenyan language/tone: 4/12.
- Salvation/apostasy: 23/32.

The 886-case report also says that the Israel/end-times answer is active in the AI branch, the gifts-continue answer is approved, the cessationism route is limited to direct questions, the apostasy/election/grace/sovereignty outlines are implemented, and identify-and-refute is enabled for four covered topics. These are coordinator-reported implementation states; the evaluation report alone does not establish that every route has passed human review.

## Required next report

The next benchmark report must include:

1. Exact full code commit SHA, not only a short SHA.
2. Unique run identifier or durable report location.
3. Case file hash and exact case count.
4. Strict and decline-pass totals and category results.
5. Critical failures, skipped cases and regressions.
6. Human rubric-review results in addition to automatic grading.
7. A clear statement whether each changed response is correct, not merely whether it matches the automated grader.

## Release boundary

Safety and privacy passing is necessary but not sufficient. The overall gate remains unmet. No merge into main, production release, production model activation or transfer of unapproved material is authorised.
