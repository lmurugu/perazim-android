package org.perazimchurch.app.ai.local;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-closed source governance for the offline knowledge collection.
 *
 * A manifest is trusted only after the existing signed-package verification has accepted it. This class does not
 * verify signatures. It decides, per source, whether the source may carry official authority. Any missing,
 * invalid or inconsistent field results in the source being treated as NOT approved.
 */
public final class SourceGovernance {

    public static final String CATEGORY_CHURCH_APPROVED = "CHURCH_APPROVED";
    public static final String CATEGORY_GENERAL_THEOLOGY = "GENERAL_THEOLOGY";
    public static final String CATEGORY_HISTORICAL = "HISTORICAL";
    public static final String CATEGORY_UNAPPROVED = "UNAPPROVED";
    public static final String CATEGORY_EXCLUDED = "EXCLUDED";

    /** The only authority class that may support an official Perazim claim (see PerazimAiEngine). */
    public static final String OFFICIAL_AUTHORITY = "APPROVED_OFFICIAL";

    private SourceGovernance() {}

    public static final class Source {
        public final String id;
        public final String version;
        public final String category;
        public final String approvedBy;
        public final String approvedAt;
        public final String scope;
        public final String sha256;
        public final String supersededBy;
        public final boolean revoked;

        Source(String id, String version, String category, String approvedBy, String approvedAt,
               String scope, String sha256, String supersededBy, boolean revoked) {
            this.id = id; this.version = version; this.category = category; this.approvedBy = approvedBy;
            this.approvedAt = approvedAt; this.scope = scope; this.sha256 = sha256;
            this.supersededBy = supersededBy; this.revoked = revoked;
        }
    }

    /** Parses a verified manifest. Malformed entries are dropped, which makes them unavailable as sources. */
    public static List<Source> parse(JSONArray manifest) {
        List<Source> out = new ArrayList<>();
        if (manifest == null) return out;
        for (int i = 0; i < manifest.length(); i++) {
            JSONObject o = manifest.optJSONObject(i);
            if (o == null) continue;
            String id = blankToNull(o.optString("id", null));
            String version = blankToNull(o.optString("version", null));
            if (id == null || version == null) continue;
            out.add(new Source(id, version,
                    blankToNull(o.optString("category", null)),
                    blankToNull(o.optString("approvedBy", null)),
                    blankToNull(o.optString("approvedAt", null)),
                    blankToNull(o.optString("scope", null)),
                    blankToNull(o.optString("sha256", null)),
                    blankToNull(o.optString("supersededBy", null)),
                    o.optBoolean("revoked", false)));
        }
        return out;
    }

    /**
     * Official authority requires all of: category CHURCH_APPROVED, a named approver, an approval date, a scope,
     * not revoked, not superseded, and a content hash that matches the installed content.
     */
    public static boolean isOfficiallyApproved(Source s, String installedSha256) {
        if (s == null) return false;
        if (!CATEGORY_CHURCH_APPROVED.equals(s.category)) return false;
        if (s.approvedBy == null || s.approvedAt == null || s.scope == null) return false;
        if (s.revoked || s.supersededBy != null) return false;
        if (s.sha256 == null || installedSha256 == null) return false;
        return s.sha256.equalsIgnoreCase(installedSha256);
    }

    /** Authority class to show the model and validator. Anything not officially approved never gets official authority. */
    public static String authorityClassFor(Source s, String installedSha256) {
        if (isOfficiallyApproved(s, installedSha256)) return OFFICIAL_AUTHORITY;
        if (s != null && CATEGORY_GENERAL_THEOLOGY.equals(s.category)) return "REFERENCE";
        if (s != null && CATEGORY_HISTORICAL.equals(s.category)) return "HISTORICAL";
        return "UNAPPROVED";
    }

    /** Historical and unapproved material may be retrieved for context but must never establish current policy. */
    public static boolean mayRetrieveForContext(Source s) {
        if (s == null || s.revoked) return false;
        return !CATEGORY_EXCLUDED.equals(s.category) && !CATEGORY_UNAPPROVED.equals(s.category);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() || "null".equals(t) ? null : t;
    }
}
