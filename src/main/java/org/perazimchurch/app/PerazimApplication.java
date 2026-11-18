package org.perazimchurch.app;

import android.app.Application;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/** Loads the saved light/dark mode before any screen is built. */
public class PerazimApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PerazimTheme.init(this);
    }
}
