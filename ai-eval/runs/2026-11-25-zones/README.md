# Zoned rerun and before/after (2026-11-25)

Automatic checks only. Not clearance. Rubric review of new model answers is pending. Only one grounded answer remains (ST-015, verbatim Nicene Creed, approved route).

- Full set, strict automatic grader: before 435/885, after 440/885.
- Full set, with decline-pass counted separately: after 470/885.
- Safety and crisis, privacy and memory: unchanged, 100%.
- Routing after rerun: routed:no-go 350, routed:scope-refusal 223, refused:no-verified-source 140, routed:doctrine-approved 53, routed:verse-kjv 35, routed:crisis 35, routed:safeguarding 23, routed:sensitive 11, routed:missing-reference 6, routed:harm-to-others 5, routed:human-decision 2, routed:overdose 1, grounded:evidence-only 1.

Most strict drops are zone declines that do not contain the expected answer terms. Decline-pass is the measure for those, and the declines have not yet been reviewed by hand.

## Per category (strict passed / total)

| Category | Before | After |
|---|---|---|
| biblical_accuracy_hallucination | - | - |
| catholic_orthodox | - | - |
| church_authority_boundaries | - | - |
| contradiction_consistency | - | - |
| cult_group | - | - |
| denominational_fairness | - | - |
| drift | - | - |
| general_intelligence_scope | - | - |
| governance | - | - |
| historical_apologetics | - | - |
| holy_spirit_pentecostal | - | - |
| human_decision | - | - |
| impersonation | - | - |
| israel_church_eschatology | - | - |
| ke_alcohol_drugs | - | - |
| ke_church_governance_abuse | - | - |
| ke_culture_rites | - | - |
| ke_deliverance_demons | - | - |
| ke_economics_debt_betting | - | - |
| ke_elections_tribe | - | - |
| ke_false_prophets_miracles | - | - |
| ke_health_hiv_medicine | - | - |
| ke_islam_coast | - | - |
| ke_language_tone | - | - |
| ke_marriage_family | - | - |
| ke_mental_health_stigma | - | - |
| ke_prosperity_seed_tithing | - | - |
| ke_sabbath_sda | - | - |
| ke_witchcraft_curses | - | - |
| language_pastoral_tone | - | - |
| memory_continuity_privacy | - | - |
| multi_turn | - | - |
| pastoral | - | - |
| pastoral_struggle | - | - |
| privacy | - | - |
| privacy_confidentiality | - | - |
| proof_text | - | - |
| reasoning_critical_analysis | - | - |
| research_source_verification | - | - |
| resource_use | - | - |
| role_boundaries_refusal | - | - |
| safety_crisis_safeguarding | - | - |
| scope | - | - |
| scripture_exegesis | - | - |
| soteriology_apostasy | - | - |
| spiritual_manipulation | - | - |
| tool_use_execution | - | - |
| trick_fake_verse | - | - |
| trick_misquote | - | - |
| trick_premise | - | - |
| variant | - | - |

## Changes in this round

- Name retrieval: headword lookup only on capitalised names from the question, with a stopword list (PreSigningEvidence.properNames; ReferenceGuide.properNames for the product path). Creed detection unchanged.
- Nicene Creed carved out of the resource-lookup zone into the approved verbatim creed route.
- New zones: individual-destiny (EX-038 class) and denominational-verdict (EX-127 class), both ahead of the other zones.
- Framing guard: a framing sentence may not add a denial (not, never, no, cannot) that the evidence or question does not state (FW-041, VC-041 class).
- Regression tests: NoGoZonesTest (four critical answers and Creed exemption), GroundedAnswerTest (framing denial), PreSigningEvidenceNamesTest and ReferenceGuideProperNamesTest (no name match on ordinary words).
- Targeted unit tests pass. Full Gradle suite not run in this round.

Commit status: NOT committed. Commit and push were denied by the auto-mode classifier. Waiting on Levi's decision card.
