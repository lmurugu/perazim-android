package org.perazimchurch.app.community;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Account AND server scoped cache. Drafts are only sent by an explicit retry. */
public final class CommunityLocalStore {
    private final SharedPreferences preferences;
    public CommunityLocalStore(Context context, String server, String userId) {
        preferences = context.getApplicationContext().getSharedPreferences(
                "community_" + scope(server, userId), Context.MODE_PRIVATE);
    }
    public static String scope(String server, String userId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (server + "\n" + userId).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte b : digest) value.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return value.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
    public JSONObject cached(String key) {
        try { return new JSONObject(preferences.getString("cache_" + key, "{}")); }
        catch (Exception ignored) { return new JSONObject(); }
    }
    public void cache(String key, JSONObject value) {
        preferences.edit().putString("cache_" + key, value.toString()).apply();
    }
    public JSONArray drafts() {
        try { return new JSONArray(preferences.getString("drafts", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    public synchronized void saveDraft(String endpoint, JSONObject body, String label) {
        removeDraft(body.optString("id"));
        JSONArray rows = drafts();
        try {
            JSONObject row = new JSONObject();
            row.put("endpoint", endpoint); row.put("body", body); row.put("label", label);
            rows.put(row);
            preferences.edit().putString("drafts", rows.toString()).commit();
        } catch (Exception error) { throw new IllegalArgumentException(error); }
    }
    public synchronized void removeDraft(String id) {
        JSONArray old = drafts(), rows = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject row = old.optJSONObject(i);
            if (row != null && !id.equals(row.optJSONObject("body").optString("id"))) rows.put(row);
        }
        preferences.edit().putString("drafts", rows.toString()).commit();
    }
}
