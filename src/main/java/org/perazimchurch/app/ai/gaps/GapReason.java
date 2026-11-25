package org.perazimchurch.app.ai.gaps;

/**
 * Why a faith question got no real answer. Codes are stored and exported, so they stay short and stable.
 * Each code has a plain-language label for the member's own list.
 */
public final class GapReason {
    /** No checked source supports the question: the "no verified source" refusal, or a not-found verse lookup. */
    public static final String NO_VERIFIED_SOURCE = "no-verified-source";
    /** Only the Scripture-only fallback could be given (a doctrine question with no checked explanation). */
    public static final String SCRIPTURE_ONLY = "scripture-only";
    /** A named no-go zone declined a faith question. */
    public static final String NO_GO_DECLINE = "no-go-decline";
    /** The scope gate declined a faith question. */
    public static final String SCOPE_DECLINE = "scope-decline";
    /** A model answer was rejected by the citation, claim or self-check rules. */
    public static final String MODEL_REJECTED = "model-output-rejected";
    /** The member tapped "Not helpful" under an answer. */
    public static final String MEMBER_NOT_HELPFUL = "member-not-helpful";

    private GapReason() {}

    public static String label(String code) {
        if (NO_VERIFIED_SOURCE.equals(code)) return "No checked source found";
        if (SCRIPTURE_ONLY.equals(code)) return "Only Scripture passages could be given";
        if (NO_GO_DECLINE.equals(code)) return "Declined: outside what the guide teaches";
        if (SCOPE_DECLINE.equals(code)) return "Declined by the scope rules";
        if (MODEL_REJECTED.equals(code)) return "Answer did not pass the checks";
        if (MEMBER_NOT_HELPFUL.equals(code)) return "Marked not helpful";
        return "Other";
    }
}
