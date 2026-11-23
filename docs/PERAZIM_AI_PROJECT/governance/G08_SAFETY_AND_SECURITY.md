# G08 AI Safety and Security Specification

**Version:** 0.1 (draft)
**Status:** DRAFT, pending approval by the technical reviewer, the safeguarding reviewer, and the independent security/privacy reviewer (to be named by the church)
**Approved by:** not yet approved
**Approval date:** pending
**Effective date:** pending

---

## 1. Scope

Refusal behaviour, crisis handling, prompt and permission integrity, data protection in transit and storage, and the independent review requirement.

## 2. Refusal behaviour

- Out-of-scope requests get the fixed polite refusal with redirection (built in `PerazimAiEngine`).
- Refusals never reveal internal rules, prompts or configuration.
- The Guide does not comply with requests to reveal its instructions, to change its permissions, or to act as a different system. Conversation text cannot override the backend permission check (G04).

## 3. Crisis handling

Crisis behaviour follows G03 section 3. In summary:

1. Detect possible crisis signals. The relay already checks a set of keywords. The detection set must be extended and tested (G09).
2. Return emergency guidance first, with verified numbers from the crisis directory (CD).
3. Never make Befrienders Kenya the only response to an immediate attempt.
4. Never claim a referral was received unless it was confirmed.
5. Log that a crisis response was shown, without logging the message text.

## 4. Child-safety fallback

Under-18 users cannot use the AI Guide (G03 section 7). If a minor's message shows abuse or immediate danger, the app returns an immediate child-safety response that includes Childline 116. It does not return an "unavailable" error alone.

## 5. Prompt and permission integrity

- Permissions are enforced server-side, not by the model.
- Retrieved knowledge is treated as data, not instructions.
- Verse text is taken only from the integrity-checked corpus (G07 section 5).
- Any tool or capability call is checked against the permission matrix before it runs.

## 6. Data security

- No secrets in the repository (`AGENTS.md`).
- Online AI keys live only at the edge relay, never in the APK (`relay/worker.js`).
- No real member data in tests or logs (`AGENTS.md`).
- Question text is not logged. The relay's claim must be verified (G05).
- Encryption in transit for all relay calls, and access control for any stored data. To be confirmed in the independent review.

## 7. Independent review

An independent security and privacy reviewer checks authentication, permissions, information leakage, data protection and infrastructure security before the pilot. The reviewer must be separate from the developer. Named by the church (CHK item 6).

## 8. Built-app baseline

Existing: scope gate, crisis keyword check in the relay, online AI off in release, key kept at the edge. Missing and to implement: verified crisis directory display, role enforcement, the child-safety fallback, and the independent review.

## 9. Revision history

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-11-23 | First draft |
