package org.perazimchurch.app.screenshots;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Renders every main tab in dark mode. A separate SDK level gives this class its own class loader, so theme colours copied into static fields are fresh. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiScreenshotDarkTest {
    @Test
    public void captureTabs() throws Exception {
        Assume.assumeTrue(UiScreenshotSupport.enabled());
        try {
            UiScreenshotSupport.captureAllTabs(false);
        } catch (Throwable failure) {
            // Print the full trace: Gradle trims frames inside test failures, and the CI log needs the origin.
            failure.printStackTrace(System.out);
            throw failure;
        }
    }
}
