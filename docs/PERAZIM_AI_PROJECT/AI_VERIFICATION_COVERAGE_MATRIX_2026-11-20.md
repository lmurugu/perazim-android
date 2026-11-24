# AI verification and test-coverage matrix (2026-11-20)

Commit under test: `5bf8502` on `feature/offline-qwen-phone`. Host unit tests: 56 run, 0 failed, 0 skipped (`org.perazimchurch.app.ai.*`).
**Verification levels:** Code (present), Host test (automated, application logic, synthetic), Build (APK assembles), Device (not performed), Model (not performed).
No row is at Device or Model level. Host tests do not show how the real model behaves.

| Requirement | Implementation location | Test (class) | Expected | Actual | Level | Remaining limitation |
|---|---|---|---|---|---|---|
| Out-of-scope questions refused before generation | `PerazimAiEngine.isOutOfScopeForModel` | `OfflineScopeHarnessTest` (150 refuse, 110 pass) | All refused; none of the in-scope refused | Pass | Host test | Keyword and allowlist based; new phrasings may leak |
| Injection in user question refused | `MODEL_OVERRIDE` | `AuthorityBoundaryTest`, `AuthorityCoverageTest` | Refused | Pass | Host test | Pattern list only |
| Human-decision requests referred | `isHumanDecisionQuery` | `AuthorityBoundaryTest`, `AuthorityCoverageTest` | "Am I saved?" refused; teaching allowed | Pass | Host test | Phrasings outside the list may slip through |
| Professional matters get professional wording | `boundaryResponseFor` | `AuthorityBoundaryTest`, `PerazimAiPolicyTest` | Professional wording | Pass | Host test | Keyword list |
| Ordinary out-of-scope keeps approved wording | `outOfScopeResponse` | `AuthorityBoundaryTest` | Exact approved sentence | Pass | Host test | None |
| Instruction hierarchy: system block fixed | `LocalPrompt.build` | `LocalPromptTest`, `ConflictAndHistoryTest.gradualManipulation...` | System block identical whatever the history | Pass | Host test | Does not show the model obeys it |
| Retrieved-document injection stays untrusted | `LocalPrompt.build` evidence block | `SyntheticInjectionAndValidatorTest` | Injection only in UNTRUSTED block | Pass | Host test | Not model compliance |
| Conflicting sources both shown, no ranking by date | `LocalPrompt.build` | `ConflictAndHistoryTest.conflictingSources...` | Both labelled; no "newest" rule | Pass | Host test | Retrieval ranking in `OfflineKnowledgeStore` not tested here |
| Conflict resolved by wording alone is flagged | `claimsOfficialStatus` + `officialClaimHasApprovedProvenance` | `ConflictAndHistoryTest.conflictResolvedByWordingAlone...` | Flagged | Pass | Host test | Pattern list |
| Official claim needs approved provenance | `officialClaimHasApprovedProvenance` | `AuthorityCoverageTest.officialClaimNeedsApprovedProvenance...` | Allowed only with cited `APPROVED_OFFICIAL`, citable item | Pass | Host test | Depends on package metadata that is not yet produced |
| "Not established" statements are not official claims | `CLAIMED_OFFICIAL_STATUS` lookahead | `AuthorityCoverageTest.notEstablished...` | Not flagged | Pass | Host test | None known |
| Fabricated approval in history stays out of system block | `LocalPrompt.build` | `ConflictAndHistoryTest.fabricatedApproval...` | Not in system block | Pass | Host test | Model may still repeat it; output check covers only pattern claims |
| Forged previous-reply not trusted | `LocalPrompt.build`, `claimsAppActionOrHumanAuthority` | `ConflictAndHistoryTest.forgedPreviousReply...` | Not trusted | Pass | Host test | Same |
| App-action claims rejected | `CLAIMED_APP_ACTION` | `AuthorityBoundaryTest`, `SyntheticInjectionAndValidatorTest` | Rejected | Pass | Host test | Pattern list |
| Human-authority claims rejected | `CLAIMED_HUMAN_AUTHORITY` | `SyntheticInjectionAndValidatorTest`, `AuthorityCoverageTest` | Rejected | Pass | Host test | Pattern list |
| Legitimate replies not suppressed | `claimsAppActionOrHumanAuthority` | `SyntheticInjectionAndValidatorTest` (10), `AuthorityCoverageTest` (7) | Pass | Pass | Host test | Sample size is small |
| Citation required and verified | `OnDeviceAi.hasVerifiedCitations`, `CitationGrammar` | `OfflineCitationGrammarTest` | Verified | Pass | Host test | Not device-tested |
| Offline knowledge store policy | `OfflineKnowledgeStore` | `OfflineKnowledgeStorePolicyTest` | Per existing tests | Pass | Host test | Retrieval ranking and signed packages not exercised end-to-end |
| Online AI disabled | `build.gradle` `ONLINE_AI_ENABLED=false` | Build config (inspection) | false in all variants | Verified by inspection | Code | Re-check each build |
| Offline Qwen only in debug | `build.gradle` debug block | Build config (inspection) | true in debug only | Verified by inspection | Code and Build | Device run not done |
| Model runs offline on device | `OnDeviceAi` | None | Inference offline | Not performed | — | Device test required |
| Model failures reported | `OnDeviceAi` | None | Error shown, no fabricated answer | Code present | Code | Not tested |

## Gaps (not yet tested)
- Retrieval ranking and provenance end-to-end (needs Android context or a package fixture).
- Model failure paths on device.
- Real-model behaviour on any of the rows above.
- Privacy of logs (the existing code discards engine stderr; not audited across the app).
