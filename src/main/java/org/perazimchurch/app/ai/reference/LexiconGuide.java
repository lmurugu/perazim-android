package org.perazimchurch.app.ai.reference;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds word-meaning evidence from approved lexicons when a question names a Strong's number or a Greek or Hebrew word.
 * It runs after Scripture and reference evidence, so Scripture always comes first. With no approved lexicon, no bundled
 * database, or no such word, the input array is returned unchanged.
 */
public final class LexiconGuide {

    private static final String TAG = "LexiconGuide";
    static final int MAX_CARDS = 2;

    private LexiconGuide() {}

    public static JSONArray augment(Context context, JSONArray evidence, String question) {
        try {
            List<LexiconKeys.Key> keys = LexiconKeys.parse(question);
            if (keys.isEmpty()) return evidence;
            ReferenceApprovals approvals = ReferenceGuide.approvals(context);
            if (approvals.isEmpty()) return evidence;
            LexiconIndex index = LexiconIndex.open(context);
            if (index == null) return evidence;
            try {
                List<ReferenceHit> hits = new ArrayList<>();
                for (LexiconKeys.Key key : keys) {
                    if (hits.size() >= MAX_CARDS) break;
                    hits.addAll(index.lookup(key, approvals, 1));
                }
                return LexiconEvidence.append(evidence, hits, MAX_CARDS);
            } finally {
                index.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Lexicon evidence skipped", e);
            return evidence;
        }
    }
}
