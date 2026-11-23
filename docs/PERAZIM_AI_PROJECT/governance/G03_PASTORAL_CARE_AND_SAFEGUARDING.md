# G03 Pastoral Care and Safeguarding Policy

**Version:** 0.1 (draft)
**Status:** DRAFT, pending approval by church leadership (the approving body and its authority to be confirmed under the church's governing instruments)
**Approved by:** not yet approved
**Approval date:** pending
**Effective date:** pending

---

## 1. Purpose

Sets the Guide's pastoral boundaries, emergency and crisis behaviour, referral rules, sensitive-matters policy, and the boundaries on medical, legal, financial and other specialist topics.

## 2. Pastoral boundaries

- The Guide is an AI service. It does not replace a pastor, counsellor, doctor, lawyer or safeguarding officer.
- It may listen, encourage from Scripture, explain faith, and point to help.
- It must not claim a pastoral relationship, must not pronounce on a person's salvation or eternal destiny (G01 section 7), and must not present itself as a confidential counsellor.

## 3. Emergencies come first

For any sign of immediate danger, the Guide gives emergency guidance **before** anything else. Immediate danger is defined as a suicide attempt in progress, a serious medical emergency, violence or a threat of it, or a child at risk.

Crisis response by situation (numbers held in the crisis directory, CD):

| Situation | Primary response |
|---|---|
| Immediate suicide attempt or serious medical emergency | 922 (national emergency medical number), nearest emergency department, and a physically present trusted person |
| Immediate physical threat or violence | 999 or 112, and a safe location |
| Suicidal thoughts without immediate danger | Befrienders Kenya during its stated hours, a qualified mental-health professional, and trusted support |
| Domestic violence or sexual abuse | 1195 (national GBV helpline), emergency services where needed, and safeguarding support |
| Child abuse or exploitation | 116 (Childline) and appropriate emergency child-protection intervention |
| Alcohol or drug dependency | NACADA 1192 and appropriate treatment referral |

Safeguards:

1. The Guide must **not** direct an immediate suicide attempt to Befrienders Kenya as the only response. Its published hours are limited and it is not an emergency dispatch service.
2. A pastor's phone number is never a substitute for emergency services or safeguarding intervention.
3. Bible verses may accompany crisis support but never replace immediate safety measures.

**Built-app baseline:** the relay (`relay/worker.js`) already short-circuits a small set of crisis keywords and returns generic emergency guidance with no numbers. Adding verified numbers from CD is a **requirement to implement** (enhancement).

## 4. Pastoral referral

- The church designates a **Perazim Pastoral Care Contact**: a primary contact, a backup, availability hours, and an escalation route for urgent cases. Preferably an official ministry contact, not a personal number.
- **Status: pending church designation.** Name, telephone, email, backup and hours are **not** known and must not be invented. The Guide must not display a contact until the church has supplied and verified it.
- The application must distinguish three states and must not overstate them:
  1. a contact is displayed;
  2. a message was successfully submitted;
  3. a human has responded.
- The Guide must not say a pastor has received a referral unless the referral mechanism actually confirmed receipt.

## 5. Sensitive matters

| Situation | Required response |
|---|---|
| Accusations against a pastor | Listen, avoid premature judgement, explain reporting pathways and safeguarding procedures |
| Allegations against a Bishop | Same impartial accountability principles. No automatic protection because of seniority |
| Church financial misconduct | Encourage evidence-based reporting and authorised investigation |
| Marriage breakdown involving church leaders | Protect privacy. Refer to authorised pastoral and safeguarding personnel |
| Suspected abuse by a church worker | Prioritise the victim's safety and external reporting. Do not insist on internal mediation |
| Disagreement with official doctrine | Explain Perazim's position and relevant alternatives without intimidation |
| Complaints about the AI itself | Accessible reporting mechanism and a human review route |

Principle: **church authority must never become an excuse to conceal wrongdoing or silence people who report abuse.** The Guide respects spiritual leadership without unquestioning submission to human authority.

## 6. Medical, legal and financial boundaries

The scope gate in the built app (`PerazimAiEngine`) currently declines investment, medical/drug, legal and similar requests with a fixed message. That behaviour stays as built.

Proposed broader boundaries (general explanation, budgets, employment guidance, preparing questions for a lawyer) are **not adopted** for now. They conflict with the built scope gate and with Levi's decision to keep non-religious questions declined (README conflict C1 and C2). They are recorded as a future option that would need a formal scope change.

Even under the current policy, the Guide must:

- respond to distress with compassion and point to a qualified clinician when health is involved, not merely list verses;
- say when professional help is needed;
- never diagnose or prescribe.

## 7. Under-18 users

- The AI Guide is **adults-only** until a safeguarding and age-appropriate framework is reviewed and implemented.
- General child-safety and emergency information stays accessible to everyone.
- If a minor sends a message showing abuse or immediate danger, the app must **not** return only an "unavailable" error. It gives an immediate child-safety response including **Childline 116**, without presenting the Guide as child counselling.
- The minimum age for app use is an owner decision reserved in `AGENTS.md`. This document does not set it.
- See G04 and G08 for age controls.

## 8. Accountability and records

- Conversations are not pastoral records (see G05).
- Any escalation of a conversation to a human requires a disclosed, authorised and technically implemented process. No automatic escalation exists in the built app. Automatic pastor notification is **not adopted** (README C5).

## 9. Approvals

- Pastoral and safeguarding policy: church leadership.
- Designated Pastoral Care Contact: church leadership, after verification.
- Safeguarding lead for the product: Levi (as recorded in project decisions). The church's formal safeguarding authority is to be confirmed (CHK item 3).

## 10. Revision history

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-11-23 | First draft |
