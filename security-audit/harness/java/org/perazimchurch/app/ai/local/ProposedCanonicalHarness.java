package org.perazimchurch.app.ai.local;

import java.util.*;
import org.json.*;

/** Proposed replacement for the string branch of canonical(): matches JSON.stringify (Node) exactly. */
final class ProposedCanonicalHarness {
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
                    if (c < 0x20) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    public static void main(String[] a) throws Exception {
        // Reads JSON-encoded strings (one per line) and prints jsonString(decoded) so Node can compare.
        for (String line : new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8").split("\n")) {
            if (line.isEmpty()) continue;
            String out = jsonString(new JSONArray("[" + line + "]").getString(0)); StringBuilder ascii = new StringBuilder(); for (char c : out.toCharArray()) { if (c > 0x7e) ascii.append(String.format(Locale.ROOT, "\\u%04x", (int) c)); else ascii.append(c); } System.out.println(ascii);
        }
    }
}
