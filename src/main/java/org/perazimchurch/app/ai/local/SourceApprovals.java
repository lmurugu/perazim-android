package org.perazimchurch.app.ai.local;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.json.JSONObject;

/**
 * Verifies the per-entry approver signature on a signed governance entry (S2-06 item 3).
 * The approver signs the approval facts with an APPROVER key. The package signature alone cannot establish
 * approval, because the package key holder could otherwise name any approver id.
 */
final class SourceApprovals {
    static final String DOMAIN = "perazim-source-approval-v1\n";
    static final String CHURCH_APPROVED = "CHURCH_APPROVED";
    private static final String[] SIGNED_FIELDS = {
            "id", "version", "category", "approvedBy", "approvedAt", "scope", "sha256"};

    private SourceApprovals() {}

    /**
     * Throws unless the entry is acceptable. A CHURCH_APPROVED entry must carry an approvalSignature that verifies
     * under an APPROVER key whose id equals approvedBy. Other entries may omit it; if present, it must still verify.
     */
    static void verifyEntry(JSONObject entry, Map<String, String> trust) throws Exception {
        boolean hasSignature = entry.has("approvalSignature") && !entry.isNull("approvalSignature");
        if (!hasSignature) {
            if (CHURCH_APPROVED.equals(entry.optString("category", ""))) {
                throw new SecurityException("Church-approved source lacks an approver signature.");
            }
            return;
        }
        String approvedBy = entry.optString("approvedBy", "");
        byte[] approverKey = TrustAnchors.resolve(trust, approvedBy, TrustAnchors.Purpose.APPROVER);
        byte[] signature = Base64.getDecoder().decode(entry.getString("approvalSignature"));
        if (signature.length != 64) throw new SecurityException("Source approval signature is malformed.");
        JSONObject signed = new JSONObject();
        for (String field : SIGNED_FIELDS) {
            signed.put(field, entry.has(field) && !entry.isNull(field) ? entry.get(field) : JSONObject.NULL);
        }
        byte[] message = (DOMAIN + CanonicalJson.canonical(signed)).getBytes(StandardCharsets.UTF_8);
        if (!Ed25519Verifier.verify(approverKey, message, signature)) {
            throw new SecurityException("Source approval signature is invalid.");
        }
    }
}
