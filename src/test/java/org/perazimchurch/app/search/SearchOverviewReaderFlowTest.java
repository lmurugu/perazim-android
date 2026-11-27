package org.perazimchurch.app.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.search.SearchOverview;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.data.repository.RoomBibleRepository;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.presentation.ui.BibleReaderDialog;
import org.perazimchurch.app.presentation.viewmodel.BibleViewModel;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * The search screen, driven through the real reader dialog with the bundled Bible: the AI OVERVIEW card, the verse
 * links in it, the reader opening at a tapped verse, and the routes that must show no card. Searches run on the
 * background executor, as they do in the app, and the main looper is drained after each action.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SearchOverviewReaderFlowTest {

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
    private BibleReaderDialog dialog;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, PerazimDatabase.class).allowMainThreadQueries().build();
        BibleTestSupport.importBundledText(context, database);
        repository = new RoomBibleRepository(context, database);
        BibleViewModel viewModel = new BibleViewModel(repository, BibleTestSupport.BACKGROUND);
        dialog = new BibleReaderDialog(context, viewModel);
        dialog.show();
        BibleTestSupport.drain();
        // The search screen: the user switches to search, then types. Results and the overview only show in it.
        call("toggleSearchMode", new Class<?>[]{});
        BibleTestSupport.drain();
    }

    @After
    public void tearDown() {
        if (dialog != null) {
            dialog.dismiss();
        }
        database.close();
    }

    // ---- reflection helpers, the same access the reader's own tests use ----

    private static Object field(Object target, String name) throws Exception {
        Field f = BibleReaderDialog.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = BibleReaderDialog.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(dialog, args);
    }

    /** Searches fresh: the search screen is on, the query runs, and every background callback has been delivered. */
    private void search(String query) throws Exception {
        // Opening a verse leaves search mode, as it does in the app; switch back before the next search.
        if (!((Boolean) field(dialog, "isSearchMode"))) {
            call("toggleSearchMode", new Class<?>[]{});
        }
        call("performSearch", new Class<?>[]{String.class}, query);
        BibleTestSupport.drain();
    }

    private View container() throws Exception {
        return (View) field(dialog, "versesContainer");
    }

    /** The AI OVERVIEW title, or null when the card is not on screen. */
    private TextView overviewTitle(View root) {
        if (root instanceof TextView && "AI OVERVIEW".equals(((TextView) root).getText().toString())) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = overviewTitle(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** The verse link whose text ends with the KJV verse text (the link is "Book C:V  KJV text"). */
    private TextView verseLink(View root, String kjvText) {
        if (root instanceof TextView) {
            String text = ((TextView) root).getText().toString();
            if (text.endsWith(kjvText) && text.contains("  ")) {
                return (TextView) root;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = verseLink(group.getChildAt(i), kjvText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private TextView findText(View root, String contains) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(contains)) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), contains);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String describe(SearchOverview.VerseRef ref) {
        return BibleMapper.getBookIdFromNumber(ref.bookNumber) + " " + ref.chapter + ":" + ref.verse;
    }

    private SearchOverview currentOverview() throws Exception {
        return (SearchOverview) field(dialog, "searchOverview");
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, View> verseRows() throws Exception {
        return (Map<Integer, View>) field(dialog, "verseRows");
    }

    private BibleVerse storedVerse(SearchOverview.VerseRef ref) throws Exception {
        return BibleTestSupport.background(() -> repository.getVerse("KJV",
                BibleMapper.getBookIdFromNumber(ref.bookNumber), ref.chapter, ref.verse));
    }

    // ---- checks ----

    /**
     * For every query, three runs: the card is on screen exactly when the overview has verses, the four keyword
     * queries must show it, and every card link carries the stored KJV text.
     */
    @Test
    public void overviewCardAppearsOnlyWhenThereAreVerses() throws Exception {
        for (String query : QUERIES) {
            for (int run = 0; run < RUNS; run++) {
                search("");
                search(query);
                SearchOverview overview = currentOverview();
                boolean cardShown = overviewTitle(container()) != null;
                boolean hasVerses = overview != null && !overview.verses.isEmpty();
                assertEquals("overview card shown for '" + query + "' run " + run, hasVerses, cardShown);
                if (query.equals("faith") || query.equals("Baal-perazim")
                        || query.equals("Show me verses that speak on propitiation")
                        || query.equals("where in the bible does Jesus weep")) {
                    assertTrue("a keyword query must show the card: '" + query + "' run " + run, cardShown);
                }
                if (hasVerses) {
                    for (SearchOverview.VerseRef ref : overview.verses) {
                        BibleVerse stored = storedVerse(ref);
                        assertNotNull("stored KJV for " + describe(ref), stored);
                        assertNotNull("card shows the KJV text of " + describe(ref),
                                verseLink(container(), stored.getText().trim()));
                    }
                }
            }
        }
    }

    /** The reported case: Jesus weep cites John 11:35 and its tap opens John 11 with that verse. */
    @Test
    public void tappingJohn1135OpensTheReaderAtThatVerse() throws Exception {
        search("");
        search("where in the bible does Jesus weep");
        SearchOverview overview = currentOverview();
        assertNotNull(overview);
        SearchOverview.VerseRef john = null;
        for (SearchOverview.VerseRef ref : overview.verses) {
            if (describe(ref).equals("JHN 11:35")) {
                john = ref;
            }
        }
        assertNotNull("John 11:35 is cited for 'Jesus weep'", john);
        TextView link = verseLink(container(), john.kjvText);
        assertNotNull("John 11:35 link on the card", link);
        assertTrue("the link shows the KJV text", link.getText().toString().contains("Jesus wept"));
        link.performClick();
        BibleTestSupport.drain();
        assertEquals("book opened", "JHN", field(dialog, "currentBookId"));
        assertEquals("chapter opened", 11, field(dialog, "currentChapter"));
        assertTrue("verse 35 is on screen", verseRows().containsKey(35));
    }

    /** Every verse link on every keyword card opens the reader at its chapter and verse. */
    @Test
    public void everyCitedVerseOpensTheReaderAtThatVerse() throws Exception {
        String[] keywordQueries = {
                "where in the bible does Jesus weep",
                "Show me verses that speak on propitiation",
                "faith",
                "Baal-perazim",
        };
        for (String query : keywordQueries) {
            for (int run = 0; run < RUNS; run++) {
                search("");
                search(query);
                SearchOverview overview = currentOverview();
                assertNotNull("overview for '" + query + "'", overview);
                for (SearchOverview.VerseRef ref : overview.verses) {
                    search(query);
                    TextView link = verseLink(container(), ref.kjvText);
                    assertNotNull("link for " + describe(ref) + " in '" + query + "'", link);
                    link.performClick();
                    BibleTestSupport.drain();
                    assertEquals("book for " + describe(ref),
                            BibleMapper.getBookIdFromNumber(ref.bookNumber), field(dialog, "currentBookId"));
                    assertEquals("chapter for " + describe(ref), ref.chapter, field(dialog, "currentChapter"));
                    assertTrue("verse " + ref.verse + " on screen for " + describe(ref),
                            verseRows().containsKey(ref.verse));
                }
            }
        }
    }

    /** A typed reference opens the passage directly and never shows the overview card. */
    @Test
    public void typedReferenceOpensPassageWithoutOverview() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            search("");
            search("John 3:16");
            assertNull("no overview card for a typed reference", overviewTitle(container()));
            assertEquals("book opened", "JHN", field(dialog, "currentBookId"));
            assertEquals("chapter opened", 3, field(dialog, "currentChapter"));
        }
    }

    /** The card's root line for propitiation names Romans 3:25 under "Also used in". Prints the exact text. */
    @Test
    public void propitiationRootLineNamesRomans325() throws Exception {
        search("");
        search("Show me verses that speak on propitiation");
        TextView rootLine = findText(container(), "Also used in");
        assertNotNull("the root line with 'Also used in' is on the card", rootLine);
        String text = rootLine.getText().toString();
        System.out.println("CARD_TEXT " + text);
        assertTrue("the root line names Romans 3:25", text.contains("Also used in") && text.contains("Romans 3:25"));
        assertTrue("the root line names the G2435 code", text.contains("G2435"));
    }

    /** "the" has no content words to search for, so no card and no crash. */
    @Test
    public void stopWordShowsNoCardAndDoesNotCrash() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            search("");
            search("the");
            assertNull("no overview card for 'the'", overviewTitle(container()));
        }
    }

    /** No matches: no card, the empty message is shown, and nothing crashes. */
    @Test
    public void noMatchesShowsEmptyMessageAndNoCard() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            search("");
            search("zzqxvbnmkj");
            assertNull("no overview card with no matches", overviewTitle(container()));
            assertNotNull("empty message for a query with no matches",
                    findText(container(), "No scripture verses found"));
        }
    }

    /** Empty query: the search is refused with a message and nothing crashes. */
    @Test
    public void emptyQueryShowsNoCardAndDoesNotCrash() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            search("");
            assertNull("no overview card for an empty query", overviewTitle(container()));
            assertNull("no overview object for an empty query", currentOverview());
        }
    }
}
