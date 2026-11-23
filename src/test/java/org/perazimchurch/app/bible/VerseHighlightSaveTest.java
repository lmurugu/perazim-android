package org.perazimchurch.app.bible;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.BibleWordHighlightEntity;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.data.repository.RoomBibleRepository;
import org.perazimchurch.app.presentation.bible.HighlightColors;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Map;

/**
 * A highlight chosen in the reader must be saved, read back, and resolve to a colour the reader can draw.
 * Runs against a real in-memory Room database, the same repository the reader uses, with no member data.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class VerseHighlightSaveTest {

    private PerazimDatabase database;
    private RoomBibleRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, PerazimDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new RoomBibleRepository(database.bibleDao());
    }

    @After
    public void tearDown() {
        database.close();
    }

    /** The picker returns "#RRGGBB" for a mix made with the thumbs; it must save and show, as a name does. */
    @Test
    public void spectrumColourSavesAndShows() {
        repository.setHighlight("2SA", 5, 4, "#C03CE0");

        Map<Integer, String> saved = repository.getHighlightsForChapter("2SA", 5);
        assertEquals("#C03CE0", saved.get(4));

        HighlightColors.Colour drawn = HighlightColors.byName(saved.get(4));
        assertNotNull("a saved picker colour must resolve to something the reader can draw", drawn);
        assertEquals("#38C03CE0", drawn.tint);
    }

    @Test
    public void namedColourSavesAndShows() {
        repository.setHighlight("2SA", 5, 4, "bold-purple");

        String saved = repository.getHighlightsForChapter("2SA", 5).get(4);
        assertEquals("bold-purple", saved);
        assertNotNull(HighlightColors.byName(saved));
    }

    @Test
    public void clearingRemovesTheHighlight() {
        repository.setHighlight("2SA", 5, 4, "red");
        repository.setHighlight("2SA", 5, 4, null);

        assertNull(repository.getHighlightsForChapter("2SA", 5).get(4));
    }

    @Test
    public void onlyTheChosenVerseIsHighlighted() {
        repository.setHighlight("2SA", 5, 4, "#C03CE0");

        Map<Integer, String> saved = repository.getHighlightsForChapter("2SA", 5);
        assertEquals(1, saved.size());
        assertNull(saved.get(3));
        assertNull(saved.get(5));
    }

    /** Several verses chosen together each keep the colour. */
    @Test
    public void severalVersesSaveTogether() {
        for (int verse = 3; verse <= 6; verse++) {
            repository.setHighlight("2SA", 5, verse, "#43A047");
        }

        Map<Integer, String> saved = repository.getHighlightsForChapter("2SA", 5);
        assertEquals(4, saved.size());
        assertEquals("#43A047", saved.get(6));
    }

    /** A word range is stored apart from the verse highlight, and clearing one range leaves the others. */
    @Test
    public void wordRangesSaveAndClearOnTheirOwn() {
        int book = BibleMapper.resolveBookNumber("2SA");
        repository.setHighlight("2SA", 5, 4, "#C03CE0");
        repository.addWordHighlight("KJV", "2SA", 5, 4, 0, 5, "red");
        repository.addWordHighlight("KJV", "2SA", 5, 4, 10, 20, "bold-yellow");

        List<BibleWordHighlightEntity> ranges = database.bibleDao().getWordHighlightsForChapter("KJV", book, 5);
        assertEquals(2, ranges.size());
        assertEquals("the verse highlight is separate from the ranges", "#C03CE0",
                repository.getHighlightsForChapter("2SA", 5).get(4));

        repository.clearWordHighlights("KJV", "2SA", 5, 4, 2, 4);

        ranges = database.bibleDao().getWordHighlightsForChapter("KJV", book, 5);
        assertEquals(1, ranges.size());
        assertEquals("bold-yellow", ranges.get(0).getColor());
        assertTrue(repository.getHighlightsForChapter("2SA", 5).containsKey(4));
    }
}
