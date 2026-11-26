package org.perazimchurch.app.ai.reference;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Picks Wesley's note for a verse out of a chapter's commentary. Pure logic, so it runs on the host. */
public class WesleyVerseNoteTest {

    private static final String CHAPTER = "1. The first note - opening words. 2. Seeing their faith - the paralytic. "
            + "3. This man blasphemeth - the scribes said so.";

    @Test
    public void findsTheNoteForTheVerse() {
        String note = ReferenceGuide.verseNote(CHAPTER, 2);
        assertTrue(note, note.startsWith("2. Seeing their faith"));
    }

    @Test
    public void verseWithoutANoteGivesTheChapterOpening() {
        assertEquals(CHAPTER, ReferenceGuide.verseNote(CHAPTER, 9));
    }

    @Test
    public void verseOneGivesTheFirstNote() {
        assertTrue(ReferenceGuide.verseNote(CHAPTER, 1).startsWith("1. The first note"));
    }
}
