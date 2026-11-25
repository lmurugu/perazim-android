# Gate round 24 (886 cases, automatic grader)

- Run ID: 2026-11-25-plus83-gate-r24
- Code commit (full SHA): d9c0cc7c131d8d95087030a3346ef84d6cd9328a
- Cases: cases-used.jsonl (same 886 cases, graded against the r16 file).
- Strict: 775/886 (87.5%). Decline-pass: 794/886 (89.6%). Round 23: 774 and 793.
- Critical failures (auto checks): 0. Safety 62/62, privacy 36/36, privacy_confidentiality 36/36.
- Regressions against round 23: 0. Fixed: KE-070.

## Changes

- Levi's section 6 corrections (review of the r19 to r21 routes) applied: HIV disclosure, depression, unanswered prayer, witchcraft fear, medication, cancer (Luke 5:31 removed), leadership appointment (no Bishop), abuse answers (safety and reporting first), remove-pastor, which-leaders-trust, politics (principles, no endorsement), god-not-a-man, jesus-is-the-father, icons, apocrypha and canon, eastern church (judgement of teaching, not destinies), dreams, and thanks and prayer (split). Stray Psalms 118:24 anchors removed from three routes.
- Every anchor in ApprovedRoutes.java checked against the bundled KJV (195 anchors, none missing).
- Swahili: Levi approved the five phrases and four variants as a native speaker (message cmsg_01TZR4y8Vg1KsGsVuQZmh5wy6mZdLtfvqKyZG6SZVRvYUj). The four variants are added as urgent triggers. No phrase was removed, so detection is not weaker.
- Open: Levi's proposed tier for "nimechoka na maisha" (compassionate safety check, not urgent) is recorded in the checklist, but the code still shows the full crisis response for that phrase. A separate softer response is not built yet. This is the safer side.
- Open: the Wesley part-to-chapter map. Matthew Henry is answered from the approved database when a member names him and a chapter (ReferenceGuide.namedHenry); the host evaluation cannot show it, so it needs a check on the phone.

## Still below the 90% gate

language_pastoral_tone, pastoral_struggle, scripture_exegesis, governance, cult_group, denominational_fairness, research_source_verification, resource_use, and several Kenya-specific areas. Release gates are not met. No merge or release without Levi's approval.
