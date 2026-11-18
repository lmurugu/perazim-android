package org.perazimchurch.app.screenshots;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.perazimchurch.app.MainActivity;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.ui.theme.PerazimTheme;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.robolectric.Shadows.shadowOf;

/**
 * Design-review helper: renders each main tab to PNG so layout changes can be checked without a phone.
 * Only runs when the build passes -Dperazim.screenshots=true (see the "UI screenshots" CI job).
 */
final class UiScreenshotSupport {

    static final int WIDTH_PX = 1080;
    static final int HEIGHT_PX = 2340;
    private static final String[] TAB_NAMES = {"home", "sermons", "worship", "community", "give", "profile"};

    private UiScreenshotSupport() {
    }

    static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty("perazim.screenshots", "false"));
    }

    static void captureAllTabs(boolean light) throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx);
        PerazimDatabase db = Room.inMemoryDatabaseBuilder(ctx, PerazimDatabase.class)
                .allowMainThreadQueries()
                .build();
        Field instance = PerazimDatabase.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, db);

        PerazimTheme.saveMode(ctx, light);
        PerazimTheme.applyMode(light);

        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        idle();

        Field splash = MainActivity.class.getDeclaredField("splashContainer");
        splash.setAccessible(true);
        View splashView = (View) splash.get(activity);
        if (splashView != null) {
            splashView.setVisibility(View.GONE);
        }

        Method switchTab = MainActivity.class.getDeclaredMethod("switchTab", int.class);
        switchTab.setAccessible(true);
        Field tabViewsField = MainActivity.class.getDeclaredField("tabViews");
        tabViewsField.setAccessible(true);

        String mode = light ? "light" : "dark";
        File outDir = new File("build/ui-screenshots");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();

        for (int i = TAB_NAMES.length - 1; i >= 0; i--) {
            switchTab.invoke(activity, i);
            idle();
            String name = mode + "_" + i + "_" + TAB_NAMES[i];
            writePng(renderWindow(activity), new File(outDir, name + ".png"));
            View[] tabViews = (View[]) tabViewsField.get(activity);
            if (tabViews != null && tabViews[i] instanceof ViewGroup
                    && ((ViewGroup) tabViews[i]).getChildCount() > 0) {
                writePng(renderFullLength(((ViewGroup) tabViews[i]).getChildAt(0), activity),
                        new File(outDir, name + "_full.png"));
            }
        }
        controller.pause().stop().destroy();
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static Bitmap renderWindow(Activity activity) {
        View root = activity.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, WIDTH_PX, HEIGHT_PX);
        Bitmap bitmap = Bitmap.createBitmap(WIDTH_PX, HEIGHT_PX, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        return bitmap;
    }

    /** Draws a scroll view's whole content on the page background, for reviewing long screens. */
    private static Bitmap renderFullLength(View content, Activity activity) {
        content.measure(View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int h = Math.max(1, Math.min(content.getMeasuredHeight(), 12000));
        content.layout(0, 0, WIDTH_PX, h);
        Bitmap bitmap = Bitmap.createBitmap(WIDTH_PX, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        android.graphics.drawable.Drawable bg = PerazimTheme.createBackgroundGradient();
        bg.setBounds(0, 0, WIDTH_PX, h);
        bg.draw(canvas);
        content.draw(canvas);
        // Put the live layout back for the window render.
        activity.getWindow().getDecorView().requestLayout();
        return bitmap;
    }

    private static void writePng(Bitmap bitmap, File file) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
