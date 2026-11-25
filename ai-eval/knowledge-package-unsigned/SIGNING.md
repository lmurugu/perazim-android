# Signing the unsigned knowledge package (pre-signing estimate)

Status: UNSIGNED. The product will refuse this file until it is signed. Nothing in the app reads it.

## What the file is
- `package.unsigned.json`: the envelope `{ payload, signingKeyId, signature }` in the product format `perazim-knowledge-v1`.
- Built by `scripts/knowledge/build_unsigned_package.py` from approved material only:
  - APPROVED_LEVI doctrine entries (16 documents, Perazim view first, KJV verses, no-verdict line),
  - UNDER_REVIEW developing views, labelled as under review, and UNDECIDED topics (explained without a position),
  - the comparative knowledge base 1.5.22 (27 APPROVED_LEVI entries, from PR 76 at d052d7c; copy in `source/`),
  - the Nicene-Constantinopolitan Creed of 381,
  - Hitchcock's Bible Names Dictionary (approved factual source, 2,626 entries).
- Not included, on purpose: Henry and Wesley commentaries (optional download), Smith, Easton and Nave's (the 2 MB package
  limit cannot hold them), cross-references and places (not model-facing text), the other creeds (not approved),
  DRAFT_PENDING_LEVI items (for example the 66-book canon).
- The payload's `contentDigest` is the SHA-256 of the canonical payload without that field. Each document's `contentHash`
  is the SHA-256 of its text.

## Before signing, check
1. `python3 scripts/knowledge/build_unsigned_package.py` reproduces the same file. The digest changes only if the source files change.
2. The document list is what you expect: `python3 -c "import json;d=json.load(open('package.unsigned.json'))['payload']['documents'];print(len(d))"`.

## What Levi does on the laptop (offline)
1. Sign the canonical payload with the package key (Ed25519) that the app trusts for `PACKAGE`:
   - canonical form: keys sorted, no whitespace, strings as JSON.stringify writes them (`json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))`);
   - sign the UTF-8 bytes of that string;
   - set `signature` to the Base64 signature and `signingKeyId` to the key's id as the app's trust anchors list it.
2. Sign the withdrawal control manifest as well. The product returns no evidence without a current, signed control
   manifest (`readWithdrawalControl`). A control manifest is NOT built yet; it is a separate step.
3. Put the signed envelope in the validation assets as `ai/knowledge/package.json` (and the control manifest as
   `ai/knowledge/control.json`) only when Levi decides to ship it.

## After signing
The product path (`OfflineKnowledgeStore.retrieve`) should return the same documents as the harness for the same
question. The harness currently uses `PreSigningEvidence` (a keyword match over this file). Label every score from it
as "pre-signing estimate".

## Not yet built
- The control manifest (see step 2).
- The governance array and the approver signatures (`SourceApprovals.verifyEntry`). The product accepts a package
  without a governance array, but source governance will be missing until it is added.
