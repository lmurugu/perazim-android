package org.perazimchurch.app.ai.guide;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.ai.local.LocalScriptureRetriever;

/**
 * Adds drift evidence to a question's evidence array.
 *
 * Source order (Levi, 2026-11-25): the Bible comes first. A drift answer must rest on at least two distinct Scripture
 * passages (fail closed: with fewer, nothing is added). The comparative note describes another faith, church or group
 * only as a FALSE_DOCTRINE_EXAMPLE, so the guide can identify the teaching and refute it from Scripture. It is never
 * citable and never presented as valid. POLICY_RULE entries add Scripture only and no note.
 *
 * The KJV passages go first, because the prompt shows only the first three excerpts.
 */
public final class DriftEvidence {

    static final String KB_ASSET = "ai/guide/comparative/comparative-kb.json";
    static final String SOURCE_ID = "perazim-comparative-kb";
    static final String KB_VERSION = "1.5.22";
    static final int MIN_PASSAGES = 2;
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
            List<JSONObject> passages = passagesFor(context, entry);
            if (passages.size() < MIN_PASSAGES) return evidence;
            JSONArray out = new JSONArray();
            for (JSONObject p : passages) out.put(p);
            if (!"POLICY_RULE".equals(entry.optString("classification"))) out.put(noteItem(entry));
            for (int i = 0; evidence != null && i < evidence.length(); i++) {
                if (evidence.optJSONObject(i) != null) out.put(evidence.getJSONObject(i));
            }
            return out;
        } catch (Exception e) {
            return evidence;
        }
    }

    /** Up to MAX_ANCHORS distinct KJV passages for the entry. Fewer than MIN_PASSAGES means the entry is not used. */
    static List<JSONObject> passagesFor(Context context, JSONObject entry) throws Exception {
        List<JSONObject> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        JSONArray anchors = entry.optJSONArray("anchors");
        for (int i = 0; anchors != null && i < anchors.length() && out.size() < MAX_ANCHORS; i++) {
            String ref = anchors.optString(i);
            if (!seen.add(ref)) continue;
            JSONArray passage = LocalScriptureRetriever.retrieve(context, ref);
            if (passage.length() > 0 && passage.optJSONObject(0) != null) out.add(passage.getJSONObject(0));
        }
        return out;
    }

    /** The model-facing note: origin and the Scripture evaluation. Context only; the model may not cite it. */
    static JSONObject noteItem(JSONObject entry) throws Exception {
        String id = entry.optString("id");
        String text = "False-doctrine example, not a credible source and not Perazim's position. Teaching: "
                + entry.optString("summary") + " Origin: " + entry.optString("origin")
                + " Scripture: " + entry.optString("scripture");
        if (text.length() > MAX_NOTE_CHARS) text = text.substring(0, MAX_NOTE_CHARS);
        JSONObject item = new JSONObject();
        item.put("sourceId", SOURCE_ID);
        item.put("version", KB_VERSION);
        item.put("location", id);
        item.put("title", "False-doctrine example: " + id.replace('_', ' '));
        item.put("authorityClass", "FALSE_DOCTRINE_EXAMPLE");
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
