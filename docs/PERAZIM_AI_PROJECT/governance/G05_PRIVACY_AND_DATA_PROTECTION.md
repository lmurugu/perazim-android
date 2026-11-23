# G05 Privacy and Data Protection Policy

**Version:** 0.1 (draft)
**Status:** DRAFT, pending approval by the church data controller and an appropriate Kenyan privacy/legal reviewer. Not to be finalised by the AI development team alone.
**Approved by:** not yet approved
**Approval date:** pending
**Effective date:** pending

---

## 1. Controller and parties

- **Data controller:** Perazim Mission Church, P.O. Box 831-60100, Embu, Kenya.
- **Processors:** external AI providers and hosting providers. **Not yet disclosed in full.** The relay sends online questions to Google Gemini. The actual processing arrangement, location and provider terms must be confirmed and disclosed before launch (see the open items in CHK).
- The on-device model processes questions on the phone. Where any question leaves the device, the disclosure below applies.

## 2. Sensitive data

Conversations may reveal religious belief, health, family, financial and counselling information. These are treated as **sensitive personal data**. The Data Protection Act 2019 includes belief and health in its sensitive categories. Legal review must confirm the exact provisions.

## 3. Impact assessment

- A **Data Protection Impact Assessment (DPIA)** is required before launch. Section 31 of the Act requires an assessment where processing is likely to result in high risk to individuals' rights and freedoms. This processing is treated as high risk.
- The DPIA is a pre-launch gate in G10. It is a church action item (CHK item 5).

## 4. Children

- Section 33 sets conditions for processing children's data, including parental or guardian consent and the child's best interests.
- The AI Guide is adults-only (G03 section 7). The Guide must not collect a minor's data for AI conversation.

## 5. What the app must disclose

- That the Guide is an AI service and not a pastor or confidential counsellor.
- That conversations may be stored, which data is stored, for how long, and who can read it.
- Which providers process data, where, and whether any provider uses data to train its models.
- How to request deletion, and what happens to deletion requests.
- That crisis messages are not automatically sent to pastors (unless and until a disclosed process exists).

## 6. Storage, access and retention

| Question | Position |
|---|---|
| Are AI conversations stored? | **Built app:** the conversation context is held in memory (`ConversationHistory`, cleared with `clear()`). The retention periods and any server-side storage must be confirmed by code review before this statement is published. |
| Who can read conversations? | No unrestricted pastoral access. Narrow roles only (G04) |
| Can pastors read all conversations? | No |
| Can administrators access pastoral discussions? | Only under a specific, approved, lawful and necessary process, logged |
| Are conversations sent to external providers? | Yes for online answers, via the relay to Gemini. Disclosure required |
| Are conversations used to train external models? | Must be disclosed accurately, with the provider's controls stated. To be confirmed in provider terms |
| Can members request deletion? | Required. A deletion procedure must be implemented and tested |
| Are sensitive conversations used in analytics? | Data minimisation and de-identification required. Aggregated daily counts with no user ID only (as agreed for the engagement feature) |

The relay states it does not log question text. That claim must be verified in the server code and infrastructure before it is repeated to members.

## 7. Breach response

A written breach-response procedure is required: detection, containment, assessment, reporting and communication, including statutory notification to the Office of the Data Protection Commissioner within the period required by the Act. The legal reviewer must confirm the period.

## 8. Data subject rights

Members must be able to learn what is held about them, request correction, and request deletion. The exact procedure is part of the DPIA and the legal review.

## 9. Built-app baseline

- Online AI is off in release builds (`ONLINE_AI_ENABLED=false`) and on in debug and validation builds.
- Conversation history is in memory.
- The relay states no question logging.

Items 6 and 7 above need implementation evidence before launch.

## 10. Requirements to implement (additions)

1. An in-app disclosure screen covering section 5 before first AI use.
2. A deletion-request path.
3. A breach-response runbook.
4. A verified and published list of providers and processing locations.

## 11. Revision history

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-11-23 | First draft |
