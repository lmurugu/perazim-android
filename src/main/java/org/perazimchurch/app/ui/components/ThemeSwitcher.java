package org.perazimchurch.app.ui.components;

import org.perazimchurch.app.ui.theme.AppDialogs;

import android.app.Activity;
import android.app.AlertDialog;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Lets the person pick System (follows the phone), Light or Dark, and rebuilds the current screen in place.
 *
 * <p>The theme colours are read when each view is built, so applying the new mode and recreating the activity is
 * enough. The app never exits, so a switch cannot look like a crash.
 */
public final class ThemeSwitcher {

    private ThemeSwitcher() {
    }

    public static void choose(Activity activity) {
        final int current = PerazimTheme.savedChoice(activity);
        String[] labels = {
                "System (follows your phone)",
                "Light glass",
                "Dark purple glass"
        };
        AppDialogs.builder(activity)
                .setTitle("Appearance")
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) {
                        PerazimTheme.saveChoice(activity, which);
                        PerazimTheme.applyPreference(activity);
                        activity.recreate();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
