# Knowledge-package governance design (2026-11-20)

**Status:** the decision logic is implemented and host-tested (`SourceGovernance.java`, `SourceGovernanceTest`). It is **not yet connected** to the offline knowledge store, and no source is approved.

## 1. Categories
| Category | Meaning | May establish official policy? |
|---|---|---|
| CHURCH_APPROVED | Individually approved church source | Yes, only with every condition in section 2 |
| GENERAL_THEOLOGY | Reference theology, not church policy | No (cited as reference) |
| HISTORICAL | Past documents kept for context | No |
| UNAPPROVED | Present but not approved | No |
| EXCLUDED | Must not be retrieved | No |

## 2. Conditions for official authority (all required, otherwise fail closed)
1. Category CHURCH_APPROVED.
2. A named approver (`approvedBy`).
3. An approval date (`approvedAt`).
4. A scope (`scope`) describing permitted use.
5. Not revoked.
6. Not superseded (`supersededBy` absent).
7. A content hash (`sha256`) equal to the installed content hash.

Only then does the source's authority class become `APPROVED_OFFICIAL`. The output validator accepts an official claim only when the reply cites such an item (`PerazimAiEngine.officialClaimHasApprovedProvenance`).

## 3. Trust boundary
- Approval fields come only from the **manifest**, and the manifest must pass the existing signed-package verification (Ed25519, `Ed25519Verifier`). Content cannot approve itself: `SourceGovernanceTest.selfDeclaredApprovalInContentIsIgnored`.
- Malformed manifest entries are dropped, so they cannot become sources.

## 4. Supersession and revocation
- A newer version does not supersede an older one unless the manifest records `supersededBy` by a trusted decision.
- Revocation: `revoked: true` removes official authority and context retrieval.
- Superseded sources stay available as HISTORICAL context only when the category is set to HISTORICAL. Current policy does not come from them.

## 5. Not yet implemented
- Wiring into `OfflineKnowledgeStore` retrieval and the `authorityClass` written to evidence items.
- Integrity check of the installed content against `sha256` at retrieval time.
- Auditing of approval records (who approved, when) beyond the manifest fields.
- Any real church source. The owner has approved none.

## 6. Tests (synthetic only)
`SourceGovernanceTest`: approved source gets authority; missing approver, date or scope fails closed; revoked and superseded sources lose authority; hash mismatch or missing hash fails closed; non-church categories never get authority; self-declared approval ignored; malformed entries dropped.
