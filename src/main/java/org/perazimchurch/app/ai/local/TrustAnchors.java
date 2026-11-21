package org.perazimchurch.app.ai.local;

import java.util.Map;

/**
 * Purpose-bound trust anchors (S2-06). A trust map value has the form "PURPOSE:base64(32-byte Ed25519 key)".
 * Callers pass the map through unchanged; only verifiers resolve keys, and each resolve requires an exact purpose match
 * before any signature is checked. Raw values with no purpose are rejected.
 */
final class TrustAnchors {
    enum Purpose { PACKAGE, CONTROL, ROLLBACK_AUTH, APPROVER }

    private static final String REFUSED = "Signing key is not trusted for this purpose.";

    private TrustAnchors() {}

    static String encode(Purpose purpose, String base64Key) {
        return purpose.name() + ":" + base64Key;
    }

    /** Returns the raw 32-byte public key for keyId, only if it is trusted for exactly the required purpose. */
    static byte[] resolve(Map<String, String> trust, String keyId, Purpose required) {
        String value = (trust == null || keyId == null) ? null : trust.get(keyId);
        if (value == null) throw new SecurityException(REFUSED);
        int separator = value.indexOf(':');
        if (separator <= 0) throw new SecurityException(REFUSED);
        Purpose purpose;
        try {
            purpose = Purpose.valueOf(value.substring(0, separator));
        } catch (IllegalArgumentException error) {
            throw new SecurityException(REFUSED);
        }
        if (purpose != required) throw new SecurityException(REFUSED);
        byte[] raw;
        try {
            raw = StrictBase64.decode(value.substring(separator + 1));
        } catch (IllegalArgumentException error) {
            throw new SecurityException("Trusted Ed25519 public key is not valid base64.");
        }
        if (raw.length != 32) throw new SecurityException("Trusted Ed25519 public key has the wrong length.");
        return raw;
    }
}
