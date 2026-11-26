package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The person's app size, text size and font choices. All three apply on the next screen build, and they persist.
 *
 * <p>The text scales are already smaller than the first release. "Phone setting" follows the phone's own text
 * size at 90%, so the app no longer fills the screen with large type on a default phone.
 */
public final class DisplaySettings {

    private static final String PREFS = "perazim_display";
    private static final String KEY_APP = "app_size";
    private static final String KEY_TEXT = "text_size";
    private static final String KEY_FONT = "font_choice";

    public static final String[] APP_LABELS = {"Small", "Default", "Large", "Larger"};
    private static final float[] APP_SCALES = {0.85f, 1f, 1.12f, 1.25f};
    public static final int APP_DEFAULT = 1;

    /** 0 follows the phone's own text setting, scaled by {@link #PHONE_TEXT_FACTOR}. */
    public static final String[] TEXT_LABELS = {"Phone setting", "Small", "Large", "Larger"};
    private static final float[] TEXT_SCALES = {0f, 0.85f, 1f, 1.15f};
    /** The share of the phone's own text size used by "Phone setting". */
    public static final float PHONE_TEXT_FACTOR = 0.9f;

    /** The fonts the person can pick. Index 0 is the phone's own font (Roboto on most phones). */
    public static final String[] FONT_LABELS = {
            "Phone font", "Inter", "Poppins", "Montserrat", "Lato", "Nunito",
            "Merriweather", "Playfair Display", "Open Sans"};
    /** Asset file stems under assets/fonts (each has a -regular.ttf and -bold.ttf). Index 0 has no file. */
    public static final String[] FONT_FILES = {
            null, "inter", "poppins", "montserrat", "lato", "nunito",
            "merriweather", "playfair", "opensans"};

    private DisplaySettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int appSize(Context context) {
        int v = prefs(context).getInt(KEY_APP, APP_DEFAULT);
        return v >= 0 && v < APP_SCALES.length ? v : APP_DEFAULT;
    }

    public static int textSize(Context context) {
        int v = prefs(context).getInt(KEY_TEXT, 0);
        return v >= 0 && v < TEXT_SCALES.length ? v : 0;
    }

    public static int fontChoice(Context context) {
        int v = prefs(context).getInt(KEY_FONT, 0);
        return v >= 0 && v < FONT_FILES.length ? v : 0;
    }

    public static void saveAppSize(Context context, int index) {
        prefs(context).edit().putInt(KEY_APP, index).apply();
    }

    public static void saveTextSize(Context context, int index) {
        prefs(context).edit().putInt(KEY_TEXT, index).apply();
    }

    public static void saveFontChoice(Context context, int index) {
        prefs(context).edit().putInt(KEY_FONT, index).apply();
        AppFonts.forget();
    }

    public static float appScale(int index) {
        return APP_SCALES[index];
    }

    /** The fixed text scale for a choice. 0 means "Phone setting", which the caller resolves. */
    public static float fontScale(int index) {
        return TEXT_SCALES[index];
    }
}
