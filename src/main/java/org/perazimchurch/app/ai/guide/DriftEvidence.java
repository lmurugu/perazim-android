package org.perazimchurch.app.ai.guide;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.ai.local.LocalScriptureRetriever;

/**
 * Adds drift evidence to a question's evidence array: up to two KJV passages chosen for the drift concept (citable,
 * retrieved verbatim from the bundled KJV). The KJV passages go first, because the prompt shows only the first three
 * excerpts. Nothing is added when no concept drifts.
 *
 * Since 2026-11-25 no description of another faith, church or group is sent to the model. The comparative knowledge
 * base was removed: only Bible references remain, in assets/ai/guide/drift/scripture-anchors.json. The model is not
 * given the other tradition's wording, origin or sources.
 */
public final class DriftEvidence {

    static final String ANCHORS_ASSET = "ai/guide/drift/scripture-anchors.json";
    static final int MAX_ANCHORS = 2;

    private static JSONObject anchors;
    private static final Object LOCK = new Object();

    private DriftEvidence() {}

    public static JSONArray augment(Context context, JSONArray evidence, String question) {
        try {
            List<DriftDetector.Hit> hits = DriftDetector.detect(question);
            if (hits.isEmpty()) return evidence;
            JSONArray conceptAnchors = anchorsFor(context, hits.get(0).conceptId);
            if (conceptAnchors == null || conceptAnchors.length() == 0) return evidence;
            JSONArray out = new JSONArray();
            int added = 0;
            for (int i = 0; i < conceptAnchors.length() && added < MAX_ANCHORS; i++) {
                JSONArray passage = LocalScriptureRetriever.retrieve(context, conceptAnchors.optString(i));
                if (passage.length() > 0 && passage.optJSONObject(0) != null) {
                    out.put(passage.getJSONObject(0));
                    added++;
                }
            }
            for (int i = 0; evidence != null && i < evidence.length(); i++) {
                if (evidence.optJSONObject(i) != null) out.put(evidence.getJSONObject(i));
            }
            return out;
        } catch (Exception e) {
            return evidence;
        }
    }

    /** The Bible references for one drift concept, or null when the concept has none. */
    static JSONArray anchorsFor(Context context, String conceptId) throws Exception {
        JSONObject root = load(context);
        JSONArray entries = root.optJSONArray("entries");
        for (int i = 0; entries != null && i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            if (e != null && conceptId.equals(e.optString("id"))) return e.optJSONArray("anchors");
        }
        return null;
    }

    private static JSONObject load(Context context) throws Exception {
        synchronized (LOCK) {
            if (anchors != null) return anchors;
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(ANCHORS_ASSET), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line).append('\n');
            }
            anchors = new JSONObject(json.toString());
            return anchors;
        }
    }
}
