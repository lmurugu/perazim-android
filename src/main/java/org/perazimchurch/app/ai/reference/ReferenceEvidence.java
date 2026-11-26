package org.perazimchurch.app.ai.reference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Turns reference hits into evidence objects in the same shape the guide already uses for package documents
 * and scripture passages. Every item is labelled as reference material, carries its author and licence, and
 * never claims official authority.
 */
public final class ReferenceEvidence {

    static final String AUTHORITY_CLASS = "REFERENCE";
    static final String NOTICE = "Reference material, not Perazim's position.";
    static final int MAX_TEXT_CHARS = 600;
    private static final int MAX_LOCATION_CHARS = 80;

    private ReferenceEvidence() {}

    /** Returns a new evidence object for one hit. */
    public static JSONObject toEvidence(ReferenceHit hit) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("sourceId", "REF-" + safeId(hit.sourceId));
        item.put("title", clip(hit.title, 120));
        item.put("version", "1");
        item.put("location", safeLocation(hit.headword));
        item.put("authorityClass", AUTHORITY_CLASS);
        item.put("author", clip(hit.author, 160));
        item.put("licence", clip(hit.licence, 160));
        item.put("theologyTag", clip(hit.theologyTag, 80));
        item.put("notice", NOTICE);
        String text = hit.text == null ? "" : hit.text.trim();
        if (ReferenceHit.KIND_CROSS_REFERENCE.equals(hit.kind)) {
            text = "Cross-reference from " + hit.headword + " to " + text + " (" + hit.votes + " votes).";
        } else if (ReferenceHit.KIND_COMMENTARY.equals(hit.kind)) {
            text = commentaryText(hit.author, hit.theologyTag, hit.headword, text);
        }
        item.put("text", clip(text, MAX_TEXT_CHARS));
        return item;
    }

    /**
     * Appends up to {@code maxAdded} hits to a copy of {@code evidence}. Hits whose source is not approved must
     * already have been filtered out by the caller. The original array is never changed.
     */
    public static JSONArray append(JSONArray evidence, List<ReferenceHit> hits, int maxAdded) throws JSONException {
        JSONArray out = new JSONArray();
        if (evidence != null) {
            for (int i = 0; i < evidence.length(); i++) out.put(evidence.get(i));
        }
        int added = 0;
        if (hits != null) {
            for (ReferenceHit hit : hits) {
                if (added >= maxAdded) break;
                if (hit == null || hit.text == null || hit.text.trim().isEmpty()) continue;
                out.put(toEvidence(hit));
                added++;
            }
        }
        return out;
    }

    /** Tradition label for a commentary's theology tag. Empty when the tag is not a known commentary tradition. */
    static String tradition(String theologyTag) {
        if ("PURITAN_REFORMED_COMMENTARY".equals(theologyTag)) return "Puritan, Reformed";
        if ("ARMINIAN_WESLEYAN_COMMENTARY".equals(theologyTag)) return "Arminian, Wesleyan";
        return "";
    }

    /** Puts the author and tradition in front of every commentary excerpt, so the model cannot present it unlabelled. */
    static String commentaryText(String author, String theologyTag, String headword, String body) {
        String name = author == null || author.trim().isEmpty() ? "Commentary" : author.trim();
        String tradition = tradition(theologyTag);
        String label = tradition.isEmpty() ? name : name + " (" + tradition + ")";
        return label + ", commentary on " + headword + ": " + body;
    }

    static String safeId(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[^A-Za-z0-9-]", "-");
        return clip(cleaned.toUpperCase(Locale.ROOT), 60);
    }

    /** Removes characters the citation grammar reserves ("]", "@" and ":") and bounds the length. */
    static String safeLocation(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[\\]\\[@:]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) cleaned = "entry";
        return clip(cleaned, MAX_LOCATION_CHARS);
    }

    private static String clip(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
