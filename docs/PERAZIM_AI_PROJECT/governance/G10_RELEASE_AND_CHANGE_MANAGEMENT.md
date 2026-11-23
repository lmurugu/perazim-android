# G10 AI Release and Change Management Policy

**Version:** 0.1 (draft)
**Status:** DRAFT, pending approval by church leadership and the product owner
**Approved by:** not yet approved
**Approval date:** pending
**Effective date:** pending

---

## 1. Three release permissions

These are separate releases. They must not be treated as one.

| Permission | Audience | Gate |
|---|---|---|
| Internal testing | Authorised project personnel | Technical and theological test pass (G09) |
| Registered adult members | Adult members, verified at registration | Pilot results, DPIA complete, privacy disclosure live |
| Wider public, including non-members | Public | Further security, privacy, load and governance validation, and a separate approval |

## 2. Sequence

1. **Closed technical and theological testing**: simulated conversations, synthetic accounts, difficult questions, critical security and safety checks.
2. **Controlled member pilot**: 20 to 50 selected adult members for **at least 2 to 4 weeks**. Voluntary. They receive a clear explanation of limits and data practices. The window is a minimum, not an automatic launch date.
3. **Church-wide adult availability**: after pilot findings are fixed and approvals obtained.
4. **Wider public availability**: only after the church-wide release has performed acceptably and the public-access requirements are met.

The pilot sample includes people with different levels of biblical knowledge, education, digital literacy and church experience.

## 3. Pre-launch gates

- All G09 thresholds met.
- DPIA completed and approved (G05).
- Privacy disclosure implemented (G05 section 10).
- Crisis directory verified and displayed (CD).
- Under-18 restriction and child-safety fallback implemented and tested (G03, G08).
- Permission enforcement tested server-side (G04).
- Independent security and privacy review completed, with critical findings resolved (G08 section 7).
- Official approvals recorded for G01 to G10 and the approved position papers.

## 4. In-app issue reporting

Members can report an inaccurate interpretation, a dangerous recommendation, an offensive response, a privacy concern, or a technical fault, without contacting the developer personally. **Requirement to implement.** This is an addition and does not contradict the built app.

## 5. Change control

| Change type | Route |
|---|---|
| Change to doctrine, doctrinal status, or position papers | Final approver (G01, G02) |
| Change to pastoral, safeguarding or crisis behaviour | Church leadership and safeguarding reviewer |
| Change to privacy or data handling | Data controller and privacy reviewer (G05) |
| Change to permissions or roles | Church leadership and product owner (G04) |
| Model update, prompt change, knowledge package, provider change | Technical change process, then full regression (G09) |
| Technical change that does not alter doctrinal or safety behaviour | Technical change process |

No uploaded document is adopted automatically.

## 6. Rollback

- Every release is versioned and can be rolled back to the previous approved version.
- A critical failure found after release triggers rollback or an immediate restriction, then regression testing.
- Crisis directory errors are treated as safety defects and corrected immediately.

## 7. Built-app baseline

Release builds currently keep online and offline generative AI off (`ONLINE_AI_ENABLED=false`, `OFFLINE_QWEN_ENABLED=false`). The three-stage release and the pilot are **not yet in place** and are requirements for the release record.

## 8. Revision history

| Version | Date | Change |
|---|---|---|
| 0.1 | 2026-11-23 | First draft |
