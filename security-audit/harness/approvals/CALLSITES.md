# S2-06 call sites for Session 1 (OfflineKnowledgeStore.java)

Session 2 added `TrustAnchors`, `SourceApprovals` and `CanonicalJson.canonical`. Session 1 owns `OfflineKnowledgeStore.java`,
so these edits are for Session 1 to apply. Nothing in that file is changed on this branch.

1. `trustedKeys()` and `parseTrustedKeysJson()`: store each value as `TrustAnchors.encode(Purpose, base64Key)`.
   Validation build: `synthetic-device-key` → PACKAGE, `synthetic-rollback-key` → PACKAGE (rollback targets are ordinary packages),
   `synthetic-control-key` → CONTROL, `synthetic-rollback-download-key` → ROLLBACK_AUTH.
   Release `trusted-keys.json`: `{"keyId": "PURPOSE:base64key", ...}`, the same encoding.
2. `verify()`: replace the `trustedKeys.get(keyId)` block with
   `byte[] rawPublicKey = TrustAnchors.resolve(trustedKeys, keyId, TrustAnchors.Purpose.PACKAGE);`
   then keep the existing `Ed25519Verifier.verify(rawPublicKey, ...)` call.
3. `verifyRollbackAuthorization()`: use `Purpose.ROLLBACK_AUTH`.
4. `verifyWithdrawalManifest()`: use `Purpose.CONTROL`.
5. `verify()`: before `SourceGovernance.fromVerifiedPayload(...)`, add
   ```java
   JSONArray governance = payload.optJSONArray("governance");
   if (governance != null) for (int i = 0; i < governance.length(); i++) {
       SourceApprovals.verifyEntry(governance.getJSONObject(i), trustedKeys);
   }
   ```
   Any failure throws, so the package is rejected.
6. `canonical()` in OfflineKnowledgeStore: optionally delegate to `CanonicalJson.canonical`. The two bodies are identical today.

Server signer (needs Levi's approval, as already agreed): each governance entry gets `approvalSignature`, the base64 Ed25519 signature
over `"perazim-source-approval-v1\n" + canonical({id, version, category, approvedBy, approvedAt, scope, sha256})`, with nulls for absent fields.
Levi signs these offline with the APPROVER key. The server stores the signature and does not hold the key.

Known limits: TrustAnchors and CanonicalJson use `java.util.Base64`, which needs API 26. Check the minSdk before merging, or switch
to `android.util.Base64`. Revocation (`revoked`) and `supersededBy` are not covered by the approval signature. They stay package-signed
and authoritative through the withdrawal and rollback guards.
