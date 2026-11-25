# Gate round 23 (886 cases, automatic grader)

- Run ID: 2026-11-25-plus83-gate-r23
- Code commit (full SHA): bcd9d36b3690b6d96abd6a7fe0207afd2d22b2be
- Cases: cases-used.jsonl (same 886 cases as rounds 16 to 22, graded against the r16 file).
- Strict: 774/886 (87.4%), up from 719 in round 22.
- Decline-pass: 793/886 (89.5%), up from 740 in round 22.
- Critical failures (auto checks): 0. Safety 62/62, privacy 36/36, privacy_confidentiality 36/36, impersonation 5/5.
- Regressions against round 22: 0 (checked case by case). Three regressions found in the first pass were fixed before this record.

## Quote-mark confirmation (coordinator, 2026-11-25)

- No summary uses quote marks, or "the Bible says '...'", without verbatim text.
- An audit of every route body flagged 23 quote-mark spans not found verbatim in the bundled KJV. Nearly all are apostrophe artefacts or attributed non-Scripture quotes (Ignatius, Justin, Augustine NPNF, Strong's glosses, the cleanliness saying). No unverified Scripture quotation was found.

## What changed in this round

- No-go zones added (fixed declines, each naming a trusted church leader): human-decision-authority (Bishop or elder, church office), out-of-scope-task (scope wording), uncertain-live-research (cannot browse, search or verify).
- Routes added with KJV anchors (verified by reference check, all 199 anchors exist): grace-free-not-earned, baptism-saves-premise, spirit-and-sin, all-paths-one-way, created-not-god, one-mediator-prayer, ancestors-and-offerings, sabbath-not-condition, soul-sleep-and-presence, no-god-and-moral-law, someone-else-on-cross, nicaea-and-the-bible, how-books-recognised, trinity-before-nicaea, christianity-defended-rationally, new-testament-reliability, creed-resurrection, easton-bethlehem, easton-ebenezer, smith-nazareth, hitchcock-jeremiah, henry-genesis-one, wesley-notes, strongs-g26-agape, strongs-h7225-reshith, john-1-1-cross-references, psalm-23-niv-request, psalm-91-context, psalm-23-verse-count, bible-note-lookup, routine-status-today.
- Fixed: the creation route anchor was "Psalm 19:1"; the KJV book name is "Psalms", so the route never answered. Now "Psalms 19:1".
- Fixed: sovereignty route now matches "sovereign" (not only "sovereignty").
- Fixed: the Spirit route keeps the approved Spirit baptism wording (distinct from conversion; tongues not the only evidence) and excludes baptism and tongue questions.

## Category breakdown (strict, round 22 to round 23)

| Category | Round 22 | Round 23 |
|---|---|---|
| church_authority_boundaries | 13/21 | 21/21 |
| drift | 13/21 | 21/21 |
| role_boundaries_refusal | 19/24 | 24/24 |
| historical_apologetics | 14/24 | 23/24 |
| resource_use | 16/31 | 27/31 |
| tool_use_execution | 26/32 | 31/32 |
| research_source_verification | 12/18 | 13/18 |
| contradiction_consistency | 10/17 | 16/17 |
| biblical_accuracy_hallucination | 26/26 | 26/26 |
| holy_spirit_pentecostal | 26/27 | 26/27 |
| governance | 2/6 | 2/6 |

Unchanged in round 23: all other categories (see the decline-pass output for the full list).

## Still open (below the 90% gate)

- language_pastoral_tone 15/27, pastoral_struggle 34/45, scripture_exegesis 31/42, general_intelligence_scope 51/61, governance 2/6, cult_group 5/8, denominational_fairness 12/16, ke_culture_rites 9/13, ke_marriage_family 9/12, ke_sabbath_sda 6/8, ke_health_hiv_medicine 8/10, ke_mental_health_stigma 8/10, research_source_verification 13/18, resource_use 27/31.
- Release gates are not met. No merge or release without Levi's explicit approval.
- Rubric re-sample not done for any round since round 15.

## Follow-up (2026-11-25): Matthew Henry

- Code commit: 74b11d987fc6d5b9222f371858e4ca8156459b85
- Decline-pass 793/886 and strict 774/886, the same as the round 23 record, with zero regressions and zero critical failures.
- Henry's commentary (all six CCEL volumes, public domain, approved) is already in assets/reference/reference-commentaries.db.gz (16.4 MB, bundled before this round). No new asset was added, so the app size change is 0 bytes.
- The Henry answer path (ReferenceGuide.namedHenry) needs an Android database, so the host evaluation cannot show it. ST-013 uses the fail-closed fallback here. The on-device answer still needs checking on the phone.
- Wesley's notes are in the same database but are keyed by part ("Notes on ... part N"), not by chapter. A passage answer needs a part-to-chapter map first, so Wesley still declines by passage.
