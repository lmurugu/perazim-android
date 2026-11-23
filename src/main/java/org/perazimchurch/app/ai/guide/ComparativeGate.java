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

    private ComparativeGate() {}

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
