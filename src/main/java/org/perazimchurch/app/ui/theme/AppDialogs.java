package org.perazimchurch.app.ui.theme;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Builds AlertDialogs that follow the app's own Light / Dark setting.
 *
 * A plain AlertDialog.Builder picks the framework default theme, so its title bar and
 * buttons stay light when the app is set to Dark (and the reverse). Use this helper
 * everywhere instead of `new AlertDialog.Builder(context)`.
 */
public final class AppDialogs {

    private AppDialogs() {
    }

    /** Full window dialog theme (no action bar) that follows the app's Light / Dark setting. */
    public static int windowTheme() {
        return PerazimTheme.isLight()
                ? android.R.style.Theme_Material_Light_NoActionBar
                : android.R.style.Theme_Material_NoActionBar;
    }

    /** Full-screen dialog theme that follows the app's Light / Dark setting. */
    public static int fullscreenTheme() {
        return PerazimTheme.isLight()
                ? android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
                : android.R.style.Theme_Material_NoActionBar_Fullscreen;
    }

    public static AlertDialog.Builder builder(Context context) {
        int theme = PerazimTheme.isLight()
                ? android.R.style.Theme_Material_Light_Dialog
                : android.R.style.Theme_Material_Dialog;
        return new AlertDialog.Builder(context, theme);
    }
}
