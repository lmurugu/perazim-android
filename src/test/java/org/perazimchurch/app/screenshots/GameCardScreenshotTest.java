package org.perazimchurch.app.screenshots;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.presentation.ui.games.GamesHubDialog;
import org.perazimchurch.app.ui.theme.PerazimTheme;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Design-review render of the Bible Games hub, light and dark. The Home game row is captured by the
 * all-tabs screenshot tests. Runs only in the UI screenshot job.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GameCardScreenshotTest {

    @Test
    public void captureHubLight() throws Exception {
        Assume.assumeTrue(UiScreenshotSupport.enabled());
        capture(true);
    }

    @Test
    public void captureHubDark() throws Exception {
        Assume.assumeTrue(UiScreenshotSupport.enabled());
        capture(false);
    }

    /**
     * The hub reads the signed-in user through the app database, which loads SQLCipher's native library. That
     * library is not available under Robolectric, so the test swaps in an in-memory database, as the all-tabs
     * screenshot does.
     */
    private static void installInMemoryDatabase() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx);
        PerazimDatabase db = Room.inMemoryDatabaseBuilder(ctx, PerazimDatabase.class)
                .allowMainThreadQueries()
                .build();
        Field instance = PerazimDatabase.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, db);
    }

    private static void capture(boolean light) throws Exception {
        installInMemoryDatabase();
        PerazimTheme.applyMode(light);
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        GamesHubDialog.show(activity);
        Dialog dialog = ShadowDialog.getLatestDialog();
        if (dialog == null || dialog.getWindow() == null) {
            throw new AssertionError("Games hub dialog did not open");
        }
        View content = dialog.getWindow().getDecorView().findViewById(android.R.id.content);
        View hub = content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0
                ? ((ViewGroup) content).getChildAt(0) : content;
        int w = UiScreenshotSupport.WIDTH_PX;
        int h = Math.min(4000, UiScreenshotSupport.HEIGHT_PX * 2);
        hub.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.max(1, Math.min(h, hub.getMeasuredHeight()));
        hub.layout(0, 0, w, height);
        Bitmap bitmap = Bitmap.createBitmap(w, height, Bitmap.Config.ARGB_8888);
        hub.draw(new Canvas(bitmap));
        File outDir = new File("build/ui-screenshots");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        UiScreenshotSupport.writePng(bitmap, new File(outDir, (light ? "light" : "dark") + "_hub_game_cards.png"));
        dialog.dismiss();
    }
}
