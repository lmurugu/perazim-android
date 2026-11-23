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
 * Adds drift evidence to a question's evidence array: up to two KJV passages named in the comparative knowledge base
 * (citable, retrieved verbatim from the bundled KJV) and one comparative note (context only, never cited). The KJV
 * passages go first, because the prompt shows only the first three excerpts. Nothing is added when no concept drifts.
 * Internal notes and church names never reach this path: only summary, origin and scripture text are sent.
 */
public final class DriftEvidence {

    static final String KB_ASSET = "ai/guide/comparative/comparative-kb.json";
    static final String SOURCE_ID = "perazim-comparative-kb";
    static final String KB_VERSION = "1.0.0";
    static final int MAX_ANCHORS = 2;
    static final int MAX_NOTE_CHARS = 700;

    private static JSONObject kb;
    private static final Object LOCK = new Object();

    private DriftEvidence() {}

    public static JSONArray augment(Context context, JSONArray evidence, String question) {
        try {
            List<DriftDetector.Hit> hits = DriftDetector.detect(question);
            if (hits.isEmpty()) return evidence;
            JSONObject entry = entry(context, hits.get(0).conceptId);
            if (entry == null) return evidence;
            JSONArray out = new JSONArray();
            JSONArray anchors = entry.optJSONArray("anchors");
            int added = 0;
            for (int i = 0; anchors != null && i < anchors.length() && added < MAX_ANCHORS; i++) {
                JSONArray passage = LocalScriptureRetriever.retrieve(context, anchors.optString(i));
                if (passage.length() > 0 && passage.optJSONObject(0) != null) {
                    out.put(passage.getJSONObject(0));
                    added++;
                }
            }
            out.put(noteItem(entry));
            for (int i = 0; evidence != null && i < evidence.length(); i++) {
                if (evidence.optJSONObject(i) != null) out.put(evidence.getJSONObject(i));
            }
            return out;
        } catch (Exception e) {
            return evidence;
        }
    }

    /** The model-facing note: origin and the Scripture evaluation. Context only; the model may not cite it. */
    static JSONObject noteItem(JSONObject entry) throws Exception {
        String id = entry.optString("id");
        String text = "Comparative note, not Perazim's position. Origin: " + entry.optString("origin")
                + " Scripture: " + entry.optString("scripture");
        if (text.length() > MAX_NOTE_CHARS) text = text.substring(0, MAX_NOTE_CHARS);
        JSONObject item = new JSONObject();
        item.put("sourceId", SOURCE_ID);
        item.put("version", KB_VERSION);
        item.put("location", id);
        item.put("title", "Comparative note: " + id.replace('_', ' '));
        item.put("authorityClass", "REFERENCE");
        item.put("citationEligible", false);
        item.put("text", text);
        return item;
    }

    static JSONObject entry(Context context, String conceptId) throws Exception {
        JSONObject root = load(context);
        JSONArray entries = root.optJSONArray("entries");
        for (int i = 0; entries != null && i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            if (e != null && conceptId.equals(e.optString("id"))) return e;
        }
        return null;
    }

    private static JSONObject load(Context context) throws Exception {
        synchronized (LOCK) {
            if (kb != null) return kb;
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(KB_ASSET), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line).append('\n');
            }
            kb = new JSONObject(json.toString());
            return kb;
        }
    }
}
