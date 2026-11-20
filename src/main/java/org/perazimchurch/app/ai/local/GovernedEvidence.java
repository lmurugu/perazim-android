package org.perazimchurch.app.ai.local;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Decides which verified package documents may be shown to the model as evidence.
 *
 * Authority comes only from the governance manifest. Fields carried inside a document (authorityClass,
 * citationEligible, category, approvedBy and similar) are ignored. Android-free so it can be host-tested.
 */
final class GovernedEvidence {

    static final String UNAPPROVED = "UNAPPROVED";

    /** Shared with OfflineKnowledgeStore so retrieval and governed selection score terms identically. */
    static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "about", "after", "also", "and", "are", "because", "been", "before", "being", "but",
            "can", "could", "did", "does", "doing", "down", "for", "from", "had", "has", "have",
            "bible", "chapter", "how", "into", "just", "kjv", "like", "not", "of", "off", "on", "once", "only", "or", "our",
            "out", "over", "own", "please", "read", "show", "same", "should", "so", "some", "such",
            "scripture", "passage", "mean", "means", "meaning", "explain", "interpret", "interpreting", "say",
            "tell", "than", "that", "the", "their", "them", "there", "these", "they", "this", "those",
            "through", "too", "under", "until", "very", "what", "when", "where", "which", "while",
            "verse", "verses", "who", "with", "would", "you", "your"));

    private GovernedEvidence() {}

    /**
     * Returns fresh evidence objects, best match first, at most {@code documentLimit} of them.
     * Any document lacking a governed, hash-matching, non-withdrawn, non-ambiguous manifest entry is dropped.
     */
    static JSONArray select(JSONArray documents, Set<String> withdrawnKeys,
                            List<SourceGovernance.Source> governance, String question, int documentLimit)
            throws Exception {
        Map<String, SourceGovernance.Source> bySource = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (SourceGovernance.Source source : governance) {
            String key = key(source.id, source.version);
            if (bySource.put(key, source) != null) ambiguous.add(key);
        }
        List<JSONObject> candidates = new ArrayList<>();
        if (documents != null && question != null) {
            for (int i = 0; i < documents.length(); i++) {
                JSONObject evidence = evidenceFor(documents.getJSONObject(i), withdrawnKeys, bySource, ambiguous, question);
                if (evidence != null) candidates.add(evidence);
            }
        }
        candidates.sort((left, right) -> {
            try {
                return Integer.compare(overlap(right.getString("text"), question), overlap(left.getString("text"), question));
            } catch (JSONException error) { return 0; }
        });
        JSONArray selected = new JSONArray();
        int chars = 0;
        for (JSONObject evidence : candidates) {
            if (selected.length() >= documentLimit || chars >= 3600) break;
            selected.put(evidence);
            chars += Math.min(1200, evidence.optString("text").length());
        }
        return selected;
    }

    private static JSONObject evidenceFor(JSONObject document, Set<String> withdrawnKeys,
                                          Map<String, SourceGovernance.Source> bySource, Set<String> ambiguous,
                                          String question) throws Exception {
        String sourceId = document.optString("sourceId");
        String version = document.optString("version");
        String key = key(sourceId, version);
        if (withdrawnKeys != null && withdrawnKeys.contains(key)) return null;
        String text = document.getString("text");
        if (overlap(text, question) == 0) return null;
        SourceGovernance.Source entry = ambiguous.contains(key) ? null : bySource.get(key);
        String actualHash = sha256(text);
        String authority = SourceGovernance.authorityClassFor(entry, actualHash);
        if (UNAPPROVED.equals(authority) || !SourceGovernance.mayRetrieveForContext(entry)) return null;
        boolean official = SourceGovernance.OFFICIAL_AUTHORITY.equals(authority);
        JSONObject evidence = new JSONObject()
                .put("sourceId", sourceId)
                .put("version", version)
                .put("location", document.optString("location"))
                .put("title", document.optString("title"))
                .put("text", text)
                .put("contentHash", actualHash)
                .put("authorityClass", authority)
                .put("citationEligible", official);
        if (official) evidence.put("approvedBy", entry.approvedBy).put("approvedAt", entry.approvedAt);
        return evidence;
    }

    static int overlap(String text, String question) {
        int score = 0;
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String term : question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (term.length() >= 3 && !STOP_WORDS.contains(term) && normalized.contains(term)) score++;
        }
        return score;
    }

    static String key(String sourceId, String version) {
        return sourceId + "\u0000" + version;
    }

    static String sha256(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte item : digest) hex.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        return hex.toString();
    }
}
