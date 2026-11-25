# Status.md: Perazim Guide

- Guide version: 1.0.0 (2026-11-23), operating files updated 2026-11-25. Operating files: Agents 1.1.0, Skills 1.3.0, Status 1.2.0.
- Model: Qwen2.5 0.5B Instruct, Q4_K_M, 491,400,032 bytes. Pinned in OnDeviceAi.java with its SHA-256.
- Knowledge package: read from the signed package at runtime (assets/ai/knowledge/package.json, signed with the owner's offline key).
- Test set: ai-eval/perazim-guide/cases.jsonl (532 cases) and ai-eval/perazim-guide/cases-stress-2026-11-23.jsonl (150 trick-question cases). Results are recorded by the laptop benchmark thread, not here.
- Drift detection: Bible references only (assets/ai/guide/drift/scripture-anchors.json, Levi-approved anchors). The comparative knowledge base describing other faiths was removed on 2026-11-25 on Levi's instruction.
- Sources: Bible text (KJV for quotations) and Bible-centred Protestant, Pentecostal and Evangelical material only. See docs/PERAZIM_AI_PROJECT/SOURCE_RESTRICTION_2026-11-25.md.
- Feature flags: OFFLINE_QWEN_ENABLED is true only in debug and validation builds. Release stays off until the owner's gates pass.

KNOWN LIMITS
- Crisis keyword detection is in locked code. English, Swahili and Sheng phrases are matched (added 2026-11-23 after Levi approved them).
- The 0.5B model can drift in quotation wording. The self-check catches quotes that are not in the supplied evidence.
- Lessons change style and format only. They do not retrain the model and do not change rules.
- Memory is per device and per install. Nothing is uploaded. Clearing app data erases it.
- Online Gemini answers are not covered by this guide's self-learning.
- Verse and chapter checks use the standard chapter counts of the 66 books. Verse-number checks need the Bible database.

RELEASE GATES (Levi, 2026-11-24)
- Safety and privacy: 100% pass, zero critical failures. Never a no-go zone.
- Doctrine, interpretation and verse search: 98% or more, with zero fabricated Scripture.
- Every other category: 90% or more, or an 80% overall average if 90% is not reachable.
- Automatic checks are not clearance. A partial pass is not clearance.

NO-GO ZONES (declared 2026-11-25 from the grounded-answer rubric review)
- 13 zones, listed in NoGoZones.java. Zones cover every grounded question in the review. Basis: AI rubric review, pending Levi's spot check (ai-eval/runs/2026-11-25-grounded).
- In a zone the guide gives one fixed, warm decline that names a trusted church leader, and the model never answers. Zones run after the safety, privacy, approved-doctrine and verbatim-verse routes.
- A zone is lifted only by a rerun that passes its category gate. Safety and privacy are never zoned.
- Open for Levi: the resource-lookup zone also blocks the verbatim Nicene Creed (rubric 4). Decide whether to keep it zoned or add an approved creed route.
