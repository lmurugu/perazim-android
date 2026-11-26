package org.perazimchurch.app.ai.gaps;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes personal details from a question before it is logged. Fails closed: when anything personal is still visible
 * after the rules run, {@link #redact} returns null and the question is not logged.
 *
 * <p>Known limit: a lowercase first name, or a first name that is also a Bible name (Mary, Peter, John), cannot be
 * told apart from ordinary words here. The member sees the exact text before anything is shared (see GapLog export).
 */
public final class GapRedactor {
    static final int MAX_CHARS = 300;

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern LINK = Pattern.compile("(?i)(?:https?://|www\\.)\\S+");
    private static final Pattern PHONE = Pattern.compile("(?<!\\w)\\+?\\d[\\d ()-]{6,}\\d(?!\\w)");
    private static final Pattern ID_LIKE = Pattern.compile("(?<![\\w])[A-Za-z]{0,2}\\d{6,}[A-Za-z]{0,2}(?![\\w])");
    /** Introductions and titles that come just before a name: "my name is Grace Wambui", "Pastor Kamau". */
    private static final Pattern INTRO = Pattern.compile(
            "\\b(?i:my name is|i am|i'm|call me|this is|pastor|mzee|mama|baba|bishop|apostle|elder|deacon|mr|mrs|ms|miss|dr|brother|sister)"
                    + "\\s+((?:[A-Z][\\p{L}'’-]+)(?:\\s+[A-Z][\\p{L}'’-]+){0,2})");
    private static final Pattern WORD = Pattern.compile("\\p{Lu}[\\p{L}'’-]*|\\p{L}[\\p{L}'’-]*");
    private static final Pattern ALL_CAPS_SHORT = Pattern.compile("\\p{Lu}{2,5}");
    private static final Pattern DIGITS_LEFT = Pattern.compile("\\d{6,}");

    /** Bible book names and common faith words, so a capital letter in them is not treated as a name. */
    private static final Set<String> KEEP = new HashSet<>(Arrays.asList(
            "genesis", "exodus", "leviticus", "numbers", "deuteronomy", "joshua", "judges", "ruth", "samuel", "kings",
            "chronicles", "ezra", "nehemiah", "esther", "job", "psalm", "psalms", "proverbs", "ecclesiastes", "song",
            "isaiah", "jeremiah", "lamentations", "ezekiel", "daniel", "hosea", "joel", "amos", "obadiah", "jonah", "micah",
            "nahum", "habakkuk", "zephaniah", "haggai", "zechariah", "malachi", "matthew", "mark", "luke", "john", "acts",
            "romans", "corinthians", "galatians", "ephesians", "philippians", "colossians", "thessalonians", "timothy",
            "titus", "philemon", "hebrews", "james", "peter", "jude", "revelation",
            "god", "jesus", "christ", "lord", "holy", "spirit", "ghost", "bible", "scripture", "church", "kenya", "kenyan",
            "swahili", "sheng", "christian", "christians", "protestant", "pentecostal", "evangelical", "catholic",
            "catholics", "muslim", "muslims", "islam", "quran", "qur'an", "mormon", "jehovah", "witnesses", "trinity",
            "father", "son", "mary", "paul", "abraham", "moses", "adam", "eve", "noah", "david", "solomon", "israel",
            "gospel", "heaven", "hell", "satan", "devil", "easter", "christmas", "sunday", "sabbath", "pentecost",
            "sermon", "tithe", "pmci", "perazim", "kjv", "nasb", "pastor", "bishop", "apostle", "elder", "i"));

    private GapRedactor() {}

    /** The redacted question, or null when it cannot be made safe. Output is trimmed and capped at 300 characters. */
    public static String redact(String question) {
        if (question == null) return null;
        String s = question.replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return null;
        s = EMAIL.matcher(s).replaceAll("[email]");
        s = LINK.matcher(s).replaceAll("[link]");
        s = replaceNumbers(PHONE.matcher(s), "[phone]", 9);
        s = replaceNumbers(ID_LIKE.matcher(s), "[number]", 6);
        s = redactIntroNames(s);
        s = redactCapitalWords(s);
        if (s.length() > MAX_CHARS) s = s.substring(0, MAX_CHARS).trim();
        if (s.contains("@") || DIGITS_LEFT.matcher(s).find()) return null;
        return s;
    }

    /** A key that matches the same question written with different case, spacing or punctuation. */
    public static String normalizedKey(String redacted) {
        if (redacted == null) return "";
        return redacted.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static String replaceNumbers(Matcher m, String token, int minDigits) {
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String found = m.group();
            // Short numbers such as verse references (3:16) are kept; only long digit runs are personal.
            int digits = found.replaceAll("\\D", "").length();
            m.appendReplacement(out, Matcher.quoteReplacement(digits >= minDigits ? token : found));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String redactIntroNames(String s) {
        Matcher m = INTRO.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            String first = name.split("\\s+")[0];
            String replacement = KEEP.contains(first.toLowerCase(Locale.ROOT)) ? m.group()
                    : m.group().substring(0, m.start(1) - m.start()) + "[name]";
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Capital letters that do not start a sentence and are not Bible or faith words are treated as names. */
    private static String redactCapitalWords(String s) {
        Matcher m = WORD.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String word = m.group();
            if (!isName(s, m.start(), word)) continue;
            m.appendReplacement(out, "[name]");
        }
        m.appendTail(out);
        return out.toString();
    }

    private static boolean isName(String s, int start, String word) {
        if (!Character.isUpperCase(word.charAt(0))) return false;
        if (word.length() == 1) return false;
        if (ALL_CAPS_SHORT.matcher(word).matches()) return false;
        if (startsSentence(s, start)) return false;
        String base = word.replaceAll("(?i)['’]s$", "").toLowerCase(Locale.ROOT);
        return !KEEP.contains(base);
    }

    private static boolean startsSentence(String s, int start) {
        for (int i = start - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) continue;
            return c == '.' || c == '?' || c == '!' || c == ':' || c == '"' || c == '(';
        }
        return true;
    }
}
