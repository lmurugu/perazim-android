package org.perazimchurch.app.ai.search;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Question ranking on a small fixture of verse texts. The fixtures are excerpts used only to test the ranking code.
 * They are not a source for any Scripture shown to users. The same ranking runs for every question, and the
 * questions here are not used to tune it.
 */
public class QuestionSearchTest {

    private static final int JOB = 18, LUKE = 42, ISAIAH = 23, REV = 66, FIXTURE = 99;

    /** Verses as {book, chapter, verse, text}. */
    private static final Object[][] CORPUS = {
            {LUKE, 10, 18, "And he said unto them, I beheld Satan as lightning fall from heaven."},
            {REV, 12, 9, "And the great dragon was cast out, that old serpent, called the Devil, and Satan, which deceiveth the whole world: he was cast out into the earth, and his angels were cast out with him."},
            {REV, 12, 10, "For the accuser of our brethren is cast down, which accused them before our God day and night."},
            {ISAIAH, 14, 12, "How art thou fallen from heaven, O Lucifer, son of the morning! how art thou cut down to the ground, which didst weaken the nations!"},
            {JOB, 1, 6, "Now there was a day when the sons of God came to present themselves before the LORD, and Satan came also among them."},
            {FIXTURE, 1, 1, "[fixture] an outcast from the camp"},
            {FIXTURE, 1, 2, "[fixture] the rain went down the valley"},
            {FIXTURE, 1, 3, "[fixture] the flood will drown the field"},
    };

    private static QuestionSearch.Lookup corpusLookup() {
        return phrase -> {
            List<QuestionSearch.Hit> hits = new ArrayList<>();
            String needle = phrase.toLowerCase();
            for (Object[] row : CORPUS) {
                String text = (String) row[3];
                if (text.toLowerCase().contains(needle)) {
                    hits.add(new QuestionSearch.Hit((Integer) row[0], (Integer) row[1], (Integer) row[2], text));
                }
            }
            return hits;
        };
    }

    private static List<String> refs(List<QuestionSearch.Ranked> ranked) {
        List<String> refs = new ArrayList<>();
        for (QuestionSearch.Ranked r : ranked) {
            refs.add(r.bookNumber + ":" + r.chapter + ":" + r.verse);
        }
        return refs;
    }

    @Test
    public void questionFramingIsDropped() {
        assertEquals(Arrays.asList("satan", "cast", "down"),
                SearchTerms.of("Where in the Bible is it written that Satan was cast down?"));
    }

    @Test
    public void wholeSentenceQuestionFindsTheVersesAboutSatanBeingCastDown() {
        List<String> refs = refs(QuestionSearch.rank(
                "Where in the Bible is it written that Satan was cast down?", corpusLookup()));
        // Three words match in Revelation 12:10 (accuser, cast, down), so it ranks first.
        assertEquals("66:12:10", refs.get(0));
        assertTrue(refs.contains("66:12:9"));
        assertTrue(refs.contains("42:10:18"));
        assertTrue(refs.contains("23:14:12"));
    }

    @Test
    public void partialWordMatchesDoNotCount() {
        List<String> refs = refs(QuestionSearch.rank(
                "Where in the Bible is it written that Satan was cast down?", corpusLookup()));
        assertFalse("outcast must not match cast", refs.contains("99:1:1"));
        assertFalse("drown must not match down", refs.contains("99:1:3"));
    }

    @Test
    public void unseenQuestionRanksTheVerseWithMostWordsFirst() {
        List<String> refs = refs(QuestionSearch.rank(
                "Which verse says the devil fell from heaven?", corpusLookup()));
        // Luke 10:18 has Satan (devil), fall (fell) and heaven: three words.
        assertEquals("42:10:18", refs.get(0));
    }

    @Test
    public void synonymFindsAVerseThatUsesADifferentWord() {
        List<String> refs = refs(QuestionSearch.rank("Who is the adversary in the book of Job?", corpusLookup()));
        // "adversary" is grouped with "Satan", so Job 1:6 is found even though it never says "adversary".
        assertTrue(refs.contains("18:1:6"));
    }

    @Test
    public void emptyQuestionGivesNothing() {
        assertTrue(QuestionSearch.rank("   ", corpusLookup()).isEmpty());
        assertTrue(QuestionSearch.rank("the of is", corpusLookup()).isEmpty());
    }

    @Test
    public void resultsAreCapped() {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            rows.add(new Object[]{FIXTURE, 2, i, "[fixture] item " + i + " went down"});
        }
        QuestionSearch.Lookup lookup = phrase -> {
            List<QuestionSearch.Hit> hits = new ArrayList<>();
            for (Object[] row : rows) {
                String text = (String) row[3];
                if (text.toLowerCase().contains(phrase)) {
                    hits.add(new QuestionSearch.Hit((Integer) row[0], (Integer) row[1], (Integer) row[2], text));
                }
            }
            return hits;
        };
        assertEquals(QuestionSearch.MAX_RESULTS, QuestionSearch.rank("went down", lookup).size());
    }
}
