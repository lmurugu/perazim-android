package org.perazimchurch.app.ai.guide;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether a lesson may be stored. Lessons may shape style, length, format and verse-reference habits only.
 * They may never touch doctrine, salvation, safety, crisis, privacy, scope, authority, translations or rules.
 */
public final class LessonGuard {
    public static final Set<String> KINDS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("STYLE", "LENGTH", "FORMAT", "VERSE_FORMAT", "CHECK")));
    public static final int MAX_CHARS = 160;

    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(doctrine|theolog\\w*|salvat\\w*|saved|sin|sins|sinful|grace|heaven|hell|eternal|baptism|baptis\\w*|"
            + "communion|election|predestin\\w*|apostas\\w*|lose|lost|faith alone|works|tongues|prophe\\w*|"
            + "safety|safe|crisis|suicid\\w*|kill\\w*|die|death|self harm|harm\\w*|hurt\\w*|abuse\\w*|violen\\w*|"
            + "emergenc\\w*|doctor|medic\\w*|privacy|private|confidential\\w*|secret\\w*|personal|data|"
            + "scope|refus\\w*|decline\\w*|allowed|forbidden|ignore|override|rule\\w*|instruction\\w*|"
            + "always answer|never answer|translation\\w*|esv|niv|nkjv|nasb|kjv|quot\\w*|"
            + "bishop|elder\\w*|authori\\w*|approv\\w*|ban|forbid\\w*|pay\\w*|money|tithe\\w*|offering|"
            + "bank|account|phone|mpesa|paybill|official\\w*|position\\w*|church)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DIGITS = Pattern.compile("\\d");
    private static final Pattern LINK_OR_CODE = Pattern.compile("https?:|www\\.|@|<|>|\\{|\\}|\\[|\\]|`");

    private LessonGuard() {}

    /** Returns null when the text may be stored, otherwise a short reason. */
    public static String reject(String kind, String text) {
        if (kind == null || !KINDS.contains(kind)) return "kind";
        if (text == null) return "empty";
        String raw = text.trim();
        if (raw.isEmpty()) return "empty";
        if (raw.length() > MAX_CHARS) return "length";
        String t = GuideText.clean(raw, MAX_CHARS);
        if (DIGITS.matcher(t).find()) return "digits";
        if (LINK_OR_CODE.matcher(t).find()) return "markup";
        if (FORBIDDEN.matcher(t).find()) return "restricted-topic";
        return null;
    }
}
