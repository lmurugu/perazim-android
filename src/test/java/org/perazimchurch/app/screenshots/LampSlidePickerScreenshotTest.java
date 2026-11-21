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
            // Gradle's console shows only the class and line, so put the top frames in the message.
            StringBuilder frames = new StringBuilder("picker open failed:");
            for (StackTraceElement el : failure.getStackTrace()) {
                if (frames.length() > 1500) break;
                frames.append(" | ").append(el);
            }
            // Diagnostic: the small copies are pushed to a git ref, so the full trace is readable from there.
            java.io.StringWriter trace = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(trace));
            File small = new File("build/ui-screenshots/small");
            //noinspection ResultOfMethodCallIgnored
            small.mkdirs();
            try (java.io.FileWriter out = new java.io.FileWriter(new File(small, "picker_error.jpg"))) {
                out.write(trace.toString());
            }
            throw new AssertionError(frames.toString(), failure);
        }
        Dialog dialog = ShadowDialog.getLatestDialog();
        if (dialog == null || dialog.getWindow() == null) {
            throw new AssertionError("Lamp Slide picker dialog did not open");
        }
        // Render the picker's own content (its scroll view), not the dialog's decor view, which fails to lay out here.
        View content = dialog.getWindow().getDecorView().findViewById(android.R.id.content);
        View picker = content instanceof android.view.ViewGroup && ((android.view.ViewGroup) content).getChildCount() > 0
                ? ((android.view.ViewGroup) content).getChildAt(0) : content;
        int w = UiScreenshotSupport.WIDTH_PX;
        int h = Math.min(4000, UiScreenshotSupport.HEIGHT_PX * 2);
        picker.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.max(1, Math.min(h, picker.getMeasuredHeight()));
        picker.layout(0, 0, w, height);
        Bitmap bitmap = Bitmap.createBitmap(w, height, Bitmap.Config.ARGB_8888);
        picker.draw(new Canvas(bitmap));
        File outDir = new File("build/ui-screenshots");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        UiScreenshotSupport.writePng(bitmap, new File(outDir, "light_7_lamp_picker.png"));
        dialog.dismiss();
    }
}
