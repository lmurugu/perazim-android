package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Verse, chapter, phrase and word lookup, checked against the bundled KJV file read independently (Levi, 2026-11-25):
 * every verse quoted must be the file's text, every word count must match a count over the file, and doctrine questions
 * must reach their approved route. Deterministic, no model.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LookupRegressionTest {

    /** The KJV file, read directly: book number, chapter, verse and text per line. */
    private static final Map<String, String> VERSES = new HashMap<>();
    private static final Map<Integer, String> BOOK_NAMES = new HashMap<>();
    private static final Map<String, Integer> BOOK_NUMBERS = new HashMap<>();

    static {
        try {
            org.json.JSONArray manifest = new org.json.JSONArray(new String(java.nio.file.Files.readAllBytes(
                    new File("src/main/assets/bible/kjv_manifest.json").toPath()), StandardCharsets.UTF_8));
            for (int i = 0; i < manifest.length(); i++) {
                org.json.JSONObject b = manifest.getJSONObject(i);
                BOOK_NAMES.put(b.getInt("bookNumber"), b.getString("name"));
                BOOK_NUMBERS.put(b.getString("name").toLowerCase(Locale.ROOT), b.getInt("bookNumber"));
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream("src/main/assets/bible/text/KJV.tsv.gz")), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] f = line.split("\t", 4);
                    if (f.length < 4) continue;
                    VERSES.put(f[0] + ":" + f[1] + ":" + f[2], f[3]);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String route(Context c, String question) {
        SoftSafety.reset();
        GuideTurnRouter.Turn turn = GuideTurnRouter.route(c, new PerazimAiEngine(c), question);
        return turn.response == null ? "" : turn.response.getMessageText();
    }

    private static String kjv(String ref, int chapter, int verse) {
        Integer book = BOOK_NUMBERS.get(ref.toLowerCase(Locale.ROOT));
        return book == null ? null : VERSES.get(book + ":" + chapter + ":" + verse);
    }

    @Test
    public void verseReferencesQuoteTheFileText() {
        Context c = ApplicationProvider.getApplicationContext();
        String[][] refs = {
            {"John", "3", "16"}, {"Romans", "8", "28"}, {"Psalms", "23", "1"}, {"Genesis", "1", "1"}, {"Matthew", "5", "3"},
            {"1 Corinthians", "13", "4"}, {"Revelation", "21", "4"}, {"Hebrews", "11", "1"}, {"Isaiah", "53", "5"},
            {"Ephesians", "2", "8"}, {"Philippians", "4", "6"}, {"James", "1", "5"}, {"Titus", "2", "11"}, {"Revelation", "22", "21"}
        };
        String[] templates = {"{r}", "What does {r} say?", "Show me {r}", "Read {r} to me", "Where can I find {r}?", "Find {r}", "Tell me {r}"};
        StringBuilder failures = new StringBuilder();
        for (String[] ref : refs) {
            String label = ref[0] + " " + ref[1] + ":" + ref[2];
            String text = kjv(ref[0], Integer.parseInt(ref[1]), Integer.parseInt(ref[2]));
            for (String t : templates) {
                String q = t.replace("{r}", label);
                String out = route(c, q);
                if (text == null || !out.contains(text)) failures.append(q).append(" -> ").append(out, 0, Math.min(120, out.length())).append('\n');
            }
        }
        assertTrue("Verse lookup failures:\n" + failures, failures.length() == 0);
    }

    @Test
    public void chapterReferencesQuoteTheFirstVerse() {
        Context c = ApplicationProvider.getApplicationContext();
        StringBuilder failures = new StringBuilder();
        String[][] chapters = {{"Psalms", "23"}, {"Psalm 23", "x"}};
        String[] qs = {"Give me Psalm 23", "Where is Psalm 23?", "Show me Psalms 23", "Psalm 23"};
        String first = kjv("Psalms", 23, 1);
        for (String q : qs) {
            String out = route(c, q);
            if (!out.contains(first)) failures.append(q).append(" -> ").append(out, 0, Math.min(120, out.length())).append('\n');
        }
        assertTrue("Chapter lookup failures:\n" + failures, failures.length() == 0);
    }

    @Test
    public void wordCountsMatchTheFile() {
        Context c = ApplicationProvider.getApplicationContext();
        String[][] cases = {
            {"grace", ""}, {"faith", "Romans"}, {"love", "John"}, {"holy", "Psalms"}, {"mercy", ""}, {"hope", ""}, {"angel", ""}
        };
        String[] templates = {"Where is the word '{w}' in the Bible?", "How many times does '{w}' appear in {b}?", "word search: {w}",
            "Count the word {w} in {b}", "Find the word '{w}' in the Bible"};
        StringBuilder failures = new StringBuilder();
        for (String[] cs : cases) {
            String word = cs[0];
            String book = cs[1];
            for (String t : templates) {
                // Templates with "{b}" count within the book; the others count the whole Bible.
                String scope = t.contains("{b}") ? book : "";
                if (t.contains("{b}") && book.isEmpty()) continue;
                int[] counts = countWord(word, scope);
                String q = t.replace("{w}", word).replace("{b}", book);
                String out = route(c, q);
                String expect = "appears " + counts[0] + (counts[0] == 1 ? " time" : " times");
                if (!out.contains(expect)) failures.append(q).append(" expected '").append(expect).append("' -> ")
                        .append(out, 0, Math.min(120, out.length())).append('\n');
            }
        }
        assertTrue("Word count failures:\n" + failures, failures.length() == 0);
    }

    /** Occurrences and verse count of the exact word, in one book or the whole Bible, over the file. */
    private static int[] countWord(String word, String book) {
        int occurrences = 0;
        int verses = 0;
        int bookNumber = book.isEmpty() ? -1 : BOOK_NUMBERS.get(book.toLowerCase(Locale.ROOT));
        for (Map.Entry<String, String> e : VERSES.entrySet()) {
            String[] k = e.getKey().split(":");
            if (bookNumber >= 0 && Integer.parseInt(k[0]) != bookNumber) continue;
            int n = 0;
            for (String token : e.getValue().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().split(" ")) {
                if (token.equals(word)) n++;
            }
            if (n > 0) { occurrences += n; verses++; }
        }
        return new int[] {occurrences, verses};
    }

    @Test
    public void phraseAndVerseSayingSearch() {
        Context c = ApplicationProvider.getApplicationContext();
        String[][] cases = {
            {"Who said 'Jesus wept'?", "Jesus wept"},
            {"Find the phrase 'faith without works'", "faith without works is dead"},
            {"Which verse says God is love?", "God is love"},
            {"Where is the phrase 'God is love'?", "God is love"},
            {"Where in the Bible is 'For God so loved the world'?", "For God so loved the world"},
            {"Where is Jesus wept", "Jesus wept"},
            {"who said jesus wept", "Jesus wept"},
            {"Find faith without works", "faith without works is dead"},
            {"Where is the verse about the lilies?", "lilies"}
        };
        StringBuilder failures = new StringBuilder();
        for (String[] cs : cases) {
            String out = route(c, cs[0]);
            if (!out.toLowerCase(Locale.ROOT).contains(cs[1].toLowerCase(Locale.ROOT))) {
                failures.append(cs[0]).append(" -> ").append(out, 0, Math.min(120, out.length())).append('\n');
            }
        }
        assertTrue("Phrase search failures:\n" + failures, failures.length() == 0);
    }

    @Test
    public void shortLookupNeverGetsTheTooLongReply() {
        Context c = ApplicationProvider.getApplicationContext();
        for (String q : new String[] {"Where is Jesus wept", "hello", "what can you do", "Give me Psalm 23"}) {
            String out = route(c, q);
            assertTrue(q + " -> " + out, !out.contains("too long"));
        }
    }

    @Test
    public void greetingAndCapabilityNameWhatTheGuideCanDo() {
        Context c = ApplicationProvider.getApplicationContext();
        String hello = route(c, "hello");
        assertTrue(hello, hello.contains("verse") && hello.contains("word"));
        String what = route(c, "what can you do");
        assertTrue(what, what.contains("verse") && what.contains("word") && what.contains("pray"));
    }

    @Test
    public void doctrineQuestionsReachApprovedRoutes() {
        Context c = ApplicationProvider.getApplicationContext();
        String[][] cases = {
            {"Is the Trinity in the Bible?", "Deuteronomy 6:4"},
            {"What is the Bible's teaching on the Sabbath?", "fourth commandment"},
            {"What does the Bible say about women preaching?", "not settled"},
            {"Can a Christian lose their salvation?", "assurance"},
            {"What does the Bible say about tithing?", "not settled"},
            {"Is speaking in tongues the evidence of the Holy Spirit?", "not the only evidence"},
            {"What does the Bible say about divorce?", "not settled"}
        };
        StringBuilder failures = new StringBuilder();
        for (String[] cs : cases) {
            String out = route(c, cs[0]);
            if (!out.contains(cs[1])) failures.append(cs[0]).append(" -> ").append(out, 0, Math.min(120, out.length())).append('\n');
        }
        assertTrue("Doctrine route failures:\n" + failures, failures.length() == 0);
    }
}
