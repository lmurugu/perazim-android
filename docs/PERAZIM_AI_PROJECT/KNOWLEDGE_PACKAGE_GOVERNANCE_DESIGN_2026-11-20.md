# Knowledge-package governance design (2026-11-20)

**Status:** the decision logic (`SourceGovernance.java`) is connected to offline retrieval through `GovernedEvidence.java`, called from `OfflineKnowledgeStore.retrieve`. It is host-tested with synthetic data only. Approval status now travels in a `governance` array inside the signed package payload (commit 172161c). The packages published so far carry no approved sources, so retrieval offers no package document as evidence (fail closed). No source is approved.

## 1. Categories
| Category | Meaning | May establish official policy? | May be offered as context? |
|---|---|---|---|
| CHURCH_APPROVED | Individually approved church source | Yes, only with every condition in section 2 | Yes (`APPROVED_OFFICIAL`, citable) |
| GENERAL_THEOLOGY | Reference theology, not church policy | No | Yes (`REFERENCE`, context only, not citable) |
| HISTORICAL | Past documents kept for context | No | Yes (`HISTORICAL`, context only, not citable) |
| UNAPPROVED | Present but not approved | No | No |
| EXCLUDED | Must not be retrieved | No | No |

Useful is not the same as authorised: a source can be offered as context without being allowed to establish an official Perazim position.

## 2. Conditions for official authority (all required, otherwise fail closed)
1. Category CHURCH_APPROVED.
2. A named approver (`approvedBy`).
3. An approval date (`approvedAt`).
4. A scope (`scope`) describing permitted use.
5. Not revoked.
6. Not superseded (`supersededBy` absent).
7. A content hash (`sha256`) equal to the hash of the content actually held.

Only then does the authority class become `APPROVED_OFFICIAL`. The output validator accepts an official claim only when the reply cites such an item (`PerazimAiEngine.officialClaimHasApprovedProvenance`).

## 3. Retrieval pathway (as implemented)
`OfflineKnowledgeStore.retrieve`:
1. Reads the installed package. Package signature, content digests and per-document `contentHash` are checked at install and read time (existing code).
2. Requires a current withdrawal control manifest. If it cannot be read, no package document is offered.
3. Calls `GovernedEvidence.select`, which for each document:
   - drops it if its `sourceId`/`version` is in the withdrawal set;
   - looks up its governance entry. Duplicate entries for the same source id and version are treated as no entry;
   - recomputes SHA-256 of the text actually held, and derives the authority class from the governance entry and that hash;
   - drops it if the authority class is UNAPPROVED or the category may not be used as context;
   - builds a **fresh** evidence object with `authorityClass`, `citationEligible` (true only for APPROVED_OFFICIAL), and, for official items only, `approvedBy` and `approvedAt` from the manifest.
4. Ranks by term overlap and applies the existing limits (2 documents for scripture questions, otherwise 3; 3600 characters).
5. Scripture passages from `LocalScriptureRetriever` are appended as before. They come from app-bundled Bible text, not from the knowledge package, so they are outside this manifest.

## 4. Trust boundary
- Approval fields come only from the signed payload's `governance` array, read after Ed25519 verification (`Ed25519Verifier`). The array is covered by `contentDigest`. `SourceGovernance.fromVerifiedPayload` parses it strictly: `approvedAt` must be an integer epoch-millisecond value no later than `publishedAt`, and any malformed entry rejects the whole package.
- Rollback: `GovernedEvidence.assertRollbackAllowed` refuses a target with a revoked or superseded source, a withdrawn sourceId+version, or unreadable withdrawal status when it has documents. It runs before any slot is written.
- Approver purpose binding (Session 2 item 3) is not yet implemented; it depends on the S2-06 key-map work.
- Fields inside a document (`authorityClass`, `citationEligible`, `category`, `approvedBy`, `approvedAt`, `scope`, `approvalReference`) are never copied into evidence. Tests: `GovernedSelectionTest.documentSelfDeclaredApprovalIsIgnoredWithoutGovernanceEntry` and `selfDeclaredApprovalCannotOverrideGovernedUnapprovedCategory`.
- Malformed manifest entries are dropped, so they cannot become sources.

## 5. Supersession and revocation
- A newer version does not supersede an older one unless the manifest records `supersededBy` by a trusted decision.
- Revocation (`revoked: true`) removes both official authority and context retrieval.
- Superseded sources are offered only as HISTORICAL context, and only when the category is set to HISTORICAL. Current policy does not come from them.

## 6. Behaviour change to note
Until a signed approval manifest is provided, `retrieve` returns no package evidence. The on-device answer path therefore reports that no verified approved source supports the question. This affects the validation build's bundled synthetic package. It is intentional: no source has been approved.

## 7. Not yet implemented
- Approver allowlist with purpose binding (S2-06).
- Monotonic clock floor (S2-07), checkpoint migration (S2-05), key purposes (S2-06), duplicate-key JSON parsing (S2-10): Session 2 proposals, not yet implemented here.
- Auditing of approval records (who approved, when, on what evidence) beyond the manifest fields.
- Governance of bundled scripture passages (not part of the package).
- Device or emulator verification of the retrieval change. Host tests cover the selection step only; `OfflineKnowledgeStore.retrieve` itself is Android-dependent and has not been compiled or run in this session.
- Any real church source. The owner has approved none.

## 8. Tests (synthetic only)
- `SourceGovernanceTest`: approved source gets authority; missing approver, date or scope fails closed; revoked and superseded sources lose authority; hash mismatch or missing hash fails closed; non-church categories never get authority; self-declared approval ignored; malformed entries dropped.
- `GovernedSelectionTest`: approved source offered as official and citable with provenance; self-declared approval ignored without a governance entry; governed UNAPPROVED category cannot be overridden by document fields; empty governance fails closed; content changed after approval dropped; a document's own hash field cannot stand in for the held content's hash; revoked, superseded, withdrawn, EXCLUDED and duplicate-entry sources dropped; theology and historical sources offered as non-citable context; malformed manifest entry creates no source; unrelated questions return nothing.
- Result on 2026-11-20 (host JVM, JUnit 4.13.2, org.json 20231013): 23 tests, 0 failures. Command: javac on `SourceGovernance.java`, `GovernedEvidence.java` and the two test classes, then `org.junit.runner.JUnitCore`.
