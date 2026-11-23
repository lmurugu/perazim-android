package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks a drafted answer before it is shown (the self-heal step). Each check either passes, heals the text,
 * or refuses. Citations in [brackets] are never altered. Every issue is reported by code for self-learning.
 */
public final class SelfCheck {
    public static final String ISSUE_TRANSLATION = "TRANSLATION_REFUSED";
    public static final String ISSUE_VERDICT = "VERDICT_HEALED";
    public static final String ISSUE_QUOTE = "QUOTE_UNVERIFIED";
    public static final String ISSUE_REFERENCE = "VERSE_REF_INVALID";
    public static final String ISSUE_SAFETY = "SAFETY_REFERRAL_ADDED";
    public static final String ISSUE_LENGTH = "LENGTH_CUT";

    public static final String NO_VERDICT = "I cannot judge whether any person is saved or lost. Only God knows hearts. "
            + "Please talk with a trusted church leader about this, and pray together.";
    public static final String UNVERIFIED_QUOTE = "(quotation not verified in the approved sources)";
    public static final String REFERENCE_CHECK = "(reference to be checked)";
    public static final String SAFETY_LINE = " Please also talk with a trusted person about this.";
    public static final String REFUSED = "I cannot verify an answer to this from the approved sources. "
            + "Please ask the church office.";
    public static final int MAX_CHARS = 900;

    private static final Pattern TRANSLATION = Pattern.compile("\\b(ESV|NIV|NKJV)\\b");
    private static final Pattern VERDICT = Pattern.compile(
            "\\b(?:you|he|she|they|your (?:friend|father|mother|brother|sister|uncle|aunt|husband|wife|son|daughter))\\s+"
            + "(?:is|are|will|will not|won't|is not|are not|isn't|aren't|was|were)\\s+"
            + "(?:going to hell|in hell|lost|condemned|saved|not saved|unsaved|going to heaven|in heaven|damned)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTE = Pattern.compile("\"([^\"]{4,300})\"|“([^”]{4,300})”");
    private static final Pattern REFERENCE = Pattern.compile(
            "\\b(Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|1 Samuel|2 Samuel|1 Kings|2 Kings|"
            + "1 Chronicles|2 Chronicles|Ezra|Nehemiah|Esther|Job|Psalms?|Proverbs|Ecclesiastes|Song of Solomon|"
            + "Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|"
            + "Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|1 Corinthians|2 Corinthians|"
            + "Galatians|Ephesians|Philippians|Colossians|1 Thessalonians|2 Thessalonians|1 Timothy|2 Timothy|Titus|"
            + "Philemon|Hebrews|James|1 Peter|2 Peter|1 John|2 John|3 John|Jude|Revelation) (\\d{1,3})(?::\\d{1,3})?");
    private static final Map<String, Integer> CHAPTERS = new HashMap<>();
    private static final Pattern REFERRAL = Pattern.compile(
            "(?i)\\b(trusted|bishop|pastor|elder|professional|doctor|counsel\\w*|church office|helpline|emergency)\\b");

    static {
        String table = "Genesis=50;Exodus=40;Leviticus=27;Numbers=36;Deuteronomy=34;Joshua=24;Judges=21;Ruth=4;"
            + "1 Samuel=31;2 Samuel=24;1 Kings=22;2 Kings=25;1 Chronicles=29;2 Chronicles=36;Ezra=10;Nehemiah=13;"
            + "Esther=10;Job=42;Psalm=150;Proverbs=31;Ecclesiastes=12;Song of Solomon=8;Isaiah=66;Jeremiah=52;"
            + "Lamentations=5;Ezekiel=48;Daniel=12;Hosea=14;Joel=3;Amos=9;Obadiah=1;Jonah=4;Micah=7;Nahum=3;"
            + "Habakkuk=3;Zephaniah=3;Haggai=2;Zechariah=14;Malachi=4;Matthew=28;Mark=16;Luke=24;John=21;Acts=28;"
            + "Romans=16;1 Corinthians=16;2 Corinthians=13;Galatians=6;Ephesians=6;Philippians=4;Colossians=4;"
            + "1 Thessalonians=5;2 Thessalonians=3;1 Timothy=6;2 Timothy=4;Titus=3;Philemon=1;Hebrews=13;James=5;"
            + "1 Peter=5;2 Peter=3;1 John=5;2 John=1;3 John=1;Jude=1;Revelation=22";
        for (String pair : table.split(";")) {
            int eq = pair.lastIndexOf('=');
            CHAPTERS.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
        }
        CHAPTERS.put("Psalms", 150);
    }

    private SelfCheck() {}

    /** Result of checking one drafted answer. */
    public static final class Result {
        public final String answer;
        public final List<String> issues;
        public final boolean refused;

        Result(String answer, List<String> issues, boolean refused) {
            this.answer = answer;
            this.issues = Collections.unmodifiableList(issues);
            this.refused = refused;
        }
    }

    /** evidence holds the supplied source texts; sensitive means the question touched a pastoral or safety topic. */
    public static Result check(String answer, List<String> evidence, boolean sensitive) {
        List<String> issues = new ArrayList<>();
        String text = answer == null ? "" : answer.trim();
        if (text.isEmpty()) return new Result(REFUSED, Arrays.asList("EMPTY"), true);

        if (TRANSLATION.matcher(text).find()) {
            issues.add(ISSUE_TRANSLATION);
            return new Result(REFUSED, issues, true);
        }
        if (VERDICT.matcher(text).find()) {
            issues.add(ISSUE_VERDICT);
            return new Result(NO_VERDICT, issues, false);
        }

        text = checkQuotes(text, evidence, issues);
        text = checkReferences(text, issues);
        if (sensitive && !REFERRAL.matcher(text).find()) {
            text = text + SAFETY_LINE;
            issues.add(ISSUE_SAFETY);
        }
        if (text.length() > MAX_CHARS) {
            text = cutAtSentence(text, MAX_CHARS);
            issues.add(ISSUE_LENGTH);
        }
        return new Result(text, issues, false);
    }

    /** Only the bare check of quotations, for tests and for the self-check report. */
    static String checkQuotes(String text, List<String> evidence, List<String> issues) {
        StringBuilder evidenceText = new StringBuilder();
        for (String e : evidence) evidenceText.append(' ').append(GuideText.normalise(e));
        String haystack = " " + evidenceText.toString().trim() + " ";
        Matcher m = QUOTE.matcher(text);
        StringBuffer out = new StringBuffer();
        boolean changed = false;
        while (m.find()) {
            String quoted = m.group(1) != null ? m.group(1) : m.group(2);
            String norm = GuideText.normalise(quoted);
            boolean verified = norm.split(" ").length < 4 || haystack.contains(" " + norm + " ");
            if (verified) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(out, Matcher.quoteReplacement(UNVERIFIED_QUOTE));
                changed = true;
            }
        }
        m.appendTail(out);
        if (changed) issues.add(ISSUE_QUOTE);
        return out.toString();
    }

    private static String checkReferences(String text, List<String> issues) {
        Matcher m = REFERENCE.matcher(text);
        StringBuffer out = new StringBuffer();
        boolean changed = false;
        while (m.find()) {
            if (insideBrackets(text, m.start())) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
                continue;
            }
            Integer max = CHAPTERS.get(m.group(1));
            int chapter = Integer.parseInt(m.group(2));
            if (max == null || chapter < 1 || chapter > max) {
                m.appendReplacement(out, Matcher.quoteReplacement(REFERENCE_CHECK));
                changed = true;
            } else {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
            }
        }
        m.appendTail(out);
        if (changed) issues.add(ISSUE_REFERENCE);
        return out.toString();
    }

    private static boolean insideBrackets(String text, int index) {
        int open = text.lastIndexOf('[', index);
        int close = text.lastIndexOf(']', index);
        return open >= 0 && open > close;
    }

    private static String cutAtSentence(String text, int max) {
        int cut = Math.max(text.lastIndexOf(". ", max), text.lastIndexOf("? ", max));
        return cut > max / 2 ? text.substring(0, cut + 1) : text.substring(0, max);
    }
}
