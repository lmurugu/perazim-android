package org.perazimchurch.app.ai.local;

import androidx.annotation.Nullable;

/**
 * The literal route status of a model-path reply, for the gap log (coordinator, 2026-11-25). Derived from the fixed reply
 * texts, so the gap log never matches free wording. Null when the reply is an ordinary answer or fixed route text.
 */
public final class ReplyStatus {
    /** The no-source line: no verified approved source supports the question. */
    public static final String NO_VERIFIED_SOURCE = "refused:no-verified-source";
    /** The Scripture-only reply: verse passages, with the note that a fuller explanation is being prepared. */
    public static final String SCRIPTURE_ONLY = "scripture-only";

    private ReplyStatus() {}

    @Nullable
    public static String of(@Nullable String reply) {
        if (reply == null) return null;
        String text = reply.trim();
        if (text.equals(OnDeviceAi.NO_SOURCE_MESSAGE)) return NO_VERIFIED_SOURCE;
        if (text.startsWith(OwnKnowledge.SCRIPTURE_ONLY_LEAD.trim())) return SCRIPTURE_ONLY;
        return null;
    }
}
