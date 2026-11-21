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
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityStarted(android.app.Activity a) {
                org.perazimchurch.app.ui.theme.GlassPolish.install(a);
            }
            @Override public void onActivityResumed(android.app.Activity a) { }
            @Override public void onActivityPaused(android.app.Activity a) { }
            @Override public void onActivityStopped(android.app.Activity a) { }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
    }
}
