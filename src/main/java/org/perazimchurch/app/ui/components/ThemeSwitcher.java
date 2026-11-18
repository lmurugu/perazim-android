package org.perazimchurch.app.ui.components;

import android.app.Activity;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Flips between dark (purple frosted glass) and light mode and rebuilds the current screen in place.
 *
 * <p>The theme colours are read at the moment each view is built, so applying the new mode and recreating the
 * activity is enough. The app never exits, so the switch cannot look like a crash.
 */
public final class ThemeSwitcher {

    private ThemeSwitcher() {
    }

    public static void toggle(Activity activity) {
        boolean light = !PerazimTheme.isLight();
        PerazimTheme.saveMode(activity, light);
        PerazimTheme.applyMode(light);
        activity.recreate();
    }
}
