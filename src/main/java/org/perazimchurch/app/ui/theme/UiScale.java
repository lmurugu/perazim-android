package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Sizes the app like the mockups on every phone.
 *
 * <p>The mockups are drawn for a phone about 390dp wide. Phones set to a small "display size" report far
 * more dp (one test phone reports about 600dp), so every card, button and word comes out small and the
 * screens look half empty. On phones only, this scales the activity's density so the screen is
 * {@link #MOCKUP_WIDTH_DP} wide: the layout then fills the screen in the same proportions as the mockups.
 * Tablets and phones already at or below the mockup width are left exactly as they are.
 */
public final class UiScale {

    /** Width the mockups were drawn at (390) plus a hair, in dp. */
    public static final float MOCKUP_WIDTH_DP = 392f;

    /** Anything physically wider than this (inches, short side) is treated as a tablet and left alone. */
    private static final float MAX_PHONE_WIDTH_INCHES = 3.7f;

    private UiScale() {
    }

    /**
     * The configuration override for an activity created on {@code base}, or null to keep the system's.
     * Use from {@code attachBaseContext}: {@code applyOverrideConfiguration(UiScale.override(base))}.
     */
    public static Configuration override(Context base) {
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
        float shortDp = shortPx / dm.density;
        float xdpi = dm.xdpi;
        float inches = (xdpi > 100f && xdpi < 1200f) ? shortPx / xdpi : shortDp / 160f;
        if (inches > MAX_PHONE_WIDTH_INCHES || shortDp <= MOCKUP_WIDTH_DP + 8f) {
            return null;
        }
        int dpi = Math.round(shortPx * 160f / MOCKUP_WIDTH_DP);
        Configuration c = new Configuration();
        c.fontScale = 0f; // undefined: keep the person's own text size setting
        c.densityDpi = dpi;
        boolean portrait = dm.widthPixels <= dm.heightPixels;
        int shortSide = Math.round(shortPx * 160f / dpi);
        int longSide = Math.round(longPx * 160f / dpi);
        c.screenWidthDp = portrait ? shortSide : longSide;
        c.screenHeightDp = portrait ? longSide : shortSide;
        c.smallestScreenWidthDp = shortSide;
        return c;
    }
}
