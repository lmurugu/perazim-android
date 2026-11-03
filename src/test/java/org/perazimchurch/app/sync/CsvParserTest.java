package org.perazimchurch.app.sync;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying RFC 4180 compliance, multiline quotes, and Swahili diacritics in CsvParser.
 */
public class CsvParserTest {

    @Test
    public void testSimpleCsvParsing() throws IOException {
        String csv = "id,title,active\n1,Event One,TRUE\n2,Event Two,FALSE";
        List<List<String>> rows = CsvParser.parse(csv);
        assertEquals(3, rows.size());
        assertEquals("id", rows.get(0).get(0));
        assertEquals("Event One", rows.get(1).get(1));
        assertEquals("TRUE", rows.get(1).get(2));
        assertEquals("2", rows.get(2).get(0));
        assertEquals("FALSE", rows.get(2).get(2));
    }

    @Test
    public void testMultilineQuotedLyricsAndChords() throws IOException {
        String csv = "id,title,lyrics,chords\n" +
                "hymn_1,\"Holy, Holy, Holy!\",\"Verse 1:\nHoly, holy, holy!\nMerciful and mighty!\",\"D - Bm - G - A\"";
        List<List<String>> rows = CsvParser.parse(csv);
        assertEquals(2, rows.size());
        assertEquals("hymn_1", rows.get(1).get(0));
        assertEquals("Holy, Holy, Holy!", rows.get(1).get(1));
        assertTrue(rows.get(1).get(2).contains("\nHoly, holy, holy!\n"));
        assertEquals("D - Bm - G - A", rows.get(1).get(3));
    }

    @Test
    public void testEscapedDoubleQuotes() throws IOException {
        String csv = "id,text\n" +
                "joke_1,\"Pastor asked: \"\"Why are you sleeping?\"\" and usher replied.\"";
        List<List<String>> rows = CsvParser.parse(csv);
        assertEquals(2, rows.size());
        assertEquals("Pastor asked: \"Why are you sleeping?\" and usher replied.", rows.get(1).get(1));
    }

    @Test
    public void testUtf8BomStripping() throws IOException {
        String csvWithBom = "\uFEFFid,title\n101,Breakthrough";
        List<List<String>> rows = CsvParser.parse(csvWithBom);
        assertEquals(2, rows.size());
        assertEquals("id", rows.get(0).get(0)); // BOM stripped
        assertEquals("101", rows.get(1).get(0));
    }

    @Test
    public void testSwahiliLanguageContent() throws IOException {
        String csv = "date,reference,text\n" +
                "2026-11-03,\"Zaburi 23:1\",\"Bwana ndiye mchungaji wangu, Sitapungukiwa na kitu.\"\n" +
                "2026-11-04,\"Wafilipi 4:13\",\"Nayaweza mambo yote katika yeye anitiaye nguvu.\"";
        List<List<String>> rows = CsvParser.parse(csv);
        assertEquals(3, rows.size());
        assertEquals("Zaburi 23:1", rows.get(1).get(1));
        assertEquals("Bwana ndiye mchungaji wangu, Sitapungukiwa na kitu.", rows.get(1).get(2));
        assertEquals("Nayaweza mambo yote katika yeye anitiaye nguvu.", rows.get(2).get(2));
    }

    @Test
    public void testEmptyAndNullSafety() throws IOException {
        List<List<String>> empty = CsvParser.parse("");
        assertNotNull(empty);
        assertEquals(0, empty.size());

        List<List<String>> nullRes = CsvParser.parse((String) null);
        assertNotNull(nullRes);
        assertEquals(0, nullRes.size());
    }
}
