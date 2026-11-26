package org.perazimchurch.app.ai.gaps;

import org.json.JSONException;
import org.json.JSONObject;

/** One unanswered question, already redacted. Holds no answer text, no account, no device id. */
public final class GapEntry {
    public static final String SCHEMA = "perazim-gap-v1";

    public final String key;
    public final String question;
    public final String reason;
    public final String route;
    public final String appVersion;
    public final long firstSeen;
    public final long lastSeen;
    public final int count;

    public GapEntry(String key, String question, String reason, String route, String appVersion,
                    long firstSeen, long lastSeen, int count) {
        this.key = key;
        this.question = question;
        this.reason = reason;
        this.route = route;
        this.appVersion = appVersion;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.count = count;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("schema", SCHEMA)
                    .put("question", question)
                    .put("reason", reason)
                    .put("route", route)
                    .put("app", appVersion)
                    .put("first", firstSeen)
                    .put("last", lastSeen)
                    .put("count", count);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static GapEntry fromJson(JSONObject o) {
        return new GapEntry(
                GapRedactor.normalizedKey(o.optString("question")) + "|" + o.optString("reason"),
                o.optString("question"), o.optString("reason"), o.optString("route"), o.optString("app"),
                o.optLong("first"), o.optLong("last"), o.optInt("count", 1));
    }
}
