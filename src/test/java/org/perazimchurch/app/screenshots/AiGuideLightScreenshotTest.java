package org.perazimchurch.app.screenshots;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.presentation.ui.AiBreakthroughGuideDialog;
import org.perazimchurch.app.ui.theme.GlassPolish;
import org.perazimchurch.app.ui.theme.PerazimTheme;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import static org.robolectric.Shadows.shadowOf;

/** Light-mode render of the Ask Perazim Guide sheet. A separate class gives this mode its own theme static fields. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AiGuideLightScreenshotTest {
    @Test
    public void captureGuideLight() throws Exception {
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("perazim.screenshots", "false")));
        AiGuideScreenshotSupport.capture(true, "light_ai_guide.png");
    }
}
