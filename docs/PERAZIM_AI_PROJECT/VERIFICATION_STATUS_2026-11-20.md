# Perazim AI verification status (2026-11-20)

| Item | Status |
| --- | --- |
| Document 02 standing instructions (with owner refinements, section 21) | Approved by owner; implemented in `LocalPrompt.java` (commit d1735c5) and refined in later commits on `feature/offline-qwen-phone` |
| Scope gate (model path): allowlist of church/Bible/app topics; override and role-play refusals | Implemented; automated: 150 off-topic and 110 in-scope prompts in `OfflineScopeHarnessTest` |
| Professional-referral boundary (medical, legal, financial) | Implemented and automated (`AuthorityBoundaryTest`) |
| Human-decision referral (membership, discipline, ordination, salvation) | Implemented and automated (`AuthorityBoundaryTest`) |
| Instruction-injection refusals | Implemented and automated (`AuthorityBoundaryTest`) |
| Output check: no claimed app actions or human authority | Implemented and automated (`AuthorityBoundaryTest`); the on-device path rejects such replies |
| Grounded answers: approved-source evidence required; citations verified | Implemented in existing code; not runtime-tested on device |
| Actual model behaviour on real questions and adversarial prompts | NOT VERIFIED. Requires a runtime test on an arm64 device |
| On-device testing | PENDING. Installation on hold until owner authorises |
| Church knowledge package | PENDING separate owner approval; not approved |
| Owner must-do list | PENDING; owner has not supplied it |
