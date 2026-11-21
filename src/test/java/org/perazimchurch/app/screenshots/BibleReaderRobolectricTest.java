package org.perazimchurch.app.screenshots;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.room.Room;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.MainActivity;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Bible reader checks on the laptop (Robolectric). Covers opening the reader, chapter navigation, the translation
 * badge and the verse list. Highlight, interlinear, Strong's and notes are NOT covered here yet.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BibleReaderRobolectricTest {

    private static List<TextView> textViews(View root, List<TextView> out) {
        if (root instanceof TextView) out.add((TextView) root);
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) textViews(g.getChildAt(i), out);
        }
        return out;
    }

    private static boolean hasText(List<TextView> views, String needle) {
        for (TextView v : views) {
            if (v.getText() != null && v.getText().toString().contains(needle)) return true;
        }
        return false;
    }

    private static TextView findExact(List<TextView> views, String text) {
        for (TextView v : views) {
            if (v.getText() != null && text.equals(v.getText().toString())) return v;
        }
        return null;
    }

    @Test
    public void readerOpensNavigatesAndShowsVerses() throws Exception {
        WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext());
        PerazimDatabase db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PerazimDatabase.class)
                .allowMainThreadQueries().build();
        Field instance = PerazimDatabase.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, db);

        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        Method open = MainActivity.class.getDeclaredMethod("showFullScriptureDialog");
        open.setAccessible(true);
        open.invoke(activity);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        Dialog dialog = null;
        for (Dialog d : ShadowDialog.getShownDialogs()) {
            if (d instanceof org.perazimchurch.app.presentation.ui.BibleReaderDialog && d.isShowing()) dialog = d;
        }
        assertTrue("reader dialog should open", dialog != null);
        // Verses and translations load asynchronously: pump the main looper until they appear (bounded).
        List<TextView> views = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            views = textViews(dialog.getWindow().getDecorView(), new ArrayList<>());
            if (hasText(views, "LORD") && findExact(views, "KJV") != null) break;
            Thread.sleep(20);
        }
        StringBuilder seen = new StringBuilder();
        for (TextView v : views) if (v.getText() != null && v.getText().length() > 0) seen.append('[').append(v.getText().toString().replace('\n', ' ')).append("] ");
        assertTrue("translation badge KJV shown; visible text: " + seen.substring(0, Math.min(seen.length(), 700)), findExact(views, "KJV") != null);

        // Component: translation badge and chapter title
        assertTrue("chapter counter shown", hasText(views, "of 24"));
        // Component: verse list has verses
        assertTrue("verse text rendered", hasText(views, "LORD"));

        // Component: Next chapter
        TextView next = findExact(views, "Next");
        assertTrue("Next button present", next != null);
        String before = findExact(views, "Next") != null ? "" : "";
        next.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        List<TextView> after = textViews(dialog.getWindow().getDecorView(), new ArrayList<>());
        String counterAfterNext = null;
        for (TextView v : after) if (v.getText() != null && v.getText().toString().contains(" of 24")) counterAfterNext = v.getText().toString();
        assertTrue("counter moved after Next", counterAfterNext != null && counterAfterNext.startsWith("Chapter"));

        // Component: Previous returns
        findExact(after, "Previous").performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertFalse("reader still open after navigation", !dialog.isShowing());
        // Screenshot of the reader for the record (build/ui-screenshots/reader_default.png)
        View decor = dialog.getWindow().getDecorView();
        decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(2340, android.view.View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 1080, 2340);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(1080, 2340, android.graphics.Bitmap.Config.ARGB_8888);
        decor.draw(new android.graphics.Canvas(bmp));
        java.io.File out = new java.io.File("build/ui-screenshots");
        out.mkdirs();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(out, "reader_default.png"))) {
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
        }
        db.close();
    }
}
