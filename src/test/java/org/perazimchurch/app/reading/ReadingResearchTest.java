package org.perazimchurch.app.reading;

import org.json.JSONArray;
import org.junit.Test;
import org.perazimchurch.app.domain.reading.ReadingCatalog;
import org.perazimchurch.app.domain.reading.ReadingResearch;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class ReadingResearchTest {
    private static final String MANIFEST = "[{\"id\":\"PSA\",\"name\":\"Psalms\",\"bookNumber\":19}]";

    @Test public void searchesTheUsersTopicAndKeepsTheQuotedText() throws Exception {
        String bible = "19\t25\t16\tTurn thee unto me, and have mercy upon me; for I am desolate and afflicted.\n"
                + "19\t102\t7\tI watch, and am as a sparrow alone upon the house top.";
        List<ReadingCatalog.Reading> results = ReadingResearch.search("Loneliness", new StringReader(bible), new JSONArray(MANIFEST));
        assertEquals(1, results.size());
        ReadingCatalog.Reading passage = results.get(0);
        assertEquals("Loneliness", passage.focus);
        assertEquals("Psalms 102:7", passage.reference);
        assertEquals("I watch, and am as a sparrow alone upon the house top.", passage.scripture);
        assertEquals(passage.id, ReadingCatalog.Reading.restoreResearch(passage.toResearchJson(), passage.focus).id);
    }

    @Test public void reportsNoEvidenceForAnUnrelatedTopic() throws Exception {
        assertTrue(ReadingResearch.search("digital rendering pipelines",
                new StringReader("19\t25\t16\tA passage about mercy."), new JSONArray(MANIFEST)).isEmpty());
    }
}
