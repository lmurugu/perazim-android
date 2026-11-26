# Gate round 22 (886 cases, automatic grader)

- Run ID: 2026-11-25-plus83-gate-r22
- Code commit (full SHA): 3dbe7b7223a170fa0f40eb88d5ed2f04042bef40
- Cases: cases-used.jsonl (same 886 cases as rounds 16 to 21).
- Strict: 719/886 (81.2%). Unchanged from round 21.
- Decline-pass: 740/886 (83.5%). Unchanged from round 21.
- Critical failures (auto checks): 0. Safety 62/62, privacy 36/36, privacy_confidentiality 36/36, impersonation 5/5.
- No category went down.

## Change in this round: Scripture quoted verbatim (coordinator, 2026-11-25)

- Qur'an answers stay paraphrase-only (rule O-1).
- The proof-text and accuracy routes now quote Scripture verbatim from the bundled KJV (assets/bible/text/KJV.tsv.gz) with its reference, and each route anchors at least two verses. Routes changed: first-gospel-preacher, god-not-a-man, archangel-michael-jesus, jesus-is-the-father, trinity-not-pagan, sunday-mark-beast, sabbath-to-sunday, quran-paraphrase, arabic-text-request, muhammad-prophet-claim, matthew-7-21-lord-lord, pharaoh-heart, sickness-not-punishment, governance-accountability (2 Corinthians 8:21 corrected to "but also").
- Check used: each inline quotation in those bodies appears verbatim in the KJV file (case-sensitive). The check found no mismatch in the changed routes.

## Still open

- An audit of every route found older routes that still summarise Scripture in their bodies, with references (for example sovereignty-and-responsibility, cessationism, pharaoh's earlier wording in other routes, payment-for-blessing, remove-pastor, leadership-appointment, explain-again, dream-warning, and the agape route). Each has two verbatim anchors, so the Scripture is quoted, but the body summaries are not yet verbatim. Flagged for a later pass.
- Sub-90% categories remain: role boundaries, church authority, drift, contradictions, historical apologetics, research sources, resource use and tool use.

## Category breakdown (strict, round 21 to round 22)

| Category | Round 21 | Round 22 |
|---|---|---|
| biblical_accuracy_hallucination | 26/26 | 26/26 |
| catholic_orthodox | 10/10 | 10/10 |
| church_authority_boundaries | 13/21 | 13/21 |
| contradiction_consistency | 10/17 | 10/17 |
| cult_group | 5/8 | 5/8 |
| denominational_fairness | 12/16 | 12/16 |
| drift | 13/21 | 13/21 |
| general_intelligence_scope | 46/61 | 46/61 |
| governance | 2/6 | 2/6 |
| historical_apologetics | 14/24 | 14/24 |
| holy_spirit_pentecostal | 26/27 | 26/27 |
| human_decision | 2/2 | 2/2 |
| impersonation | 5/5 | 5/5 |
| israel_church_eschatology | 22/24 | 22/24 |
| ke_alcohol_drugs | 7/8 | 7/8 |
| ke_church_governance_abuse | 10/10 | 10/10 |
| ke_culture_rites | 9/13 | 9/13 |
| ke_deliverance_demons | 7/8 | 7/8 |
| ke_economics_debt_betting | 9/12 | 9/12 |
| ke_elections_tribe | 5/6 | 5/6 |
| ke_false_prophets_miracles | 10/10 | 10/10 |
| ke_health_hiv_medicine | 8/10 | 8/10 |
| ke_islam_coast | 7/8 | 7/8 |
| ke_language_tone | 12/12 | 12/12 |
| ke_marriage_family | 9/12 | 9/12 |
| ke_mental_health_stigma | 8/10 | 8/10 |
| ke_prosperity_seed_tithing | 14/14 | 14/14 |
| ke_sabbath_sda | 6/8 | 6/8 |
| ke_witchcraft_curses | 13/14 | 13/14 |
| language_pastoral_tone | 15/27 | 15/27 |
| memory_continuity_privacy | 5/5 | 5/5 |
| multi_turn | 3/4 | 3/4 |
| pastoral | 6/7 | 6/7 |
| pastoral_struggle | 34/45 | 34/45 |
| privacy | 3/3 | 3/3 |
| privacy_confidentiality | 36/36 | 36/36 |
| proof_text | 18/18 | 18/18 |
| reasoning_critical_analysis | 18/18 | 18/18 |
| research_source_verification | 12/18 | 12/18 |
| resource_use | 15/31 | 15/31 |
| role_boundaries_refusal | 10/24 | 10/24 |
| safety_crisis_safeguarding | 62/62 | 62/62 |
| scope | 13/13 | 13/13 |
| scripture_exegesis | 31/42 | 31/42 |
| soteriology_apostasy | 32/32 | 32/32 |
| spiritual_manipulation | 23/24 | 23/24 |
| tool_use_execution | 22/32 | 22/32 |
| trick_fake_verse | 5/5 | 5/5 |
| trick_misquote | 5/5 | 5/5 |
| trick_premise | 5/6 | 5/6 |
| variant | 6/6 | 6/6 |
