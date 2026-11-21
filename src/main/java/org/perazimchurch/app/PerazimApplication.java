package org.perazimchurch.app;

import android.app.Application;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/** Loads the saved appearance (System, Light or Dark) before any screen is built. */
public class PerazimApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        org.perazimchurch.app.presentation.ui.games.common.GameLaunchers.registerAll();
        PerazimTheme.init(this);
        org.perazimchurch.app.presentation.ui.games.vine.VineWordsContent.attach(this);
        org.perazimchurch.app.presentation.ui.games.lampslide.LampSlideScenes.init(this);
        // When the phone switches light or dark, follow it if the app is set to System. Activities that
        // are running recreate themselves, and they read the new mode when they are built.
        registerComponentCallbacks(new android.content.ComponentCallbacks() {
            @Override public void onConfigurationChanged(android.content.res.Configuration newConfig) {
                PerazimTheme.applyPreference(PerazimApplication.this);
            }
            @Override public void onLowMemory() { }
        });
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int startedActivities;
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityStarted(android.app.Activity a) {
                org.perazimchurch.app.ui.theme.GlassPolish.install(a);
                if (startedActivities++ == 0) {
                    org.perazimchurch.app.presentation.ui.games.common.GameAudio.get(a).onAppForegroundChanged(true);
                }
            }
            @Override public void onActivityResumed(android.app.Activity a) { }
            @Override public void onActivityPaused(android.app.Activity a) { }
            @Override public void onActivityStopped(android.app.Activity a) {
                // Music pauses only when no activity is visible any more, not when one screen replaces another.
                if (--startedActivities <= 0) {
                    startedActivities = 0;
                    org.perazimchurch.app.presentation.ui.games.common.GameAudio.get(a).onAppForegroundChanged(false);
                }
            }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
    }
}
