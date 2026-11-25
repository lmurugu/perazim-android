package org.perazimchurch.app.ai.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, offline understanding step for Scripture references.
 * It corrects small spelling slips in book names ("Jhon 3 16", "psalms23")
 * and returns a confidence result. It never invents a reference: an unclear
 * name is reported as ambiguous so the guide can ask one clarifying question.
 */
public final class ScriptureReferenceNormalizer {

    private static final List<String> BOOKS = Arrays.asList(
            "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
            "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
            "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
            "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
            "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
            "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
            "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
            "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
            "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation");

    /** Common alternative spellings mapped to the canonical name. */
    private static final String[][] ALIASES = {
            {"psalm", "Psalms"}, {"song of songs", "Song of Solomon"}, {"songs", "Song of Solomon"},
            {"revelations", "Revelation"}, {"mathew", "Matthew"}, {"matt", "Matthew"}, {"phillipians", "Philippians"},
            {"ecclesiasties", "Ecclesiastes"}, {"jn", "John"}, {"1jn", "1 John"}, {"2jn", "2 John"}, {"3jn", "3 John"},
            // Swahili book names (Biblia Takatifu, Union Version): Mwanzo, Zaburi, Yohana and the rest.
            {"mwanzo", "Genesis"}, {"kutoka", "Exodus"}, {"walawi", "Leviticus"}, {"hesabu", "Numbers"},
            {"kumbukumbu", "Deuteronomy"}, {"yoshua", "Joshua"}, {"waamuzi", "Judges"}, {"ruthu", "Ruth"},
            {"1 samweli", "1 Samuel"}, {"2 samweli", "2 Samuel"}, {"1 wafalme", "1 Kings"}, {"2 wafalme", "2 Kings"},
            {"ezra", "Ezra"}, {"nehemia", "Nehemiah"}, {"esta", "Esther"}, {"ayubu", "Job"}, {"zaburi", "Psalms"},
            {"mithali", "Proverbs"}, {"mhubiri", "Ecclesiastes"}, {"wimbo ulio bora", "Song of Solomon"},
            {"isaya", "Isaiah"}, {"yeremia", "Jeremiah"}, {"maombolezo", "Lamentations"}, {"ezekieli", "Ezekiel"},
            {"danieli", "Daniel"}, {"hosea", "Hosea"}, {"yoeli", "Joel"}, {"amosi", "Amos"}, {"obadia", "Obadiah"},
            {"yona", "Jonah"}, {"mika", "Micah"}, {"nahumu", "Nahum"}, {"habakuki", "Habakkuk"}, {"sefania", "Zephaniah"},
            {"hagai", "Haggai"}, {"zekaria", "Zechariah"}, {"malaki", "Malachi"}, {"mathayo", "Matthew"}, {"marko", "Mark"},
            {"luka", "Luke"}, {"yohana", "John"}, {"matendo", "Acts"}, {"warumi", "Romans"}, {"1 wakorintho", "1 Corinthians"},
            {"2 wakorintho", "2 Corinthians"}, {"wagalatia", "Galatians"}, {"waefeso", "Ephesians"}, {"wafilipi", "Philippians"},
            {"wakolosai", "Colossians"}, {"1 wathesalonike", "1 Thessalonians"}, {"2 wathesalonike", "2 Thessalonians"},
            {"1 timotheo", "1 Timothy"}, {"2 timotheo", "2 Timothy"}, {"tito", "Titus"}, {"filemoni", "Philemon"},
            {"waebrania", "Hebrews"}, {"yakobo", "James"}, {"1 petro", "1 Peter"}, {"2 petro", "2 Peter"},
            {"1 yohana", "1 John"}, {"2 yohana", "2 John"}, {"3 yohana", "3 John"}, {"ufunuo", "Revelation"}
    };

    private static final Pattern REFERENCE = Pattern.compile(
            "(?i)(?<![\\p{L}\\p{N}])((?:[1-3]\\s*)?[\\p{L}]+(?:\\s+[\\p{L}]+){0,2})\\s*(\\d{1,3})\\s*(?:[:.]\\s*|\\s+)(\\d{1,3})(?![\\d])");

    private ScriptureReferenceNormalizer() {}

    /** Outcome of normalising a question. */
    public static final class Result {
        public final String normalized;   // e.g. "John 3:16", or null when no reference was found
        public final boolean confident;   // true when the name was matched without doubt
        public final String clarify;      // one short question when not confident, else null

        Result(String normalized, boolean confident, String clarify) {
            this.normalized = normalized;
            this.confident = confident;
            this.clarify = clarify;
        }
    }

    /** Returns null when the text holds no reference-shaped phrase at all. */
    public static Result normalize(String question) {
        if (question == null) return null;
        Matcher m = REFERENCE.matcher(question);
        if (!m.find()) return null;
        String rawName = m.group(1).trim();
        String chapter = m.group(2);
        String verse = m.group(3);

        String[] words = rawName.toLowerCase(Locale.ROOT).trim().split("\\s+");
        // Try the trailing words first, so "Show me John 3 16" resolves to John.
        for (int start = 0; start < words.length; start++) {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < words.length; i++) sb.append(words[i]);
            Result r = matchName(sb.toString(), chapter, verse);
            if (r != null) return r;
        }
        return null;
    }

    private static Result matchName(String compact, String chapter, String verse) {
        for (String[] alias : ALIASES) {
            if (alias[0].replace(" ", "").equals(compact)) return result(alias[1], chapter, verse);
        }
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        int ties = 0;
        for (String book : BOOKS) {
            String candidate = book.toLowerCase(Locale.ROOT).replace(" ", "");
            int d = distance(compact, candidate);
            int allowed = candidate.length() <= 3 ? 0 : (candidate.length() <= 8 ? 1 : 2);
            if (d > allowed) continue;
            if (d < bestDistance) {
                best = book;
                bestDistance = d;
                ties = 1;
            } else if (d == bestDistance) {
                ties++;
            }
        }
        if (best == null) return null;
        if (ties > 1) {
            return new Result(null, false,
                    "Which book do you mean? Please write the book name, for example \"John 3:16\" or \"1 John 3:16\".");
        }
        Result r = result(best, chapter, verse);
        if (bestDistance > 0) {
            return new Result(r.normalized, false,
                    "Did you mean " + r.normalized + "? Reply yes, or write the correct book name.");
        }
        return r;
    }

    private static Result result(String book, String chapter, String verse) {
        return new Result(book + " " + chapter + ":" + verse, true, null);
    }

    static int distance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + 1); // adjacent transposition, e.g. "Jhon"
                }
            }
        }
        return dp[a.length()][b.length()];
    }
}
