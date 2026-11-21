package org.perazimchurch.app.presentation.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.repository.RoomBibleRepository;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.presentation.viewmodel.BibleViewModel;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The reported path: open the verse bar on verse 4, open the colour picker, choose a colour, tap the check.
 * The verse must end up highlighted and saved. Uses a real in-memory database and the real reader dialog.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReaderHighlightFlowTest {

    private PerazimDatabase database;
    private RoomBibleRepository repository;
    private BibleReaderDialog dialog;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, PerazimDatabase.class).allowMainThreadQueries().build();
        repository = new RoomBibleRepository(database.bibleDao());
        BibleViewModel viewModel = new BibleViewModel(repository, Runnable::run);
        dialog = new BibleReaderDialog(context, viewModel);
    }

    @After
    public void tearDown() {
        if (dialog != null) {
            dialog.dismiss();
        }
        database.close();
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = BibleReaderDialog.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = BibleReaderDialog.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static View findByDescription(View root, String description) {
        if (description.contentEquals(root.getContentDescription() == null ? "" : root.getContentDescription())) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findByDescription(group.getChildAt(i), description);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static TextView findTextView(View root, String contains) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(contains)) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTextView(group.getChildAt(i), contains);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    public void checkAfterChoosingAColourHighlightsTheOpenVerse() throws Exception {
        dialog.show();

        BibleVerse verse = new BibleVerse();
        verse.setBookId("2SA");
        verse.setChapterNumber(5);
        verse.setVerseNumber(4);
        verse.setTranslationId("KJV");
        verse.setText("David was thirty years old when he began to reign.");
        @SuppressWarnings("unchecked")
        List<BibleVerse> current = (List<BibleVerse>) field(dialog, "currentVerses");
        current.clear();
        current.add(verse);

        // 1. Tap verse 4: the verse bar opens on it.
        call(dialog, "onVerseTapped", new Class<?>[]{BibleVerse.class}, verse);
        assertNotNull("the verse bar must open on a tap", field(dialog, "actionBarView"));

        // 2. Open the colour picker panel, choose a colour (the first circle), then tap the check.
        call(dialog, "openSwipeColourPanel", new Class<?>[]{});
        View panel = (View) field(dialog, "notesPanelView");
        assertNotNull("the colour panel must open", panel);
        View picker = findByDescription(panel, "Done");
        assertNotNull("the check must be there", picker);
        ImageView check = (ImageView) picker;
        check.performClick();

        // 3. The verse is highlighted in memory and saved to the database.
        assertNotNull("the verse must carry a highlight colour after the check", verse.getHighlightColor());
        assertEquals(verse.getHighlightColor(), repository.getHighlightsForChapter("2SA", 5).get(4));
    }

    /**
     * A single finger tap on the verse TEXT (not the number) must open the verse bar on the first tap.
     * Selectable text takes focus on the first tap and swallows the click, so a click listener alone is not enough.
     */
    @Test
    public void oneTapOnVerseTextOpensTheVerseBar() throws Exception {
        dialog.show();

        BibleVerse verse = new BibleVerse();
        verse.setBookId("2SA");
        verse.setChapterNumber(5);
        verse.setVerseNumber(4);
        verse.setTranslationId("KJV");
        verse.setText("David was thirty years old when he began to reign, and he reigned forty years.");
        @SuppressWarnings("unchecked")
        List<BibleVerse> current = (List<BibleVerse>) field(dialog, "currentVerses");
        current.clear();
        current.add(verse);
        call(dialog, "renderVerseList", new Class<?>[]{List.class, boolean.class}, current, false);

        View container = (View) field(dialog, "versesContainer");
        TextView text = findTextView(container, "thirty years");
        assertNotNull("the verse text must be on screen", text);
        text.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        text.layout(0, 0, text.getMeasuredWidth(), text.getMeasuredHeight());

        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 200f, 20f, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, 200f, 20f, 0);
        text.dispatchTouchEvent(down);
        text.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();

        assertNotNull("one tap on the verse text must open the verse bar", field(dialog, "actionBarView"));
    }
}
