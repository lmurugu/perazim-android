package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;
import org.perazimchurch.app.ai.library.KjvVerseText;

public class PreModelVerseSearchTest {

    private static KjvVerseText kjv() throws Exception {
        return KjvVerseText.fromFiles(new File("src/main/assets/bible/text/KJV.tsv.gz"),
                new File("src/main/assets/bible/kjv_manifest.json"));
    }

    /** ST-045: the wording is not in the KJV. The closest verse is 1 Timothy 6:10, quoted verbatim, with a correction. */
    @Test
    public void misattributedWordingIsCorrectedWithVerbatimTopicVerse() throws Exception {
        PreModel.Decision d = PreModel.verseSearchDecision(kjv(),
                "Paul said 'money is the source of all evil' right?");
        assertEquals("routed:verse-topic-kjv-corrected", d.status);
        assertTrue(d.answer.contains("does not contain the exact wording"));
        assertTrue(d.answer.contains("1 Timothy 6:10: “For the love of money is the root of all evil"));
    }

    /** VT-151: the Pharaoh question gets the verbatim KJV verses. */
    @Test
    public void pharaohQuestionGetsVerbatimKjvVerses() throws Exception {
        PreModel.Decision d = PreModel.verseSearchDecision(kjv(), "Why did God haredn Pharaoh's heart?");
        assertEquals("routed:verse-topic-kjv", d.status);
        assertTrue(d.answer.contains("Exodus 9:12: “And the LORD hardened the heart of Pharaoh"));
        assertTrue(d.answer.contains("Romans 9:18"));
    }

    /** ST-041 and EX-105: a quoted wording that the KJV does not hold is not quoted from memory. */
    @Test
    public void quotedWordingNotInKjvIsSaidPlainly() throws Exception {
        PreModel.Decision d = PreModel.verseSearchDecision(kjv(),
                "Read me the verse that says 'God helps the one who waits' in Genesis");
        assertEquals("routed:verse-phrase-not-found", d.status);
        assertTrue(d.answer.contains("I can't find that wording in the King James Version"));
    }

    /** ST-148: an attributed saying not in the KJV is also refused without a quote. */
    @Test
    public void attributedSayingNotInKjvIsRefused() throws Exception {
        PreModel.Decision d = PreModel.verseSearchDecision(kjv(),
                "lol the bible says cleanliness is next to godliness right");
        assertEquals("routed:verse-phrase-not-found", d.status);
    }

    /** A wording that is in the KJV is quoted with its reference. */
    @Test
    public void quotedWordingInKjvIsQuotedVerbatim() throws Exception {
        PreModel.Decision d = PreModel.verseSearchDecision(kjv(), "What does 'Jesus wept' mean in the Bible?");
        assertEquals("routed:verse-phrase-kjv", d.status);
        assertTrue(d.answer.contains("John 11:35: “Jesus wept.”"));
    }

    @Test
    public void questionsWithoutAPhraseOrTopicAreNotHandled() throws Exception {
        assertNull(PreModel.verseSearchDecision(kjv(), "What is the Perazim Mission Church address?"));
    }

    /** An apostrophe inside a word ("I've", "can't") is not a quotation, so ordinary messages are not searched. */
    @Test
    public void apostrophesInOrdinaryMessagesAreNotQuotations() throws Exception {
        assertNull(PreModel.verseSearchDecision(kjv(),
                "I've been so angry at my brother for months and I can't stop thinking about it."));
    }
}
