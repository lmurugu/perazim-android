# Perazim AI Guide: Limitations and Metrics

Status: draft, 2026-11-23. Only items marked **measured** have results. Everything else is **not measured** and must not be reported as a score.

## Summary

The guide answers from a small offline model (Qwen2.5 0.5B, Q4_K_M, about 491 MB) plus bundled, approved text. It is weakest where the model must reason or recall beyond the retrieved text. Its safety, verse-quoting and clarification rules are deterministic code, so they do not depend on the model.

## Limitations by task (what it cannot do or does unreliably)

| Task | Current behaviour (from code) | Status | Path to 95%+ |
|---|---|---|---|
| Single verse by reference ("John 3:16") | KJV text quoted directly from bundled Bible, no model generation | Code path added (4d19a57); not run on phone | Already deterministic |
| Verse ranges and multiple references ("1 Corinthians 13:4-7") | Still goes to the model; a range can be paraphrased or wrongly joined | Not built | Quote each verse from KJV, model adds explanation only |
| Misspelt book names ("Jhon 3 16") | Corrected for close matches; asks one question when unsure | Parser built (608765a); not run on phone | Deterministic, add tests for each book |
| Swahili book names | Not supported in the parser | Not built | Add a Swahili name table |
| Topical verse search without a reference ("verses about patience") | Keyword retrieval over the bundled Bible, capped at 900 characters total | Not measured | Topical index, deterministic |
| Cross-references | No cross-reference data is wired into the guide | Not measured | Decline honestly until data is bundled |
| Word studies, Greek and Hebrew | Strong's and interlinear stores exist in the app's data layer but the AI pipeline does not call them | Not measured | Deterministic lookup from the existing stores; the model should not guess meanings |
| Comparing translations | Only KJV is bundled; NASB text is not in the assets | Not measured | Decline comparisons until licensed text is bundled |
| NASB quotes | Not bundled | Not available | Needs licensed text and attribution |
| Multi-step doctrinal reasoning | 0.5B model; baseline answers were evasive or misleading | Measured on 119-question baseline (qualitative only) | Clarify and decline rather than guess; better model is unlikely at this size |
| Long passages | Excerpts capped at 700 characters each, three excerpts, 5000-character prompt | Limit is in code, not measured | Quote in chunks |
| Very long conversations | Only the last four turns, 650 characters each, reach the model | Limit is in code, not measured | State the limit; do not summarise from memory |
| Fabricated verses | Baseline invented Ezekiel 50:4 and a Matthew 17:25 quote | Single-verse case fixed in code; others not measured | Zero tolerance: quote from KJV or decline |
| Crisis and safety | Fixed crisis text with verified Kenyan contacts, shown before any model output | Text changed (6088ae1); not run on phone | Fixed text, not model text |

## Metrics to report (each graded separately)

For every metric below, record accuracy %, fabrication rate, clarify rate and latency on the phone-sized model. None is measured yet.

- Single-verse accuracy
- Range and multi-reference accuracy
- Misspelling and ambiguity handling (clarify when unclear, answer when clear)
- Topical search precision
- Cross-reference coverage
- Word-study accuracy
- Translation comparison (decline or correct)
- Multi-step doctrinal reasoning
- Long-passage handling
- Long-conversation memory
- Swahili handling
- Crisis and safety (must stay at 100%)

## Release gates (from governance answers)

- Doctrine, Scripture interpretation and verse search: 98%+ with zero fabricated or misquoted Scripture.
- All other areas: 90%+.
- Safety and privacy: 100%, with zero critical failures.
- Tone: natural and warm, with its own 90% gate once the grader exists.
- Overall target for the report: 95%+.

## Honest declines (to be added)

Where a metric cannot reach 95% with the current data or model, the guide should decline or redirect instead of guessing. Candidates: cross-references, translation comparison, word studies until the Strong's lookup is wired, and multi-step doctrinal reasoning without retrieved text.
