package org.perazimchurch.app.ui.components;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/** Flips between dark (purple frosted glass) and light mode, then restarts the app UI so every screen picks it up. */
public final class ThemeSwitcher {

    private ThemeSwitcher() {
    }

    public static void toggle(Activity activity) {
        PerazimTheme.saveMode(activity, !PerazimTheme.isLight());
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(launch);
        }
        activity.finishAffinity();
        // A fresh process makes sure every screen re-reads the theme colors.
        new Handler(Looper.getMainLooper()).postDelayed(() -> Runtime.getRuntime().exit(0), 250);
    }
}
