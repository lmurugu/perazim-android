package org.perazimchurch.app.ui.components;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
            // Relaunch from an alarm, so the app starts only after this process has exited. Starting the
            // activity in the old process and killing it moments later raced the new screen against the exit.
            int flags = PendingIntent.FLAG_CANCEL_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent relaunch = PendingIntent.getActivity(activity, 0, launch, flags);
            AlarmManager alarms = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (alarms != null) {
                alarms.set(AlarmManager.RTC, System.currentTimeMillis() + 300, relaunch);
            }
        }
        activity.finishAffinity();
        // A fresh process makes sure every screen re-reads the theme colors.
        new Handler(Looper.getMainLooper()).postDelayed(() -> Runtime.getRuntime().exit(0), 400);
    }
}
