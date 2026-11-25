package org.perazimchurch.app.ai.local;

import java.util.regex.Pattern;

/**
 * Removes the end-of-turn markers an on-phone model can print, so none of them reaches a member (Levi, 2026-11-25).
 * Every path that shows model text uses this, including the composed replies and the own-knowledge answers.
 */
final class ModelOutput {
    private ModelOutput() {}

    private static final Pattern END_MARKERS = Pattern.compile(
            "<\\|im_end\\|>|<\\|endoftext\\|>|<\\|eot_id\\|>|<\\|end\\|>|</s>|\\[end of text\\]", Pattern.CASE_INSENSITIVE);

    /** The text with every end marker removed and the ends trimmed. Null stays null. */
    static String stripEndMarkers(String raw) {
        if (raw == null) return null;
        return END_MARKERS.matcher(raw).replaceAll("").trim();
    }
}
