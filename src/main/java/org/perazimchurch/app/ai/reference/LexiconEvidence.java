package org.perazimchurch.app.ai.reference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Evidence cards for word meanings from approved lexicons. Each card is at most 300 characters, names its source and
 * licence, and is labelled as lexical evidence. A word meaning is never doctrine and is never quoted as Scripture.
 */
public final class LexiconEvidence {

    static final String AUTHORITY_CLASS = "LEXICAL_REFERENCE";
    static final String NOTICE = "Word meaning from a lexicon, not Perazim's position. Scripture decides doctrine.";
    static final int MAX_TEXT_CHARS = 300;
    private static final int MAX_LICENCE_CHARS = 160;

    private LexiconEvidence() {}

    /** Returns a new evidence object for one lexicon hit. */
    public static JSONObject toEvidence(ReferenceHit hit) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("sourceId", "LEX-" + ReferenceEvidence.safeId(hit.sourceId));
        item.put("title", clip(hit.title, 120));
        item.put("version", "1");
        item.put("location", ReferenceEvidence.safeLocation(hit.headword));
        item.put("authorityClass", AUTHORITY_CLASS);
        item.put("author", clip(hit.author, 160));
        item.put("licence", clip(hit.licence, MAX_LICENCE_CHARS));
        item.put("notice", NOTICE);
        item.put("text", card(hit.headword, hit.text));
        return item;
    }

    /** One card: the word, then its meaning, cut at a word boundary so it never exceeds the limit. */
    static String card(String headword, String body) {
        String word = headword == null ? "" : headword.trim();
        String meaning = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        return clipAtWord(("Word: " + word + ". " + meaning).trim(), MAX_TEXT_CHARS);
    }

    /** Appends up to {@code maxAdded} hits to a copy of {@code evidence}. The original array is never changed. */
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

    static String clipAtWord(String value, int max) {
        if (value.length() <= max) return value;
        int cut = value.lastIndexOf(' ', max - 3);
        if (cut < max / 2) cut = max - 3;
        return value.substring(0, cut).trim() + "...";
    }

    private static String clip(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
