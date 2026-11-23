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
    /** The evidence sourceId that marks a comparative note in the prompt. Notes are context only and never cited. */
    public static final String NOTE_SOURCE_ID = "perazim-comparative-kb";
    /** Budget for one model-facing note. Fields are kept whole or cut at a whole sentence, never mid-sentence. */
    public static final int MAX_NOTE_CHARS = 900;
    static final String HEADER = "Comparative note, not Perazim's position.";
    static final int MAX_SOURCE_TITLES = 3;

    private ComparativeGate() {}

    /**
     * The model-facing note, built field by field in priority order: the official teaching, the Scripture evaluation,
     * local practice (when the entry has one), origin, then source titles. Internal notes, reviewer material and URLs
     * never appear. A field that does not fit whole is cut at its last whole sentence, or dropped.
     */
    public static String noteText(JSONObject entry) {
        return noteText(entry, MAX_NOTE_CHARS);
    }

    static String noteText(JSONObject entry, int budget) {
        StringBuilder out = new StringBuilder(HEADER);
        out.append(" Category: ").append(categoryLabel(entry.optString("group"))).append('.');
        int room = budget - out.length();
        String summary = entry.optString("summary").trim();
        String scripture = entry.optString("scripture").trim();
        String[][] fields = {
                {"Official teaching", summary},
                {"Scripture evaluation", scripture},
                {"Local practice", entry.optString("practice")},
                {"Origin", entry.optString("origin")},
                {"Sources", sourceTitles(entry.optJSONArray("sources"))}
        };
        // The official summary and the Scripture evaluation both must reach the model. When both first sentences
        // fit, keep room for the Scripture first sentence so the summary's length cannot push it out.
        String scriptureLead = scripture.isEmpty() ? "" : " Scripture evaluation: " + firstSentence(scripture);
        int reserve = 0;
        if (!summary.isEmpty() && !scripture.isEmpty()
                && (" Official teaching: " + firstSentence(summary)).length() + scriptureLead.length() <= room) {
            reserve = scriptureLead.length();
        }
        for (String[] field : fields) {
            String value = field[1].trim();
            if (value.isEmpty()) continue;
            String prefix = " " + field[0] + ": ";
            String whole = prefix + value;
            int allowed = field[0].equals("Official teaching") ? room - reserve : room;
            if (whole.length() <= allowed) {
                out.append(whole);
                room -= whole.length();
            } else {
                String cut = wholeSentences(value, allowed - prefix.length());
                if (!cut.isEmpty()) {
                    out.append(prefix).append(cut);
                    room -= prefix.length() + cut.length();
                }
            }
        }
        return out.toString();
    }

    /** The first sentence of the text, ending at sentence punctuation followed by a space or the end. */
    static String firstSentence(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean stop = c == '.' || c == '!' || c == '?';
            boolean boundary = i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1));
            if (stop && boundary) return text.substring(0, i + 1);
        }
        return text;
    }

    /**
     * Returns the text if it fits within room characters; otherwise its longest prefix that ends at a sentence end
     * within the room, or "". Never returns a fragment of a sentence.
     */
    public static String wholeSentences(String text, int room) {
        if (room <= 0) return "";
        if (text.length() <= room) return text;
        int end = -1;
        for (int i = 0; i < room; i++) {
            char c = text.charAt(i);
            boolean stop = c == '.' || c == '!' || c == '?';
            boolean boundary = i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1));
            if (stop && boundary) end = i;
        }
        return end < 0 ? "" : text.substring(0, end + 1);
    }

    static String categoryLabel(String group) {
        return group == null || group.isEmpty() ? "unspecified" : group.replace('_', ' ');
    }

    static String sourceTitles(JSONArray sources) {
        if (sources == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sources.length() && i < MAX_SOURCE_TITLES; i++) {
            JSONObject source = sources.optJSONObject(i);
            String title = source == null ? "" : source.optString("title").trim();
            if (title.isEmpty()) continue;
            if (out.length() > 0) out.append("; ");
            out.append(title);
        }
        return out.toString();
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
