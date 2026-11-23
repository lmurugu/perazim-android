package org.perazimchurch.app.ai.reference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The reference sources Levi has approved for the on-phone guide, read from assets/reference/approvals.json.
 *
 * Fail closed: a missing, unreadable or malformed file approves nothing, and an entry that is not a plain source
 * id is ignored. Approval is recorded in git and reviewed in a pull request. Presence of data is never approval.
 */
public final class ReferenceApprovals {

    static final String ASSET = "reference/approvals.json";
    private static final Pattern SOURCE_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    private final Set<String> approved;

    private ReferenceApprovals(Set<String> approved) {
        this.approved = Collections.unmodifiableSet(approved);
    }

    public static ReferenceApprovals none() {
        return new ReferenceApprovals(new LinkedHashSet<String>());
    }

    /** Parses the approvals file. Any structural problem approves nothing. */
    public static ReferenceApprovals parse(String json) {
        if (json == null || json.trim().isEmpty()) return none();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray ids = root.optJSONArray("approvedSourceIds");
            if (ids == null) return none();
            Set<String> out = new LinkedHashSet<>();
            for (int i = 0; i < ids.length(); i++) {
                Object value = ids.opt(i);
                if (value instanceof String && SOURCE_ID.matcher((String) value).matches()) {
                    out.add((String) value);
                }
            }
            return new ReferenceApprovals(out);
        } catch (JSONException malformed) {
            return none();
        }
    }

    public boolean isApproved(String sourceId) {
        return sourceId != null && approved.contains(sourceId);
    }

    public boolean isEmpty() {
        return approved.isEmpty();
    }

    public Set<String> ids() {
        return approved;
    }
}
