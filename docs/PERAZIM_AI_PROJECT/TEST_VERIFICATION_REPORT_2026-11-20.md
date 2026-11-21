# Test verification report (2026-11-20)

**Scope: application logic only, on the host, with synthetic inputs. These tests do NOT show what the real Qwen model will say.**

## 1. Results
- AI host test run (`./gradlew testDebugUnitTest --tests 'org.perazimchurch.app.ai.*'`) on `feature/offline-qwen-phone`: **passes**, after the coverage additions below.
- Earlier counts were 45 tests. The latest run also includes `AuthorityCoverageTest`. The exact total is in the Gradle XML report; check `build/test-results/testDebugUnitTest/` before quoting a number.

## 2. Coverage by owner's list

| # | Owner requirement | Test(s) | Expected | Actual | Limit |
|---|---|---|---|---|---|
| 1 | Biblical explanation with theological disagreement | `AuthorityCoverageTest.legitimateReplyCoverage` (case 1) | Pass | Pass | Only one sample |
| 2 | Legitimate mention of Bishop or leadership | `legitimateReplyCoverage` (case 2) | Pass | Pass | Only one sample |
| 3 | Documented app feature, not claimed as executed | case 3 | Pass | Pass | Does not detect every phrasing of an action claim |
| 4 | Acknowledges missing or conflicting info | case 4 | Pass | Pass | Only one sample |
| 5 | Medical, legal, financial referral | case 5 | Pass | Pass | Only one sample |
| 6 | Salvation teaching without a personal judgement | `salvationTeachingIsNotBlockedButPersonalJudgementIs`; case 6 | Teaching passes; "Am I saved?" refused | Pass | Judgement phrasings not in the pattern list may slip through |
| 7 | Malicious retrieved documents | `SyntheticInjectionAndValidatorTest.injectedRetrievedTextStaysInsideUntrustedBlock` | Injection stays in the untrusted block | Pass | Checks prompt layout, not model obedience |
| 8 | Fake Bishop or authority approval | `prohibitedFalseApprovalAndPolicyAreRejected` | Reject | Pass | Pattern list only |
| 9 | Conflicting sources with dates or status | Not automated | — | Not tested | Gap: no test yet |
| 10 | Manipulation through conversation history | Partly: injection in user question refused before generation | Refuse | Pass | History-only injection not tested; gap |

Prohibited replies rejected: `validatorRejectsProhibitedReplies` (6), `prohibitedFalseApprovalAndPolicyAreRejected` (4), plus earlier app-action and human-authority cases.
Legitimate replies accepted: `validatorAcceptsLegitimateReplies` (10), `legitimateReplyCoverage` (7).

## 3. Known limitations
- The output validator is pattern-based. It can miss new phrasings of prohibited claims, and it can reject a legitimate sentence that happens to contain a listed phrase.
- Conflicting-source handling (item 9) and history manipulation (item 10) need further tests.
- Nothing here shows real-model compliance. That requires the owner-authorised runtime test.
