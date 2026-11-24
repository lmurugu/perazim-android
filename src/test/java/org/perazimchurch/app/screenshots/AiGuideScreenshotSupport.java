package org.perazimchurch.app.screenshots;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.perazimchurch.app.presentation.ui.AiBreakthroughGuideDialog;
import org.perazimchurch.app.ui.theme.GlassPolish;
import org.perazimchurch.app.ui.theme.PerazimTheme;
import org.robolectric.Robolectric;

import java.io.File;
import java.io.FileOutputStream;

import static org.robolectric.Shadows.shadowOf;

/** Shared renderer for the Ask Perazim Guide screenshot tests. */
final class AiGuideScreenshotSupport {
    static final int WIDTH_PX = 1080;
    static final int HEIGHT_PX = 2340;

    private AiGuideScreenshotSupport() {
    }

    static void capture(boolean light, String fileName) throws Exception {
        PerazimTheme.saveChoice(org.robolectric.RuntimeEnvironment.getApplication(),
                light ? PerazimTheme.CHOICE_LIGHT : PerazimTheme.CHOICE_DARK);
        PerazimTheme.applyMode(light);

        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().visible().get();
        AiBreakthroughGuideDialog dialog = new AiBreakthroughGuideDialog(activity, new AiBreakthroughGuideDialog.AiActionListener() {
            public void onNavigateTab(int tabIndex) { }
            public void onOpenGiving() { }
            public void onOpenLiveSermons() { }
            public void onOpenGames() { }
            public void onOpenProfile() { }
            public void onOpenBible(String book) { }
            public void onLaunchRiddles() { }
            public void onLaunchJokes() { }
            public void onStartReflections() { }
        });
        dialog.show();
        shadowOf(android.os.Looper.getMainLooper()).idle();

        View root = dialog.getWindow().getDecorView();
        for (int pass = 0; pass < 2; pass++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, WIDTH_PX, HEIGHT_PX);
            GlassPolish.apply(root);
        }
        Bitmap bitmap = Bitmap.createBitmap(WIDTH_PX, HEIGHT_PX, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));

        File outDir = new File("build/ui-screenshots");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(outDir, fileName))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        dialog.dismiss();
    }
}
