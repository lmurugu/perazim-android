package org.perazimchurch.app.ai.guide;

import java.util.regex.Pattern;

/** Broad sensitivity check. Sensitive questions are never written to long-term memory. */
public final class GuideSafety {
    private static final Pattern SENSITIVE = Pattern.compile(
            "\\b(suicid\\w*|kill\\w*|end (?:my|it|this)|hurt\\w* myself|self harm\\w*|cutting|overdos\\w*|"
            + "die|dying|dead|death|abus\\w*|assault\\w*|rape|hit|hits|hitting|beat|beating|threat\\w*|"
            + "depress\\w*|anxi\\w*|panic|trauma|grie\\w*|hopeless\\w*|addict\\w*|pregnan\\w*|confess\\w*|"
            + "affair|divorce|separat\\w*|confidential|secret|kujiua|kuumiza|unyanyasaji)\\b",
            Pattern.CASE_INSENSITIVE);

    private GuideSafety() {}

    public static boolean isSensitive(String question) {
        return question != null && SENSITIVE.matcher(question).find();
    }
}
