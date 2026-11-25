package org.perazimchurch.app.ai.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    /** The status prefix of a reply the on-phone model composed from approved text. */
    public static final String COMPOSED_PREFIX = "composed:";

    private GuideTurnRouter() {}

    /**
     * One message, end to end, as the guide dialog and the evaluation see it (Levi and the coordinator, 2026-11-25).
     *
     * <p>Safety, built-in answers and the approved routes are checked first, exactly as {@link #route}. A conversational
     * reply, a greeting or an approved pastoral reply is then composed by the on-phone model from its approved text, and every
     * sentence is verified; if the model is unavailable or no sentence passes, the approved fixed text is returned. Crisis,
     * safeguarding, no-go zones, doctrine, Scripture and the marriage clarifier are never composed.
     *
     * <p>This call blocks while the model runs. Call it on a background thread, never the UI thread.
     */
    @NonNull
    public static Turn respond(@NonNull Context c, @NonNull PerazimAiEngine engine, @Nullable String query,
                               @Nullable List<ConversationHistory.Turn> history) {
        Turn routed = route(c, engine, query);
        if (routed.response == null) return routed;
        String grounding;
        boolean requireSupport;
        String status = routed.status == null ? "" : routed.status;
        // Greetings keep their approved wording: the model's composed greeting was generic and did not name what the guide can do.
        if ("routed:approved-greeting-only".equals(status) || "routed:conversational-greeting".equals(status)) {
            return routed;
        }
        if (status.startsWith("routed:conversational-")) {
            grounding = ConversationalTurn.groundingFor(status);
            requireSupport = false;
        } else if (COMPOSABLE_PASTORAL.contains(status)) {
            grounding = routed.response.getMessageText();
            requireSupport = true;
        } else {
            return routed;
        }
        if (grounding == null || query == null) return routed;
        String previous = null;
        List<String> recent = new ArrayList<>();
        if (history != null) {
            int from = Math.max(0, history.size() - 4);
            for (int i = from; i < history.size(); i++) {
                ConversationHistory.Turn turn = history.get(i);
                recent.add("<|im_start|>" + turn.role + "\n" + turn.text + "<|im_end|>");
            }
            for (int i = history.size() - 1; i >= 0 && previous == null; i--) {
                ConversationHistory.Turn turn = history.get(i);
                if ("assistant".equals(turn.role)) previous = turn.text;
            }
        }
        ConversationComposer.Result composed = ConversationComposer.compose(
                (prompt, tokens) -> OnDeviceAi.composeText(c, prompt, tokens),
                query, grounding, previous, recent, requireSupport);
        if (!composed.composed()) return routed;
        return new Turn(COMPOSED_PREFIX + status, AiResponse.plainText(composed.text, null),
                "Perazim Guide · on-phone model", false);
    }

    /** Approved pastoral routes whose text the model may compose from. Safety-adjacent routes stay fixed. */
    private static final java.util.Set<String> COMPOSABLE_PASTORAL = new java.util.HashSet<>(java.util.Arrays.asList(
            "routed:approved-prayer-request-support",
            "routed:approved-job-loss-hopelessness",
            "routed:approved-far-from-god-support"));

    /**
     * Fixes common misspellings of doctrine keywords before routing (Antichrsit, falilng, tognues). Only these explicit
     * spellings change, so ordinary words such as "failing" or "calling" are left alone.
     */
    static String correctDoctrineSpelling(@Nullable String query) {
        if (query == null) return null;
        String q = query;
        q = Pattern.compile("\\bantichr(?!ist\\b)[a-z]*\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("antichrist");
        q = Pattern.compile("\\bfal(?!ling\\b)[a-z]{0,3}ng\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("falling");
        q = Pattern.compile("\\b(?:tognues|tounges|tongess|tonges|tongus)\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("tongues");
        q = Pattern.compile("\\b(?:tognue|tounge)\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("tongue");
        q = Pattern.compile("\\b(?:baptsim|bapstism|baptisim|baptsm)\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("baptism");
        q = Pattern.compile("\\b(?:sabath|sabbeth)\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("sabbath");
        q = Pattern.compile("\\b(?:trinty|trinitty)\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("trinity");
        // Common words in pastoral and general faith questions (round 49 gap fill, coordinator-approved).
        q = Pattern.compile("\\btihnk\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("think");
        q = Pattern.compile("\\btmies\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("times");
        q = Pattern.compile("\\bepxlain\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("explain");
        q = Pattern.compile("\\bapepars\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("appears");
        q = Pattern.compile("\\bsvaed\\b", Pattern.CASE_INSENSITIVE).matcher(q).replaceAll("saved");
        return q;
    }

    /** Routes one message. The engine is the one the dialog holds; the context is the app context. */
    @NonNull
    public static Turn route(@NonNull Context c, @NonNull PerazimAiEngine engine, @Nullable String query) {
        query = correctDoctrineSpelling(query);
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
        // Greetings, thanks, goodbyes and "what can you do" get the varied conversational replies, not the built-in intro.
        ConversationalTurn.Reply chat = ConversationalTurn.reply(query);
        if (chat != null) return new Turn(chat.status, AiResponse.plainText(chat.text, null), "Perazim Guide · built-in", true);
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
