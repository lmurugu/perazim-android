package org.perazimchurch.app.ai.guide;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The app's entry point to the guide. Every call is safe to make when the memory is unavailable: the guide then
 * simply runs with its locked rules and no personal notes.
 */
public final class GuideRuntime {
    private static final Object LOCK = new Object();
    private static GuideDocs docs;
    private static GuideMemory memory;
    private static boolean failed;

    private GuideRuntime() {}

    /** The prompt block for this question. Empty when the guide files or the keystore cannot be used. */
    public static GuideBlock contextFor(Context context, String question) {
        GuideMemory m = memory(context);
        if (m == null) return GuideBlock.empty();
        try {
            return m.contextFor(question, System.currentTimeMillis());
        } catch (RuntimeException e) {
            return GuideBlock.empty();
        }
    }

    /** Records the self-check outcome and the topics of the question. Sensitive questions are not kept. */
    public static void afterAnswer(Context context, String question, List<String> issues, boolean sensitive) {
        GuideMemory m = memory(context);
        if (m == null) return;
        try {
            m.afterAnswer(question, issues, sensitive, System.currentTimeMillis());
        } catch (RuntimeException ignored) {
            // Learning is best effort; an answer is never blocked by it.
        }
    }

    public static void feedback(Context context, boolean helpful, String question, String answer) {
        GuideMemory m = memory(context);
        if (m == null) return;
        m.feedback(helpful, question, answer, System.currentTimeMillis());
    }

    public static GuideMemory.Outcome correction(Context context, String question, String text) {
        GuideMemory m = memory(context);
        if (m == null) return GuideMemory.Outcome.REJECTED_NOT_STYLE;
        return m.correction(question, text, System.currentTimeMillis());
    }

    public static String describe(Context context) {
        GuideMemory m = memory(context);
        return m == null ? "The guide's memory is unavailable on this device." : m.describe(System.currentTimeMillis());
    }

    public static void setDisplayName(Context context, String name) {
        GuideMemory m = memory(context);
        if (m != null) m.setDisplayName(name);
    }

    public static void forget(Context context) {
        GuideMemory m = memory(context);
        if (m != null) m.forgetAll();
        org.perazimchurch.app.ai.gaps.GapLog.clear(context);
    }

    /** The bundled skill index (trigger rows and playbook names). Empty when the guide files cannot be read. */
    public static List<GuideDocs.SkillEntry> skillIndex(Context context) {
        synchronized (LOCK) {
            if (docs == null) {
                try {
                    docs = GuideDocs.load(new AssetGuideSource(context.getApplicationContext().getAssets()));
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
            return docs.index();
        }
    }

    /** Opens the memory once per process. Returns null when the keystore or the bundled files are unavailable. */
    static GuideMemory memory(Context context) {
        synchronized (LOCK) {
            if (memory != null || failed) return memory;
            try {
                Context app = context.getApplicationContext();
                if (docs == null) docs = GuideDocs.load(new AssetGuideSource(app.getAssets()));
                File dir = new File(app.getFilesDir(), "ai/guide-memory");
                memory = GuideMemory.open(docs, new GuideVault(dir, new AndroidGuideCipher()));
            } catch (Exception e) {
                failed = true;
                memory = null;
            }
            return memory;
        }
    }

    /** Evidence texts for the self-check, from the excerpts the model actually saw. */
    public static List<String> evidenceTexts(org.json.JSONArray shown) {
        List<String> out = new ArrayList<>();
        if (shown == null) return out;
        for (int i = 0; i < shown.length(); i++) {
            org.json.JSONObject item = shown.optJSONObject(i);
            if (item != null) out.add(item.optString("text", ""));
        }
        return out;
    }
}
