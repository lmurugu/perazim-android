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

    /** Reference dictionaries and lexicons: word and name entries, not doctrine. */
    private static final java.util.regex.Pattern DICTIONARY_SOURCE = java.util.regex.Pattern.compile(
            "hitchcock|easton|strong|dictionary|lexicon|concordance|names", java.util.regex.Pattern.CASE_INSENSITIVE);

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
        // A doctrine question is answered from Scripture and the approved doctrine, never from a name or word dictionary
        // (a Hitchcock name entry answered "Mary" and a dictionary gave a wrong Nicaea date on the held-out run, 2026-11-25).
        if (PreModel.scriptureDoctrineQuestion(question)) {
            java.util.Iterator<JSONObject> dropped = candidates.iterator();
            while (dropped.hasNext()) {
                JSONObject e = dropped.next();
                if (DICTIONARY_SOURCE.matcher(e.optString("sourceId") + " " + e.optString("title")).find()) dropped.remove();
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

    /**
     * Refuses a rollback target that carries a revoked or superseded governance entry, or any document whose
     * sourceId+version is withdrawn. A null withdrawn set means the withdrawal status could not be read, so a target
     * with documents is refused (fail closed).
     */
    static void assertRollbackAllowed(List<SourceGovernance.Source> governance, JSONArray documents,
                                      Set<String> withdrawnKeys) {
        for (SourceGovernance.Source source : governance) {
            if (source.revoked || source.supersededBy != null) {
                throw new SecurityException("Rollback target contains a revoked or superseded source.");
            }
        }
        int count = documents == null ? 0 : documents.length();
        if (count == 0) return;
        if (withdrawnKeys == null) {
            throw new SecurityException("Rollback refused: withdrawal status cannot be established.");
        }
        for (int i = 0; i < count; i++) {
            JSONObject document = documents.optJSONObject(i);
            if (document == null) throw new SecurityException("Rollback target contains a malformed document.");
            if (withdrawnKeys.contains(key(document.optString("sourceId"), document.optString("version")))) {
                throw new SecurityException("Rollback target contains a withdrawn source.");
            }
        }
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
