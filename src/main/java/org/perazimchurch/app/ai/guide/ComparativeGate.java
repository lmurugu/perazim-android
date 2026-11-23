package org.perazimchurch.app.ai.guide;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The fail-closed approval gate for the comparative knowledge base. Pure Java, so host tests can check it.
 * Only an entry whose status is exactly APPROVED_LEVI may reach the model. Draft and pending entries are excluded,
 * not merely labelled.
 */
public final class ComparativeGate {

    public static final String APPROVED_STATUS = "APPROVED_LEVI";
    static final int MAX_NOTE_CHARS = 700;

    private ComparativeGate() {}

    /**
     * The model-facing note: origin and Scripture only. Internal notes, sources and church names never reach the model.
     */
    public static String noteText(JSONObject entry) {
        String text = "Comparative note, not Perazim's position. Origin: " + entry.optString("origin")
                + " Scripture: " + entry.optString("scripture");
        return text.length() > MAX_NOTE_CHARS ? text.substring(0, MAX_NOTE_CHARS) : text;
    }

    /** Returns the entry for the concept only when its status is exactly APPROVED_LEVI; otherwise null. */
    public static JSONObject approvedEntry(JSONArray entries, String conceptId) {
        if (conceptId == null) return null;
        for (int i = 0; entries != null && i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            if (e != null && conceptId.equals(e.optString("id")) && APPROVED_STATUS.equals(e.optString("status"))) {
                return e;
            }
        }
        return null;
    }
}
