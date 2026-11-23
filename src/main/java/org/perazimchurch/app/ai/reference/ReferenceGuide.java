package org.perazimchurch.app.ai.reference;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adds approved reference evidence to a question's evidence array. Called once per question, after the governed
 * package and scripture evidence has been selected. With no approved sources, or no bundled database, the input
 * array is returned unchanged.
 */
public final class ReferenceGuide {

    private static final String TAG = "ReferenceGuide";
    static final int MAX_REFERENCE_ITEMS = 2;
    private static final int MAX_WORDS = 4;

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "about", "after", "again", "against", "also", "because", "before", "being", "between", "could",
            "does", "doing", "found", "from", "have", "into", "just", "like", "meaning", "mean", "more", "much",
            "must", "only", "other", "over", "please", "said", "same", "should", "show", "some", "such",
            "tell", "than", "that", "their", "them", "then", "there", "these", "they", "this", "those",
            "through", "under", "until", "very", "were", "what", "when", "where", "which", "while", "with",
            "would", "your", "bible", "verse", "verses", "scripture", "passage", "explain", "define", "word",
            "words", "chapter", "read", "give", "help", "know", "does", "want"));

    private ReferenceGuide() {}

    public static JSONArray augment(Context context, JSONArray evidence, String question) {
        try {
            ReferenceApprovals approvals = approvals(context);
            if (approvals.isEmpty() || question == null || question.trim().isEmpty()) return evidence;
            ReferenceIndex index = ReferenceIndex.open(context);
            if (index == null) return evidence;
            try {
                List<ReferenceHit> hits = new ArrayList<>();
                int verse = ReferenceKeys.parseReference(question);
                if (verse > 0) hits.addAll(index.crossReferences(verse, approvals, 2));
                for (String word : words(question)) {
                    if (hits.size() >= MAX_REFERENCE_ITEMS) break;
                    hits.addAll(index.dictionary(word, approvals, 1));
                }
                return ReferenceEvidence.append(evidence, hits, MAX_REFERENCE_ITEMS);
            } finally {
                index.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Reference evidence skipped", e);
            return evidence;
        }
    }

    static List<String> words(String question) {
        List<String> out = new ArrayList<>();
        for (String raw : question.split("[^\\p{L}]+")) {
            String word = raw.toLowerCase(Locale.ROOT);
            if (word.length() < 4 || STOP.contains(word)) continue;
            if (!out.contains(word)) out.add(capitalise(word));
            if (out.size() >= MAX_WORDS) break;
        }
        return out;
    }

    private static String capitalise(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    private static ReferenceApprovals approvals(Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(ReferenceApprovals.ASSET), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line).append('\n');
            return ReferenceApprovals.parse(json.toString());
        } catch (Exception missing) {
            return ReferenceApprovals.none();
        }
    }
}
