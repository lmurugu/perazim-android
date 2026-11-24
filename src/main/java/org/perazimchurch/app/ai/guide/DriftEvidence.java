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
 * The note carries only the approved fields (see {@link ComparativeGate#noteText}): the official teaching summary, the
 * Scripture evaluation, optional local practice, origin, and up to three approved source titles. Internal notes,
 * reviewer material and source URLs never reach this path. The provenance version is read from the knowledge base.
 */
public final class DriftEvidence {

    static final String KB_ASSET = "ai/guide/comparative/comparative-kb.json";
    static final String SOURCE_ID = ComparativeGate.NOTE_SOURCE_ID;
    static final int MAX_ANCHORS = 2;

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
            out.put(noteItem(entry, kbVersion(load(context))));
            for (int i = 0; evidence != null && i < evidence.length(); i++) {
                if (evidence.optJSONObject(i) != null) out.put(evidence.getJSONObject(i));
            }
            return out;
        } catch (Exception e) {
            return evidence;
        }
    }

    /** The knowledge base's own version, read from its JSON so provenance cannot drift from the data. */
    static String kbVersion(JSONObject kb) {
        return kb.optString("version", "unversioned");
    }

    /** The model-facing note: the approved fields. Context only; the model may not cite it. */
    static JSONObject noteItem(JSONObject entry, String kbVersion) throws Exception {
        String id = entry.optString("id");
        String text = ComparativeGate.noteText(entry);
        JSONObject item = new JSONObject();
        item.put("sourceId", SOURCE_ID);
        item.put("version", kbVersion);
        item.put("location", id);
        item.put("title", "Comparative note: " + id.replace('_', ' '));
        item.put("authorityClass", "REFERENCE");
        item.put("citationEligible", false);
        item.put("text", text);
        return item;
    }

    static JSONObject entry(Context context, String conceptId) throws Exception {
        return ComparativeGate.approvedEntry(load(context).optJSONArray("entries"), conceptId);
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
