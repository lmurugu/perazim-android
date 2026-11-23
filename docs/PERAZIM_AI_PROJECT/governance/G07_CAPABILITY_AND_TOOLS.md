# G07 AI Capability and Tool Permissions Specification

**Version:** 0.1 (draft)
**Status:** DRAFT, pending approval by the product owner (Levi) and a technical reviewer
**Approved by:** not yet approved
**Approval date:** pending
**Effective date:** pending

---

## 1. Principle

A capability may be advertised to users **only** when it is wired into the application, enabled, tested, and available to that user. A system prompt cannot enable web browsing, file creation, spreadsheets or any integration the application does not provide.

## 2. Current scope (decided)

- The Guide answers Christian faith questions, approved Perazim information, and questions about using the app.
- Non-religious questions stay **declined** by the scope gate, as Levi decided on 23 November 2026.
- The built scope gate (`PerazimAiEngine`) declines investment, medical/drug, legal, sports and campaign-speech requests with a fixed response. That behaviour is kept.
- The refusal is polite and redirects to what the Guide can do or to a human leader.

## 3. Proposed general capability: not adopted for now

The proposal's Part V asked for wide general capability (research, data analysis, file generation, writing for the user, and so on). Under the completed-app rule and Levi's decision, this is **not adopted** at this time. It is recorded as a future option that would need a formal scope change, a capability acceptance test, and approval under G10. README conflicts C1 to C3 apply.

## 4. Capability register

| Capability | Status in built app | Requirement |
|---|---|---|
| Scripture quotation (KJV, NASB) | KJV in use. NASB not found in `src/main` | Quote only from an approved, integrity-checked corpus, with attribution. NASB to implement |
| Scripture quotation (ESV) | **Not permitted for AI answers** (standing decision) | Do not quote. Reader translation list flagged (README C4) |
| Church information lookup | Governed knowledge design | Per G06 only |
| Sermon drafting | Not built | Verified leaders only (G04). Requirement to implement |
| Study outlines, devotionals | Built as AI features (scope) | Permitted to members |
| Pastoral referral | Not built (no contact yet) | Pending church contact (G03) |
| Web research, file creation, spreadsheets, code help | Not built | Not adopted (section 3) |
| Images | Not built | Not adopted |
| Languages | English | English only. Swahili after dedicated theological and safety testing. Sheng not adopted |

## 5. Scripture rules

- Quotations include the translation name.
- The Guide never labels a translation it did not use. KJV must not be presented as ESV, and no text may be attributed to ESV.
- The Guide never invents verse wording, invents a citation, or attributes a fabricated statement to a theologian.
- For a topical question with no explicit reference, the Guide explains the passages it is confident about and says where it has not checked the text.

## 6. Sermon assistance

- Members: study outlines, devotional material, discussion questions, explanations, personal study guides, discipleship lessons, sermon-study notes.
- Verified leaders: full sermon drafts, outlines, illustrations, exegetical notes and teaching manuscripts.
- Any AI-generated sermon is not official teaching until a human leader approves it.
- Sources used are attributed. The Guide never invents quotations.

## 7. Requirements to implement

1. Capability gating from the permission model (G04).
2. A capability acceptance test before any capability is advertised (G09).
3. Swahili not offered as a supported language until testing (G10).

## 8. Revision history

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-11-23 | First draft, reflecting the decline policy |
