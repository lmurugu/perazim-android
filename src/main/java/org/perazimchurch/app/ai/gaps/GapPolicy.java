package org.perazimchurch.app.ai.gaps;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decides whether a question may be logged at all. Crisis, self-harm, abuse, harm to others, health, privacy and other
 * sensitive messages are never logged (release-blocking; privacy must be 100%). Checked before any redaction.
 */
public final class GapPolicy {

    /** Always sensitive, whoever is speaking. */
    private static final Pattern HARD_SENSITIVE = Pattern.compile(
            "\\b(?:suicid\\w*|kill myself|end (?:my|this) life|ending my life|self[- ]?harm\\w*|hurt myself|overdos\\w*|"
                    + "abus\\w*|assault\\w*|rape\\w*|molest\\w*|beat(?:s|ing|en)? (?:me|us|my|our|him|her|them)|hit me|hits me|"
                    + "choke\\w*|threat\\w*|weapon\\w*|poison\\w*|kill (?:him|her|them|someone|my|you|us)|harm\\w*|violen\\w*|"
                    + "pregnan\\w*|miscarr\\w*|abortion|medicine|medication|drugs?|dosage|dose|diagnos\\w*|hiv|aids|cancer|"
                    + "sick\\w*|illness|disease|hospital|therap\\w*|depress\\w*|anxi\\w*|panic|trauma\\w*|addict\\w*|alcohol\\w*|"
                    + "hopeless\\w*|passwords?|passport|national id|id number|kra pin|pin number|account number|mpesa pin|"
                    + "privacy|personal data|sexual\\w*|porn\\w*|child(?:ren)? (?:abuse|molest\\w*)|minors?|arrest\\w*|"
                    + "kujiua|kuumiza|kubaka|unyanyasaji|kujidhuru)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Sensitive only when the member speaks about themselves ("my husband", "I feel"). Doctrine on these stays loggable. */
    private static final Pattern SOFT_SENSITIVE = Pattern.compile(
            "\\b(?:die[sd]?|dying|dead|death|divorc\\w*|separat\\w*|affair|adulter\\w*|cheat\\w*|grie\\w*|secret\\w*|confess\\w*|"
                    + "lonel\\w*|suffer\\w*|fear\\w*|afraid|scared|husband|wife|spouse|partner|girlfriend|boyfriend)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_PERSON = Pattern.compile(
            "\\b(?:i|i'm|im|i've|me|my|myself|we|our|us)\\b", Pattern.CASE_INSENSITIVE);

    /** Route statuses that come from safety routing. Any status with one of these words is never logged. */
    private static final String[] SAFETY_STATUS_WORDS = {
            "crisis", "safe", "soft", "overdose", "harm", "sensitive", "privacy", "health", "drug", "forgery",
            "manipulation", "human-decision", "pastoral-disclosure", "abuse"};

    /** Faith words, plus Bible book names, that make a question a faith question. */
    private static final Pattern FAITH = Pattern.compile(
            "\\b(?:god|jesus|christ|lord|holy|spirit|bible|scripture|verse|church|pastor|pray\\w*|faith|sin|sins|sinful|salvation|"
                    + "saved|saviou?r|baptis\\w*|communion|tongues?|heaven|hell|resurrect\\w*|gospel|tithe\\w*|offering|worship|kjv|nasb|"
                    + "psalms?|apostles?|prophe\\w*|angels?|satan|devil|israel|trinity|grace|mercy|sabbath|sermon|sacrament\\w*|"
                    + "eucharist|catholic\\w*|protestant\\w*|pentecostal\\w*|evangelical\\w*|muslims?|islam|quran|qur'an|mormon\\w*|"
                    + "jehovah|reincarnat\\w*|bishop|elder|kanisa|mungu|yesu|biblia|neno|genesis|exodus|leviticus|numbers|"
                    + "deuteronomy|joshua|judges|ruth|samuel|kings|chronicles|ezra|nehemiah|esther|job|proverbs|ecclesiastes|"
                    + "song of solomon|isaiah|jeremiah|lamentations|ezekiel|daniel|hosea|joel|amos|obadiah|jonah|micah|nahum|"
                    + "habakkuk|zephaniah|haggai|zechariah|malachi|matthew|mark|luke|john|acts|romans|corinthians|galatians|"
                    + "ephesians|philippians|colossians|thessalonians|timothy|titus|philemon|hebrews|james|peter|jude|revelation)\\b",
            Pattern.CASE_INSENSITIVE);

    private GapPolicy() {}

    /** True when the question and its route are free of anything sensitive. */
    public static boolean mayLog(String question, String route) {
        if (question == null || question.trim().isEmpty()) return false;
        if (HARD_SENSITIVE.matcher(question).find()) return false;
        if (SOFT_SENSITIVE.matcher(question).find() && FIRST_PERSON.matcher(question).find()) return false;
        if (route != null) {
            String r = route.toLowerCase(Locale.ROOT);
            for (String word : SAFETY_STATUS_WORDS) {
                if (r.contains(word)) return false;
            }
        }
        return true;
    }

    /** True when the question is about the Christian faith, the Bible or the church. */
    public static boolean isFaithQuestion(String question) {
        return question != null && FAITH.matcher(question).find();
    }
}
