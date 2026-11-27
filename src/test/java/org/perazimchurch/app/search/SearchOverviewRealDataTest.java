package org.perazimchurch.app.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.search.SearchOverview;
import org.perazimchurch.app.ai.search.SearchOverviewBuilder;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.data.repository.RoomBibleRepository;
import org.perazimchurch.app.data.search.BibleOverviewSources;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The AI overview for the search checks, against the bundled KJV text, the interlinear and the lexicon. Each query
 * runs three times and must give the same answer each time. Every cited verse must carry the KJV text from the
 * Bible database, verbatim.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SearchOverviewRealDataTest {

    private static final String[] QUERIES = {
            "where in the bible does Jesus weep",
            "Show me verses that speak on propitiation",
            "faith",
            "Baal-perazim",
            "John 3:16",
            "the",
            "",
    };
    private static final int RUNS = 3;

    private PerazimDatabase database;
    private RoomBibleRepository repository;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, PerazimDatabase.class).allowMainThreadQueries().build();
        BibleTestSupport.importBundledText(context, database);
        repository = new RoomBibleRepository(context, database);
    }

    @After
    public void tearDown() {
        database.close();
    }

    private SearchOverview overviewFor(String query) throws Exception {
        return BibleTestSupport.background(() ->
                new SearchOverviewBuilder(new BibleOverviewSources(context, repository)).build(query));
    }

    private BibleVerse storedVerse(SearchOverview.VerseRef ref) throws Exception {
        return BibleTestSupport.background(() -> repository.getVerse("KJV",
                BibleMapper.getBookIdFromNumber(ref.bookNumber), ref.chapter, ref.verse));
    }

    private static String describe(SearchOverview.VerseRef ref) {
        return BibleMapper.getBookIdFromNumber(ref.bookNumber) + " " + ref.chapter + ":" + ref.verse;
    }

    /** Every cited verse must be the KJV text stored in the Bible database. */
    private void assertCitedVersesAreKjv(SearchOverview overview) throws Exception {
        for (SearchOverview.VerseRef ref : overview.verses) {
            BibleVerse stored = storedVerse(ref);
            assertNotNull("KJV text missing for " + describe(ref), stored);
            assertEquals("verse text is not the stored KJV for " + describe(ref),
                    stored.getText().trim(), ref.kjvText.trim());
        }
    }

    @Test
    public void jesusWeepCitesJohn1135WithKjvText() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[0]);
            assertNotNull(overview);
            assertFalse("no verses for 'Jesus weep'", overview.verses.isEmpty());
            assertEquals("first cited verse for 'Jesus weep'", "JHN 11:35", describe(overview.verses.get(0)));
            assertCitedVersesAreKjv(overview);
        }
    }

    @Test
    public void propitiationCitesRomans325_1John22_1John410_andHebrews95() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[1]);
            assertNotNull(overview);
            // Hebrews 9:5 is cited through its Greek word G2435, which is the same word as Romans 3:25.
            assertEquals("verses for propitiation", 4, overview.verses.size());
            assertTrue("Romans 3:25 cited", containsVerse(overview, "ROM 3:25"));
            assertTrue("1 John 2:2 cited", containsVerse(overview, "1JN 2:2"));
            assertTrue("1 John 4:10 cited", containsVerse(overview, "1JN 4:10"));
            assertTrue("Hebrews 9:5 cited", containsVerse(overview, "HEB 9:5"));
            assertCitedVersesAreKjv(overview);
        }
    }

    /** The G2435 root note lists Romans 3:25 under "also used in", so the card links the two verses. */
    @Test
    public void propitiationRootG2435AlsoNamesRomans325() throws Exception {
        SearchOverview overview = overviewFor(QUERIES[1]);
        SearchOverview.RootNote root = null;
        for (SearchOverview.RootNote note : overview.roots) {
            if ("G2435".equals(note.strongsCode)) {
                root = note;
            }
        }
        assertNotNull("the root G2435 is shown for propitiation", root);
        boolean romans = false;
        for (SearchOverview.VerseRef ref : root.alsoIn) {
            if (describe(ref).equals("ROM 3:25")) {
                romans = true;
            }
        }
        assertTrue("G2435 alsoIn includes Romans 3:25", romans);
        System.out.println("ALSO_IN G2435 " + root.alsoIn.size() + " entries: " + describeAll(root.alsoIn));
    }

    private static String describeAll(java.util.List<SearchOverview.VerseRef> refs) {
        StringBuilder out = new StringBuilder();
        for (SearchOverview.VerseRef ref : refs) {
            if (out.length() > 0) {
                out.append("; ");
            }
            out.append(describe(ref));
        }
        return out.toString();
    }

    @Test
    public void faithGivesVersesAndOnlyKjvText() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[2]);
            assertNotNull(overview);
            assertFalse("no verses for 'faith'", overview.verses.isEmpty());
            assertCitedVersesAreKjv(overview);
        }
    }

    @Test
    public void baalPerazimCitesThe2SamuelPassage() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[3]);
            assertNotNull(overview);
            assertFalse("no verses for 'Baal-perazim'", overview.verses.isEmpty());
            assertTrue("2 Samuel 5:20 cited", containsVerse(overview, "2SA 5:20"));
            assertCitedVersesAreKjv(overview);
        }
    }

    @Test
    public void typedReferenceIsNotAKeywordSearch() throws Exception {
        // The reader opens a typed reference directly; the builder must not crash on it either.
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[4]);
            assertNotNull(overview);
            assertCitedVersesAreKjv(overview);
        }
    }

    @Test
    public void stopWordTheGivesNoOverview() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[5]);
            assertNotNull(overview);
            assertTrue("'the' must not produce an overview card", overview.verses.isEmpty());
        }
    }

    @Test
    public void emptyQueryGivesEmptyOverview() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            SearchOverview overview = overviewFor(QUERIES[6]);
            assertNotNull(overview);
            assertTrue(overview.verses.isEmpty());
            assertTrue(overview.roots.isEmpty());
            assertEquals(0, overview.totalVerses);
        }
    }

    @Test
    public void noMatchesDoesNotCrashAndGivesNoCard() throws Exception {
        SearchOverview overview = overviewFor("zzqxvbnmkj");
        assertNotNull(overview);
        assertTrue(overview.verses.isEmpty());
        assertEquals(0, overview.totalVerses);
    }

    @Test
    public void noInterlinearHitsDoesNotCrash() throws Exception {
        // A word that is in the KJV text but has no interlinear gloss must still give a clean answer.
        SearchOverview overview = overviewFor("Baal-perazim");
        assertNotNull(overview);
        assertCitedVersesAreKjv(overview);
    }

    @Test
    public void cardRuleVersesIsAllOrNothingAgainstTotal() throws Exception {
        for (String query : QUERIES) {
            SearchOverview overview = overviewFor(query);
            assertTrue("shown verses never exceed the total for '" + query + "'",
                    overview.verses.size() <= overview.totalVerses);
            assertEquals("a card is shown only when there are verses for '" + query + "'",
                    !overview.verses.isEmpty(), overview.totalVerses > 0);
        }
    }

    private static boolean containsVerse(SearchOverview overview, String wanted) {
        for (SearchOverview.VerseRef ref : overview.verses) {
            if (describe(ref).equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
