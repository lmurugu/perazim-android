package org.perazimchurch.app.ai.gaps;

import android.content.Context;

import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.ai.guide.AndroidGuideCipher;
import org.perazimchurch.app.ai.guide.GuideVault;

import java.io.File;
import java.util.List;

/**
 * Entry points for the unanswered-question log. Each one checks the policy, redacts, and stores. None of them throws:
 * a logging failure must never break a chat reply. Call these off the UI thread; they write to the encrypted vault.
 */
public final class GapLog {
    private static final Object LOCK = new Object();
    private static GapLogStore store;
    private static boolean failed;

    private GapLog() {}

    /** Records a routed turn that gave no real answer. {@code status} is the router's status, for example routed:no-go:x. */
    public static void observeRouted(Context c, String question, String status) {
        String reason = org.perazimchurch.app.ai.local.GapSignals.reasonForRouted(status);
        if (reason != null) record(c, question, reason, status);
    }

    /** Records a model answer that was only a Scripture-only fallback, or a no-source refusal sent as an answer. */
    public static void observeModelAnswer(Context c, String question, String answer) {
        String reason = org.perazimchurch.app.ai.local.GapSignals.reasonForAnswer(answer);
        if (reason != null) record(c, question, reason, "on-phone-model");
    }

    /** Records a model error. System errors (integrity, download, cancel) are not gaps and are skipped. */
    public static void observeModelError(Context c, String question, String message) {
        String reason = org.perazimchurch.app.ai.local.GapSignals.reasonForModelError(message);
        if (reason != null) record(c, question, reason, "on-phone-model");
    }

    /** The member tapped "Not helpful" under an answer. The answer text itself is never logged. */
    public static void observeMemberFlag(Context c, String question) {
        record(c, question, GapReason.MEMBER_NOT_HELPFUL, "member-flag");
    }

    public static List<GapEntry> list(Context c) {
        GapLogStore s = store(c);
        return s == null ? java.util.Collections.<GapEntry>emptyList() : s.all();
    }

    public static boolean clear(Context c) {
        GapLogStore s = store(c);
        return s != null && s.clear();
    }

    /** The exact text the member may send. Nothing else leaves the device. */
    public static String exportText(List<GapEntry> entries) {
        return GapLogStore.toJsonLines(entries);
    }

    /** The one place a question enters the log: policy, then faith check, then redaction, then storage. */
    static boolean recordWith(GapLogStore target, String question, String reason, String route, String appVersion, long now) {
        if (target == null) return false;
        if (!GapPolicy.mayLog(question, route)) return false;
        if (!GapPolicy.isFaithQuestion(question)) return false;
        String redacted = GapRedactor.redact(question);
        if (redacted == null) return false;
        return target.record(redacted, reason, route, appVersion, System.currentTimeMillis());
    }

    private static void record(Context c, String question, String reason, String route) {
        try {
            recordWith(store(c), question, reason, route, appVersion(), System.currentTimeMillis());
        } catch (RuntimeException ignored) {
            // Logging never breaks a reply.
        }
    }

    static String appVersion() {
        return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    }

    static GapLogStore store(Context c) {
        synchronized (LOCK) {
            if (store != null || failed) return store;
            try {
                File dir = new File(c.getApplicationContext().getFilesDir(), "ai/gap-log");
                store = GapLogStore.open(new GuideVault(dir, new AndroidGuideCipher()));
            } catch (RuntimeException e) {
                failed = true;
                store = null;
            }
            return store;
        }
    }
}
