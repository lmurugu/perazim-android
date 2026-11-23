# Perazim AI Guide: Governance Document Set

**Status:** DRAFT, pending approval by the named authorities in each document
**Version:** 0.1 (draft, 23 November 2026)
**Scope:** The ten governance documents (G01 to G10), the five doctrinal position papers (P1 to P5), the crisis directory, and the church action checklist.
**Prepared for:** Levi (product owner, safeguarding lead, and author of the governing decisions of 23 November 2026)

---

## 1. How this set fits the existing project

- `docs/PERAZIM_AI_PROJECT/00` to `20` and the `CODEX_*`, `DECISIONS_REGISTER.md`, `OPEN_DEPENDENCIES.md` files are **unchanged**. This set adds files only. Document 02 (`02_AI_IDENTITY_AND_BEHAVIOUR.md`) remains the approved voice and values reference.
- Numbering here uses the `G` prefix (governance) and `P` prefix (position papers) to avoid colliding with the existing 00 to 20 numbering.
- `AI_TEST_FRAMEWORK_2026-11-23.md` is Levi's test framework, committed unchanged. G09 uses it as its source.

## 2. Precedence rules applied in this draft

1. **Completed app wins.** Where a governance proposal contradicts what the Android app already does, the built behaviour is recorded and the proposal is marked *not adopted*. Nothing completed in the app is altered by this set.
2. **Enhancements are requirements.** Where a proposal adds something the app does not yet do, and does not contradict the built app, it is written as an adopted requirement to be implemented later. Those requirements are marked *to implement*.
3. **Standing decisions of Levi (23 November 2026) override the proposal text:**
   - Bible quotations: KJV and NASB only. No ESV in AI answers.
   - Non-religious questions stay declined by the scope gate.
   - Church identity: legal name "Perazim Mission Church" (Societies cert. No. 23319). Short form "PMCI" / "Perazim Missions Church International".
   - Data controller: Perazim Mission Church, P.O. Box 831-60100, Embu, Kenya.
   - Safeguarding lead: Levi.
4. **Nothing is invented.** Names, numbers and approvals not supplied by the church are marked *pending*.

## 3. Document index

| Ref | Document | Approval authority (pending) |
|---|---|---|
| G01 | Theological Constitution | Bishop Dr. David Mutweri, theological approver, subject to governance confirmation |
| G02 | Doctrinal Position Papers: index and status model | Bishop Dr. David Mutweri, theological approver |
| G03 | Pastoral Care and Safeguarding | Church leadership (authority to be confirmed) |
| G04 | Roles and Permissions | Church leadership and product owner |
| G05 | Privacy and Data Protection | Church data controller and an appropriate Kenyan privacy/legal reviewer |
| G06 | Official Church Information | Church leadership |
| G07 | Capability and Tool Permissions | Product owner and technical reviewer |
| G08 | Safety and Security | Technical reviewer, safeguarding reviewer and independent security/privacy reviewer |
| G09 | Evaluation and Regression Suite | All designated reviewers |
| G10 | Release and Change Management | Church leadership and product owner |
| P1 to P5 | Position papers (divorce and remarriage, tithing, alcohol, healing in the atonement, women in ministry) | Bishop Dr. David Mutweri, theological approver |
| CD | Crisis directory (data file and summary) | Church leadership, after independent verification |
| CHK | Church action checklist | Levi, for routing to the church |

## 4. Where each part of the 23 November proposal lives

| Proposal section | Governance location |
|---|---|
| Part II, theological positions and doctrine | G01, G02, P1 to P5 |
| Part II, sermon writing | G04 (permissions), G07 (capability) |
| Part III, crisis contacts | G03, CD |
| Part III, languages | G07 (English first) |
| Part III, pilot and cohort | G10 |
| Part III, under-18s | G04, G08 |
| Part III, reviewers and DPIA | G05, G09, CHK |
| Part III, release thresholds | G09 |
| Part IV, source hierarchy and doctrine status | G01 |
| Part IV, disagreement rules | G01 |
| Part IV, sensitive matters, medical/legal/financial, politics | G03 (sensitive matters and boundaries), G01 (political neutrality) |
| Part IV, privacy policy | G05 |
| Part IV, roles | G04 |
| Part V, general capability | G07 (current scope is the decline policy; see section 5) |
| Part VI, ten documents | G01 to G10 |

## 5. Conflict register (proposal versus built app)

| # | Proposal says | Built app does | Treatment |
|---|---|---|---|
| C1 | Part V: full general AI capability, including explaining compound interest, business plans and general medical or legal concepts. | `PerazimAiEngine` scope gate declines investment, medical/drug, legal, sports and campaign-speech requests with a fixed message. Levi confirmed on 23 Nov 2026 that non-religious questions stay declined. | **Not adopted.** Completed app and Levi's decision win. Recorded as future option in G07. |
| C2 | Part IV section 20: explain general medical, psychological and legal concepts; help with budgets. | Scope gate refuses medical, legal and investment advice outright. | **Not adopted** for now, same reason as C1. |
| C3 | Part IV section 21: AI may discuss politics and public policy. | Scope gate declines unrelated general-interest and campaign-speech requests. | **Not adopted.** Christian perspectives on justice and public responsibility are in scope (G01 section 11). |
| C4 | Standing decision: no ESV in AI answers. | Bible reader offers ESV, SUV, NIV and NKJV (`BibleReaderDialog`, `BibleDataSeeder`). | AI rule is KJV and NASB. The reader's translation list is **left as built** and flagged for Levi's decision (CHK item 9). |
| C5 | Part IV section 22: crisis messages may be sent automatically to pastors. | No automatic pastor notification exists. The relay returns general emergency guidance only. | **Not adopted** as automatic behaviour. Any escalation is a future option that needs a disclosed, authorised process (G03, G05). |
| C6 | Part IV section 22: members should not believe every chat is confidential counselling. | Not stated in the built app. | **To implement** as a disclosure requirement (G05). This is an addition, not a contradiction. |

## 6. Built-app baseline used for this draft

Checked on 23 November 2026 at commit `9111117` on `docs/ai-governance` (app code unchanged):

- `src/main/java/org/perazimchurch/app/ai/PerazimAiEngine.java`: scope gate patterns and out-of-scope response. Sensitive-topic terms are detected.
- `src/main/java/org/perazimchurch/app/ai/local/OnDeviceAi.java`: on-device model path, which applies the scope gate.
- `src/main/java/org/perazimchurch/app/ai/remote/ConversationHistory.java`: in-memory turn list with `clear()`.
- `src/main/java/org/perazimchurch/app/ai/remote/AiRelayClient.java` and `relay/worker.js`: online route through the relay to Gemini. The relay states it logs no question text. Crisis keyword check returns general emergency guidance with no numbers.
- `build.gradle`: `ONLINE_AI_ENABLED=false` and `OFFLINE_QWEN_ENABLED=false` in release. Qwen is on in debug and validation.
- No role, verified-leader, age-gate or pastoral-contact code was found. No NASB text was found in `src/main`.

## 7. Revision history

| Version | Date | Change | Author |
|---|---|---|---|
| 0.1 | 2026-11-23 | First draft of the full set | Claude (docs thread), for Levi |
