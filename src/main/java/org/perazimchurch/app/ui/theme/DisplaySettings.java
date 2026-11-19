package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;

/** The person's app size and text size choices. Both apply on the next screen build, and they persist. */
public final class DisplaySettings {

    private static final String PREFS = "perazim_display";
    private static final String KEY_APP = "app_size";
    private static final String KEY_TEXT = "text_size";

    public static final String[] APP_LABELS = {"Small", "Default", "Large", "Larger"};
    private static final float[] APP_SCALES = {0.9f, 1f, 1.12f, 1.25f};
    public static final int APP_DEFAULT = 1;

    /** 0 keeps the phone's own text setting. */
    public static final String[] TEXT_LABELS = {"Phone setting", "Small", "Large", "Larger"};
    private static final float[] TEXT_SCALES = {0f, 0.9f, 1.15f, 1.3f};

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

    public static void saveAppSize(Context context, int index) {
        prefs(context).edit().putInt(KEY_APP, index).apply();
    }

    public static void saveTextSize(Context context, int index) {
        prefs(context).edit().putInt(KEY_TEXT, index).apply();
    }

    public static float appScale(int index) {
        return APP_SCALES[index];
    }

    public static float fontScale(int index) {
        return TEXT_SCALES[index];
    }
}
