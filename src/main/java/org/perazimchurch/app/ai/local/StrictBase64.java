package org.perazimchurch.app.ai.local;

import java.io.ByteArrayOutputStream;

/**
 * Strict standard Base64 decoder for API 24 (java.util.Base64 needs API 26). Rejects anything the Node signer would not
 * produce: non-alphabet characters, bad padding, and non-canonical trailing bits.
 */
final class StrictBase64 {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private StrictBase64() {}

    static byte[] decode(String text) {
        if (text == null || text.isEmpty() || text.length() % 4 != 0) throw new IllegalArgumentException("bad base64 length");
        int padding = 0;
        if (text.endsWith("==")) padding = 2; else if (text.endsWith("=")) padding = 1;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bits = 0, count = 0;
        for (int i = 0; i < text.length() - padding; i++) {
            int v = ALPHABET.indexOf(text.charAt(i));
            if (v < 0) throw new IllegalArgumentException("bad base64 character");
            bits = (bits << 6) | v;
            count += 6;
            if (count >= 8) { count -= 8; out.write((bits >> count) & 0xff); }
        }
        // Remaining bits after the last full byte must be zero, which is what the signer produces.
        if (padding > 0 && (bits & ((1 << count) - 1)) != 0) throw new IllegalArgumentException("non-canonical base64");
        for (int i = text.length() - padding; i < text.length(); i++) {
            if (text.charAt(i) != '=') throw new IllegalArgumentException("bad base64 padding");
        }
        return out.toByteArray();
    }
}
