package org.perazimchurch.app.ai.local;
import java.util.*; import org.json.*;
final class CanonicalHarness {
    private static String canonical(Object value) throws JSONException {
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
                result.append(JSONObject.quote(entry.getKey())).append(':').append(canonical(entry.getValue()));
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
        if (value instanceof String) return JSONObject.quote((String) value);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        throw new JSONException("Unsupported package JSON value.");
    }
    public static void main(String[] a) throws Exception {
        String[] in = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8").split("\n");
        for (String line : in) { if (line.isEmpty()) continue; System.out.println(canonical(new JSONObject(line))); }
    }
}
