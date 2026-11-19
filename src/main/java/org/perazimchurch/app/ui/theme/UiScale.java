package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Sizes the app the same way on every phone, and lets the person adjust it.
 *
 * <p>Every phone lays the screen out {@link #BASE_WIDTH_DP} dp wide, scaled by the person's chosen app size
 * (see {@link DisplaySettings}). A narrow phone therefore gets the same proportions as a wide one, and nothing
 * stretches or shrinks with the phone's own display-size setting. The text size is set separately. Tablets keep the
 * system's density, and the text size still applies.
 */
public final class UiScale {

    /** Width the layout is designed for at the standard app size, in dp. A notch smaller than the 392dp mockups. */
    public static final float BASE_WIDTH_DP = 420f;

    /** Anything physically wider than this (inches, short side) is treated as a tablet and keeps its density. */
    private static final float MAX_PHONE_WIDTH_INCHES = 3.7f;

    private UiScale() {
    }

    /** Wraps a base context so the activity is laid out at the chosen size. Use from {@code attachBaseContext}. */
    public static Context wrap(Context base) {
        Configuration c = configuration(base);
        return (base == null || c == null) ? base : base.createConfigurationContext(c);
    }

    /** The configuration override for the chosen sizes, or null when nothing changes. */
    public static Configuration configuration(Context base) {
        if (base == null) {
            return null;
        }
        Resources res = base.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        int shortPx = Math.min(dm.widthPixels, dm.heightPixels);
        int longPx = Math.max(dm.widthPixels, dm.heightPixels);
        if (shortPx <= 0 || dm.density <= 0f) {
            return null;
        }
        Context app = base.getApplicationContext() != null ? base.getApplicationContext() : base;
        Configuration c = new Configuration();
        boolean changed = false;

        // Text size: "Phone setting" follows the phone's own text size, scaled down so the app is not oversized.
        int textChoice = DisplaySettings.textSize(app);
        float fontScale = DisplaySettings.fontScale(textChoice);
        if (textChoice == 0) {
            float phoneScale = base.getResources().getConfiguration().fontScale;
            fontScale = (phoneScale > 0f ? phoneScale : 1f) * DisplaySettings.PHONE_TEXT_FACTOR;
        }
        c.fontScale = fontScale;
        changed = true;

        float shortDp = shortPx / dm.density;
        float xdpi = dm.xdpi;
        float inches = (xdpi > 100f && xdpi < 1200f) ? shortPx / xdpi : shortDp / 160f;
        if (inches <= MAX_PHONE_WIDTH_INCHES) {
            float width = BASE_WIDTH_DP / DisplaySettings.appScale(DisplaySettings.appSize(app));
            int dpi = Math.round(shortPx * 160f / width);
            boolean portrait = dm.widthPixels <= dm.heightPixels;
            int shortSide = Math.round(shortPx * 160f / dpi);
            int longSide = Math.round(longPx * 160f / dpi);
            c.densityDpi = dpi;
            c.screenWidthDp = portrait ? shortSide : longSide;
            c.screenHeightDp = portrait ? longSide : shortSide;
            c.smallestScreenWidthDp = shortSide;
            changed = true;
        }
        return changed ? c : null;
    }
}
