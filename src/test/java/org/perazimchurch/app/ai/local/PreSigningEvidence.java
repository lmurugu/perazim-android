package org.perazimchurch.app.ai.local;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PRE-SIGNING ESTIMATE. Evidence for the evaluation harness, read from the unsigned knowledge package built from the
 * approved library files (ai-eval/knowledge-package-unsigned/package.unsigned.json). It is not the product path: the
 * product reads only a signed, governed package (OfflineKnowledgeStore.retrieve). After Levi signs the package and its
 * control manifest, the product path should give the same documents for the same question.
 */
final class PreSigningEvidence {
    static final String PACKAGE = "ai-eval/knowledge-package-unsigned/package.unsigned.json";
    static final int MAX_DOCUMENTS = 3;
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "what", "does", "the", "say", "says", "about", "is", "are", "was", "were", "with", "for", "and", "that",
            "this", "from", "bible", "verse", "mean", "means", "why", "how", "who", "can", "you", "tell", "give",
            "quote", "read", "your", "their", "they", "them", "have", "has", "not", "but", "will", "should", "would",
            "could", "when", "where", "which", "there", "into", "over", "under", "christians", "christian", "church",
            "jesus", "god", "scripture", "explain", "please", "guide", "perazim"));

    private static JSONArray documents;

    private PreSigningEvidence() {}

    private static synchronized JSONArray documents() throws Exception {
        if (documents == null) {
            JSONObject envelope = new JSONObject(new String(Files.readAllBytes(new File(PACKAGE).toPath()),
                    StandardCharsets.UTF_8));
            documents = envelope.getJSONObject("payload").getJSONArray("documents");
        }
        return documents;
    }

    /** Evidence documents in the shape LocalPrompt expects. Empty when nothing in the package matches the question. */
    static JSONArray retrieve(String question) throws Exception {
        JSONArray out = new JSONArray();
        Set<String> terms = terms(question);
        if (terms.isEmpty()) return out;
        JSONArray all = documents();
        List<double[]> scored = new ArrayList<>();
        for (int i = 0; i < all.length(); i++) {
            JSONObject d = all.getJSONObject(i);
            String title = d.optString("title", "").toLowerCase(Locale.ROOT);
            String text = d.optString("text", "").toLowerCase(Locale.ROOT);
            double score = 0;
            for (String t : terms) {
                if (containsWord(title, t)) score += 3;
                if (containsWord(text, t)) score += 1;
            }
            // A dictionary headword (the text before the colon) that the question names is a strong match.
            String head = d.optString("text", "");
            int colon = head.indexOf(':');
            if (d.optString("sourceId", "").equals("hitchcock-bible-names") && colon > 0) {
                String word = head.substring(0, colon).toLowerCase(Locale.ROOT);
                if (containsWord(question.toLowerCase(Locale.ROOT), word)) score += 10;
            }
            if (score >= 3) scored.add(new double[] {score, i});
        }
        scored.sort((a, b) -> Double.compare(b[0], a[0]));
        for (int k = 0; k < Math.min(MAX_DOCUMENTS, scored.size()); k++) {
            JSONObject d = all.getJSONObject((int) scored.get(k)[1]);
            JSONObject item = new JSONObject();
            item.put("sourceId", d.getString("sourceId"));
            item.put("title", d.optString("title", d.getString("sourceId")));
            item.put("authorityClass", d.optString("authorityClass", "APPROVED"));
            item.put("version", d.optString("version", ""));
            item.put("location", d.optString("location", ""));
            item.put("text", d.getString("text"));
            out.put(item);
        }
        return out;
    }

    static Set<String> terms(String question) {
        Set<String> out = new HashSet<>();
        if (question == null) return out;
        for (String w : question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (w.length() >= 4 && !STOP.contains(w)) out.add(w);
        }
        return out;
    }

    private static boolean containsWord(String haystack, String word) {
        return java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b")
                .matcher(haystack).find();
    }
}
