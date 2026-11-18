package org.perazimchurch.app;

import android.app.Application;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/** Loads the saved light/dark mode before any screen is built. */
public class PerazimApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PerazimTheme.init(this);
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
