package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.BeforeClass;
import org.junit.Test;
import org.perazimchurch.app.ai.library.KjvVerseText;
import org.perazimchurch.app.ai.library.ScriptureRangeTable;

/** The reference route: fake references are refused, bundled verses are quoted verbatim, nothing is filled from memory. */
public class PreModelTest {
    private static ScriptureRangeTable table;
    private static KjvVerseText verses;

    @BeforeClass
    public static void load() throws Exception {
        table = ScriptureRangeTable.fromJson(read("src/main/assets/ai/library/scripture/kjv-verse-counts.json"));
        verses = KjvVerseText.fromJson(read("src/main/assets/bible/kjv_verses.json"),
                read("src/main/assets/bible/kjv_manifest.json"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void nonExistentVerseIsRefusedNotQuoted() {
        PreModel.Decision d = PreModel.missingReference(table, "What does Ezekiel 50:4 say?");
        assertNotNull(d);
        assertEquals("routed:missing-reference", d.status);
        assertTrue(d.answer.contains("can't find Ezekiel 50:4"));
        assertTrue(d.answer.contains("King James Version"));
    }

    @Test
    public void nonExistentVerseForTheEvalCaseIsRefused() {
        assertNotNull(PreModel.missingReference(table, "Quote Psalm 151:1 from the King James Bible"));
    }

    @Test
    public void realReferenceIsNotRefusedAsMissing() {
        assertNull(PreModel.missingReference(table, "Quote 1 Corinthians 13:4-7"));
        assertNull(PreModel.missingReference(table, "What does Genesis 1:1 say?"));
    }

    @Test
    public void bundledVerseIsQuotedVerbatim() {
        PreModel.Decision d = PreModel.verseDecision(table, verses, "What does Genesis 1:1 say?");
        assertNotNull(d);
        assertEquals("routed:verse-kjv", d.status);
        assertTrue(d.answer.contains("In the beginning God created the heaven and the earth."));
        assertTrue(d.answer.contains("King James Version (KJV)"));
    }

    @Test
    public void validVerseMissingFromTheOfflineFileIsNotInvented() {
        PreModel.Decision d = PreModel.verseDecision(table, verses, "What does 1 Corinthians 13:4 mean?");
        assertNotNull(d);
        assertEquals("routed:verse-not-in-offline-text", d.status);
        assertTrue(d.answer.contains("I can't quote 1 Corinthians 13:4 from memory"));
    }

    @Test
    public void bundledRomansVerseIsQuotedVerbatim() {
        PreModel.Decision d = PreModel.verseDecision(table, verses, "What does Romans 8:28 mean?");
        assertNotNull(d);
        assertEquals("routed:verse-kjv", d.status);
        assertTrue(d.answer.contains("all things work together for good to them that love God"));
    }

    @Test
    public void rangeLongerThanTenVersesIsRefused() {
        PreModel.Decision d = PreModel.verseDecision(table, verses, "Quote Romans 8:1-30");
        assertNotNull(d);
        assertEquals("routed:verse-range-too-long", d.status);
    }

    @Test
    public void questionWithoutReferenceGoesOn() {
        assertNull(PreModel.verseDecision(table, verses, "What is grace?"));
        assertNull(PreModel.missingReference(table, "What is grace?"));
    }
}
