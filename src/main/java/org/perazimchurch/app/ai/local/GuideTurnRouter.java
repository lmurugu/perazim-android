package org.perazimchurch.app.ai.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;

/**
 * The fixed-answer routing the guide dialog uses for one message, in one order (Levi, 2026-11-25).
 *
 * <p>Order: safety first (abuse, self-harm, third-party and crisis wording), then the two-step vague-exhaustion check, then
 * the built-in app answers, then {@link PreModel#decide} (approved routes, Bible references, no-go zones). The evaluation
 * harness uses {@link PreModel#decide} too, so the phone and the evaluation share one path. The legacy keyword checks in
 * {@link PerazimAiEngine#processQuery} are reached only when no route here answers.
 *
 * <p>A null {@link Turn#response} means no fixed answer: the caller goes on to the app-context, scripture and model paths.
 */
public final class GuideTurnRouter {

    /** One routed reply, or no reply when {@link #response} is null. */
    public static final class Turn {
        public final String status;
        @Nullable public final AiResponse response;
        @Nullable public final String badge;
        /** True when the conversation so far should be forgotten before this reply (safety and fixed replies). */
        public final boolean clearHistory;

        Turn(String status, @Nullable AiResponse response, @Nullable String badge, boolean clearHistory) {
            this.status = status;
            this.response = response;
            this.badge = badge;
            this.clearHistory = clearHistory;
        }
    }

    private GuideTurnRouter() {}

    /** Routes one message. The engine is the one the dialog holds; the context is the app context. */
    @NonNull
    public static Turn route(@NonNull Context c, @NonNull PerazimAiEngine engine, @Nullable String query) {
        AiResponse safetyFirst = PerazimAiEngine.safetyFirstResponse(query);
        if (safetyFirst != null) {
            return new Turn("safety", safetyFirst, "Safety guidance · on-device", true);
        }
        SoftSafety.Verdict safety = SoftSafety.judge(query);
        if (safety == SoftSafety.Verdict.CRISIS) {
            return new Turn("crisis", engine.processQuery(query), "Safety guidance · on-device", true);
        }
        if (safety == SoftSafety.Verdict.SOFT_CHECK) {
            return new Turn("soft-check", engine.processQuery(query), "Safety check · on-device", false);
        }
        AiResponse builtIn = PerazimAiEngine.builtInAppAnswer(query);
        if (builtIn != null) {
            return new Turn("built-in", builtIn, "Perazim Guide · built-in", true);
        }
        PreModel.Decision routed = PreModel.decide(c, query);
        if (routed != null) {
            AiResponse reply = routed.response != null ? routed.response : AiResponse.plainText(routed.answer, null);
            return new Turn(routed.status, reply, "Perazim Guide · built-in", true);
        }
        return new Turn("none", null, null, false);
    }
}
