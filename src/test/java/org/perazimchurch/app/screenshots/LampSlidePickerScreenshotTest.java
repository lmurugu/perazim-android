package org.perazimchurch.app.screenshots;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.lampslide.LampSlideLauncher;
import org.perazimchurch.app.ui.theme.PerazimTheme;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;

/** Design-review render of the Lamp Slide level map picker, light mode. Runs only in the UI screenshot job. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LampSlidePickerScreenshotTest {
    @Test
    public void capturePicker() throws Exception {
        Assume.assumeTrue(UiScreenshotSupport.enabled());
        PerazimTheme.applyMode(true);
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        SharedPreferences prefs = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("lamp_picker_shot", 0);
        GameContext ctx = new GameContext(activity, () -> { }, prefs, "", () -> true, () -> { });
        try {
            LampSlideLauncher.open(ctx);
        } catch (Throwable failure) {
            // Print the full trace: Gradle trims frames inside test failures, and the CI log needs the origin.
            failure.printStackTrace(System.out);
            throw failure;
        }
        Dialog dialog = ShadowDialog.getLatestDialog();
        if (dialog == null || dialog.getWindow() == null) {
            throw new AssertionError("Lamp Slide picker dialog did not open");
        }
        View root = dialog.getWindow().getDecorView();
        int w = UiScreenshotSupport.WIDTH_PX;
        int h = UiScreenshotSupport.HEIGHT_PX;
        for (int pass = 0; pass < 2; pass++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, w, h);
            org.perazimchurch.app.ui.theme.GlassPolish.apply(root);
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File outDir = new File("build/ui-screenshots");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        UiScreenshotSupport.writePng(bitmap, new File(outDir, "light_7_lamp_picker.png"));
        dialog.dismiss();
    }
}
