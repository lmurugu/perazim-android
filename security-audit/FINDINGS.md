# Session 2 — Signed knowledge-package security audit

Branch: `ai/s2-crypto-audit` (base `feature/offline-qwen-phone` @ `15c6964`).
Scope: read-only audit of the signature, manifest, checkpoint and revocation code, plus
isolated verifier fix and harness. Session 1 files (`SourceGovernance.java`,
`OfflineKnowledgeStore.java`) were NOT edited; their changes are proposed below.
Synthetic keys only. No phone install, no device runs, no production data.

Reproduce: `security-audit/harness/run_harness.sh` (needs node, JDK, org.json jar via `JSON_JAR`).

## Findings, ranked

| ID | Severity | Status | Summary |
|----|----------|--------|---------|
| S2-01 | High (availability / integrity of updates) | Proposed patch for Session 1 | Java canonical JSON escapes characters that Node does not. Any package containing curly quotes, em dashes, U+0080–U+009F or `</` fails verification. Fail closed, but real updates would never install. |
| S2-02 | High (governance) | Contract needed with Session 1 | Approval metadata is not part of the verified document schema. `SourceGovernance.parse` takes an unsigned `JSONArray`. Nothing enforces that approval fields come from the signed payload. Self-granted approval is possible if wired from anywhere else. |
| S2-03 | High (governance) | Contract needed with Session 1 | Revocation and supersession are enforced only for withdrawn `sourceId+version` pairs. A rollback can restore a package whose approval was later revoked through package metadata. Retrieval does not consult `SourceGovernance`. |
| S2-04 | Medium | Fixed on branch | `Ed25519Verifier` accepts the identity public key (and identity R) with S=0 as a valid signature for any message. Exploitable only if the identity key is in a trust set. |
| S2-05 | Medium | Proposed | Checkpoint trust-on-first-use. If install/control prefs are missing, `read()` and `install()` re-establish high-water marks from whatever package is on disk. Deleting prefs resets the publish watermark and the withdrawal sequence. |
| S2-06 | Medium | Proposed | No purpose binding on trust anchors. Package, control, rollback-package and rollback-authorization keys share one namespace. A compromised package key can sign a rollback authorization. No key-revocation mechanism exists; key compromise needs an app release. |
| S2-07 | Medium | Proposed | Expiry uses the device wall clock with no monotonic floor. Setting the clock back revives expired packages and withdrawal manifests. |
| S2-08 | Low | Proposed | Duplicate `sourceId+version` documents are accepted in packages. The withdrawal manifest rejects duplicates; packages do not. |
| S2-09 | Low | Proposed | Trust-key parser accepts 44-byte keys and silently uses the last 32 bytes without checking the SPKI header. |
| S2-10 | Low | Unverified | Android `org.json` duplicate-key behaviour not tested on device. Signature covers the parsed object, so no bypass found, but a strict parser is recommended. |

## Evidence by finding

**S2-01 canonical JSON mismatch (verified on JVM).**
Node signer (`server/src/phase-b/offlineKnowledgePackage.js` `canonical`) uses `JSON.stringify` for strings.
Android `OfflineKnowledgeStore.canonical` relies on `org.json` quoting, which escapes `/` after `<`
and U+0080–U+009F and U+2000–U+20FF.
Test: `harness/CanonicalHarness.java` (verbatim copy of the Android method) against Node output.
Result: `"Peace ’ grace — hope"` gives `’` and `—` on the Java side only. `a</b>` gives `a<\/b>`.
Of 200 random strings, 180 canonicalise differently. The validation fixtures are ASCII, so the existing
fixtures did not catch this.
Note: harness uses JSON-java 20240303. Android's `org.json` escaping of these ranges is assumed to match.
Confirm on device (`OfflineKnowledgeStoreDeviceTest`) before release.
Proposed patch (Session 1 to apply in `OfflineKnowledgeStore.canonical`, string branch):
```java
static String jsonString(String s) {
    StringBuilder b = new StringBuilder(s.length() + 2).append('"');
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        switch (c) {
            case '"': b.append("\\\""); break;
            case '\\': b.append("\\\\"); break;
            case '\b': b.append("\\b"); break;
            case '\f': b.append("\\f"); break;
            case '\n': b.append("\\n"); break;
            case '\r': b.append("\\r"); break;
            case '\t': b.append("\\t"); break;
            default:
                if (c < 0x20) b.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                else b.append(c);
        }
    }
    return b.append('"').toString();
}
```
Replace `JSONObject.quote(...)` in `canonical` with `jsonString(...)`. Verified against 510 strings with
zero mismatches (`harness/fuzz_strings.js`, `harness/java/.../ProposedCanonicalHarness.java`).
Add a device or JVM test that signs a Unicode-text package with the Node signer and verifies it on Android.

**S2-02 approval metadata not bound.**
`verify()` (OfflineKnowledgeStore ~l.205–280) reads only `sourceId, version, approvalReference, location, text, contentHash`
from each document. `approvalAuthority` is signed (server l.47) but never read on device. `category`, `approvedBy`,
`approvedAt`, `scope`, `sha256`, `revoked`, `supersededBy` are consumed only by `SourceGovernance.parse(JSONArray)`,
which has no callers. Nothing ties that array to the verified payload.
`approvalReference` is only checked for non-empty. There is no approver allowlist in the trust anchor.
Contract to agree with Session 1: governance fields are read only from a `VerifiedPackage` document; `SourceGovernance.parse`
accepts only that path (or a package-verified wrapper type); approvers are an allowlist keyed by the signed key id or a
signed approver id; `approvedAt <= publishedAt` is checked.

**S2-03 revocation and supersession.**
Withdrawals are keyed by `sourceId+version` (`verifyWithdrawalManifest`, l.622–636) and filtered in `retrieve()` (l.1028–1040).
A supersession or revocation expressed only in package metadata is not enforced on device. `installAuthorizedRollback`
(l.322–360) can restore that older package, and the rollback is accepted if the controlSequence is newer.
`retrieve()` does not call `SourceGovernance.isOfficiallyApproved` or `mayRetrieveForContext`.
Required: revocations must be in the withdrawal manifest, or rollback must refuse packages that contain revoked or superseded
sources. Retrieval must apply governance status as well as withdrawals.

**S2-04 identity-key forgery (fixed).**
`Ed25519Verifier.verify` checks `x*L == identity` for A and R. The identity element passes this check. With A=identity,
R=identity, S=0, any message verifies. Probe vector: `probe-identity-key-forgery` (expected false, was true).
Fix applied on this branch: reject `a.equals(IDENTITY)` and `r.equals(IDENTITY)` (file `Ed25519Verifier.java`).
After the fix: 0 mismatches over 222 vectors (valid, tampered message, tampered signature, malleated S+L, wrong key,
identity probe, 200 bulk valid signatures).

**S2-05 checkpoint trust-on-first-use.**
`read()` (l.~383–393): if `readInstallCheckpoint` returns null, it writes a new checkpoint from the installed package.
`install()` with null checkpoint accepts any package that is newer than the current file. `readControlCheckpoint` returns null
on missing prefs, so `applyWithdrawalManifest` accepts sequence > 0.
Effect: an attacker with app-private write access can delete the prefs and replace `current.json` with an older signed package,
resetting the publish watermark. The MAC key lives in the Keystore, so a forged checkpoint fails, but a missing one is accepted.
The `legacy` checkpoint MAC branch (l.~785–797) also lacks controlSequence and activeDigest.
Proposed fix: once any package or control file exists, a missing checkpoint is a fail-closed error. Use an explicit one-time
migration marker that is itself MAC-protected. Remove the legacy branch after migration.

**S2-06 key purpose and rotation.**
`trustedKeys()` (l.111–135) builds one map. `verify`, `verifyRollbackAuthorization` and `verifyWithdrawalManifest` look up
`signingKeyId` in that map with no purpose check. Format strings are the only separation. The trust set has no validity
window or revocation list. Proposed: map each key id to a purpose (`PACKAGE`, `CONTROL`, `ROLLBACK_PKG`, `ROLLBACK_AUTH`) and
check the purpose in each verifier. Add a signed key-revocation entry or require an app release to rotate, documented as the
accepted process.

**S2-07 wall-clock expiry.**
Callers pass `System.currentTimeMillis()` (for example `OnDeviceAi.java` l.148–154). Nothing records the highest time seen.
Proposed: store MAC-protected `max_seen_at` on each successful verify and reject `now < max_seen_at - 5s`.

**S2-08 to S2-10:** see table. Proposed fixes: reject duplicate `sourceId+version` in `verify()`; check the 12-byte SPKI prefix
or require 32-byte keys; use a strict JSON parser that rejects duplicate keys.

## Audit checklist coverage (Session 2 mandate, items 1–14)

| # | Area | Result |
|---|------|--------|
| 1 | Ed25519 verification | Correct for tampered msg/sig, wrong key, non-canonical S (S+L rejected). Identity-key gap found and fixed (S2-04). Passes 222 vectors. |
| 2 | Trusted key management | Single namespace, no purpose binding, no revocation (S2-06). Release builds have no `trusted-keys.json` asset, so trust set is empty and everything fails closed (verified: `src/main/assets/ai/knowledge` absent). |
| 3 | Canonical payload | Java/Node mismatch (S2-01). Proposed fix verified. |
| 4 | Manifest integrity | Payload is signed; `contentDigest` is inside the signature. Envelope `signingKeyId` is unsigned but only selects the key; the signature still must verify. OK. |
| 5 | Source content hashes | Each `text` is checked against `contentHash` (l.266–271), and the whole payload is signed. OK. |
| 6 | Approval metadata integrity | Signed as part of payload, but not read on device (S2-02). |
| 7 | Approver identity/authorisation | No allowlist; `approvedBy` is a free string (S2-02). |
| 8 | Revocation and supersession | Withdrawal path works (fail-closed on expiry or mismatch). Supersession and package-metadata revocation not enforced (S2-03). |
| 9 | Key rotation and compromise | No mechanism (S2-06). |
| 10 | Replay and rollback | Install needs strictly newer publishedAt or version. Rollback authorization needs exact target digest and packageId and sequence greater than the checkpoint, so replay is blocked. Watermark reset possible via prefs deletion (S2-05). |
| 11 | Expiry | Enforced at verify and at retrieval (fail closed). Clock-set-back bypass (S2-07). |
| 12 | Malformed and duplicate entries | Malformed envelopes throw. Withdrawal duplicates rejected. Package duplicates not rejected (S2-08). |
| 13 | Self-approval by a document | Not possible from document text: the signature covers all fields. Possible if governance metadata comes from an unsigned source (S2-02). |
| 14 | Modified approval metadata bypassing signature | Not possible for `verify()`-accepted payloads. The risk is in the unwired `SourceGovernance` path (S2-02). |

## Attack scenarios (synthetic, reasoned from code; device tests not run)

1. Tamper approvalReference or text in a signed package → rejected (digest and signature). Covered by the harness at the primitive level; full path needs device or JVM test with the Node signer.
2. Replay older package after newer install → rejected by `isNotNewer` while the checkpoint exists. Fails open after prefs deletion (S2-05).
3. Replay rollback authorization after rollback → rejected by controlSequence. OK.
4. Replay older withdrawal manifest → rejected by sequence, unless prefs are deleted (S2-05).
5. Expired package with clock set back → accepted (S2-07).
6. Self-granted approval through governance metadata → blocked today because nothing reads that metadata. Becomes possible when Session 1 wires it without S2-02 contract.
7. Unicode-text package from the real signer → rejected today (S2-01). Fail closed, but blocks updates.

## Limitations

- Not run on Android. `OfflineKnowledgeStoreDeviceTest` was not executed (no phone install by mandate).
- JVM harness uses JSON-java for `org.json`; Android's implementation was not executed.
- The Java canonical copy in `harness/java/.../CanonicalHarness.java` is extracted verbatim from `OfflineKnowledgeStore.java` at `15c6964`. Re-extract if that method changes.
- No cryptographic protocol change proposed. The existing Ed25519 + SHA-256 canonical scheme can be extended as described.
