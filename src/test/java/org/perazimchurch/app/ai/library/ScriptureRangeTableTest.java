package org.perazimchurch.app.ai.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ScriptureRangeTableTest {

    private static ScriptureRangeTable table() throws Exception {
        File file = new File("src/main/assets/ai/library/scripture/kjv-verse-counts.json");
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return ScriptureRangeTable.fromJson(json);
    }

    @Test
    public void rejectsNonExistentChaptersAndVerses() throws Exception {
        ScriptureRangeTable t = table();
        int ezekiel = t.bookNumber("Ezekiel");
        assertEquals(26, ezekiel);
        assertEquals(48, t.chapterCount(ezekiel));
        assertFalse("Ezekiel 50 does not exist", t.isValid(ezekiel, 50, 4));
        assertFalse("Ezekiel 49 does not exist", t.isValid(ezekiel, 49, 1));
        assertFalse("Ezekiel 48 has 35 verses", t.isValid(ezekiel, 48, 36));
        assertFalse("verse 0 does not exist", t.isValid(ezekiel, 48, 0));
        assertFalse("Jude has one chapter", t.isValid(t.bookNumber("Jude"), 2, 1));
        assertFalse("no book 67", t.isValid(67, 1, 1));
    }

    @Test
    public void acceptsRealReferencesAtTheEdges() throws Exception {
        ScriptureRangeTable t = table();
        assertTrue(t.isValid(26, 48, 35));
        assertTrue(t.isValid(1, 1, 1));
        assertTrue(t.isValid(19, 119, 176));
        assertTrue(t.isValid(66, 22, 21));
        assertEquals(31, t.verseCount(1, 1));
        assertEquals(176, t.verseCount(19, 119));
    }

    @Test
    public void tableHasSixtySixBooksAndTheKjvVerseTotal() throws Exception {
        ScriptureRangeTable t = table();
        int total = 0;
        for (int b = 1; b <= 66; b++) {
            assertTrue("book " + b + " has chapters", t.chapterCount(b) > 0);
            for (int c = 1; c <= t.chapterCount(b); c++) total += t.verseCount(b, c);
        }
        assertEquals(31102, total);
    }
}
