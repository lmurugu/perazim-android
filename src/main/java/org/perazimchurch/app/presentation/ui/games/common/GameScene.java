package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.io.InputStream;

/**
 * Illustrated background for a game world. Loads a compressed scene from assets/games/scenes/ and
 * dims it in dark mode with a tint so buttons and cards stay readable. Falls back to the app's glow
 * background when the scene is missing, so no game ever shows a blank page.
 */
public final class GameScene {

    private GameScene() { }

    /** {@code name} is the scene file without extension, for example "vine_scene". */
    public static Drawable background(Activity activity, String name) {
        Drawable fallback = PerazimTheme.createBackgroundGradient();
        try (InputStream in = activity.getAssets().open("games/scenes/" + name + ".webp")) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null) return fallback;
            BitmapDrawable art = new BitmapDrawable(activity.getResources(), bitmap);
            // Light mode gets a soft cream wash over the art; dark mode gets a deeper purple wash.
            int wash = PerazimTheme.isLight() ? 0x33FFF6EB : 0x66140A26;
            return new LayerDrawable(new Drawable[]{art, new android.graphics.drawable.ColorDrawable(wash)});
        } catch (Exception missing) {
            return fallback;
        }
    }
}
