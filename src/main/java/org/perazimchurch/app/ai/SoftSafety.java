package org.perazimchurch.app.ai;

import java.util.regex.Pattern;

/**
 * Two-step safety check for vague exhaustion (Levi, 2026-11-25; coordinator note of the same day).
 *
 * <p>"Nimechoka na maisha" and "I'm tired of life" are not treated as an immediate crisis. The guide gives a warm check
 * that asks directly whether the person is thinking of harming themselves or ending their life, with the verified crisis
 * numbers. Any yes, any explicit crisis phrase, or any plan, means or attempt in the same conversation escalates to the
 * full crisis response. Explicit crisis wording always goes straight to the full response, as before.
 *
 * <p>State is kept per conversation by {@link #reset()}; a new conversation starts with no pending check.
 */
public final class SoftSafety {

    public enum Verdict { NONE, SOFT_CHECK, CRISIS }

    static final Pattern SOFT = Pattern.compile(
            "\\b(?:nimechoka na (?:hii )?maisha|i'?m tired of (?:this |my |the )?life|i am tired of (?:this |my |the )?life"
                    + "|tired of (?:this |my )?life|tired of living)\\b", Pattern.CASE_INSENSITIVE);

    /** Answers that confirm the check: yes, ndiyo, or a clear statement of wanting or intending. */
    static final Pattern AFFIRM = Pattern.compile(
            "\\b(?:yes|yeah|yep|yup|sure|ndiyo|ndio|nataka|nina|i do|i'?m thinking)\\b", Pattern.CASE_INSENSITIVE);

    /** Plans, means or attempts: these escalate at once when a check is pending. */
    static final Pattern PLAN_OR_MEANS = Pattern.compile(
            "\\b(?:plan|attempt|tried|pills|tablets|rope|poison|gun|knife|weapon|bridge|cliff|mpango)\\b", Pattern.CASE_INSENSITIVE);

    private static volatile boolean pending = false;

    /**
     * The last message judged and its verdict. The dialog and the engine both ask about the same message, so a repeat of
     * the last message returns the same verdict without changing state. This keeps one message from being judged twice.
     */
    private static String lastQuery = null;
    private static Verdict lastVerdict = Verdict.NONE;

    private SoftSafety() {}

    /** Starts a new conversation: no check is pending and the last verdict is forgotten. */
    public static synchronized void reset() {
        pending = false;
        lastQuery = null;
        lastVerdict = Verdict.NONE;
    }

    /**
     * Judges one message in the current conversation. Crisis wording wins first. A pending check escalates on an
     * affirmative answer or on a plan, means or attempt. Vague exhaustion starts a check.
     */
    public static synchronized Verdict judge(String query) {
        if (query == null || query.trim().isEmpty()) return Verdict.NONE;
        String q = query.replace('’', '\'').replaceAll("\\s+", " ");
        if (q.equals(lastQuery)) return lastVerdict;
        Verdict verdict = step(q);
        lastQuery = q;
        lastVerdict = verdict;
        return verdict;
    }

    private static Verdict step(String q) {
        if (PerazimAiEngine.isCrisisQuery(q)) {
            pending = false;
            return Verdict.CRISIS;
        }
        if (pending && (AFFIRM.matcher(q).find() || PLAN_OR_MEANS.matcher(q).find())) {
            pending = false;
            return Verdict.CRISIS;
        }
        if (SOFT.matcher(q).find()) {
            pending = true;
            return Verdict.SOFT_CHECK;
        }
        return Verdict.NONE;
    }

    /** True while a safety check is waiting for an answer. Exposed for tests. */
    static boolean isPending() {
        return pending;
    }

    /** The warm check. It always carries the verified Kenya crisis numbers. */
    public static String checkText() {
        return "I'm sorry you're carrying so much right now. I want to ask you directly: are you thinking about harming "
                + "yourself or ending your life? Please reply to tell me whether you are safe right now. If you are in danger, "
                + "or if the answer is yes, call 999 or 112 now, or go to the nearest hospital. "
                + "Kenya support lines: 999 or 112 (police emergency and immediate danger), 922 (medical emergency, including "
                + "overdose or poisoning), 116 (child helpline), 1192 (NACADA, drug and alcohol use), Befrienders Kenya "
                + "+254 722 178 177 (not available 24 hours). Please also talk with a trusted person today, such as a family "
                + "member, your pastor or a trusted church leader.";
    }
}
