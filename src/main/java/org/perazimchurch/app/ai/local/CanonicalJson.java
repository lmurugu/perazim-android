package org.perazimchurch.app.ai.local;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    /** Canonical form used for signing: sorted keys, no whitespace, strings quoted by jsonString. Same logic as OfflineKnowledgeStore.canonical. */
    static String canonical(Object value) throws JSONException {
        if (value == JSONObject.NULL || value == null) return "null";
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Iterator<String> keys = object.keys(); keys.hasNext();) {
                String key = keys.next();
                sorted.put(key, object.get(key));
            }
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) result.append(',');
                first = false;
                result.append(jsonString(entry.getKey())).append(':').append(canonical(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < array.length(); i++) {
                if (i > 0) result.append(',');
                result.append(canonical(array.get(i)));
            }
            return result.append(']').toString();
        }
        if (value instanceof String) return jsonString((String) value);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        throw new JSONException("Unsupported package JSON value.");
    }
}
