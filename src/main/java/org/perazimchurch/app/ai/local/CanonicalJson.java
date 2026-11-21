package org.perazimchurch.app.ai.local;

/** Canonical JSON string quoting shared with the Node signer. Android-free so it can be host-tested. */
final class CanonicalJson {
    private CanonicalJson() {}

    /**
     * Quotes a string exactly as the Node signer's JSON.stringify does (S2-01). org.json also escapes "</" and
     * U+0080-U+009F and U+2000-U+20FF, which makes canonical bytes differ from the signer's for ordinary text.
     */
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
}
