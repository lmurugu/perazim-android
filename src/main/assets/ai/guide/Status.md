# Status.md: Perazim Guide

- Guide version: 1.0.0 (2026-11-23). Operating files: Agents 1.0.0, Skills 1.2.0, Status 1.1.0.
- Model: Qwen2.5 0.5B Instruct, Q4_K_M, 491,400,032 bytes. Pinned in OnDeviceAi.java with its SHA-256.
- Knowledge package: read from the signed package at runtime (assets/ai/knowledge/package.json, signed with the owner's offline key).
- Test set: ai-eval/perazim-guide/cases.jsonl (532 cases) and ai-eval/perazim-guide/cases-stress-2026-11-23.jsonl (150 trick-question cases). Results are recorded by the laptop benchmark thread, not here.
- Drift detection (1.2.0): comparative knowledge in assets/ai/guide/comparative/. Draft, pending Levi's review.
- Feature flags: OFFLINE_QWEN_ENABLED is true only in debug and validation builds. Release stays off until the owner's gates pass.

KNOWN LIMITS
- Crisis keyword detection is in locked code. English, Swahili and Sheng phrases are matched (added 2026-11-23 after Levi approved them).
- The 0.5B model can drift in quotation wording. The self-check catches quotes that are not in the supplied evidence.
- Lessons change style and format only. They do not retrain the model and do not change rules.
- Memory is per device and per install. Nothing is uploaded. Clearing app data erases it.
- Online Gemini answers are not covered by this guide's self-learning.
- Verse and chapter checks use the standard chapter counts of the 66 books. Verse-number checks need the Bible database.
