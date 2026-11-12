package org.perazimchurch.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Guard against accidentally restoring unreviewed client-side services. */
public class ReleaseFeatureGateTest {
    @Test public void remoteFeaturesStayDisabledAndQwenIsValidationOnly() {
        assertFalse(BuildConfig.REMOTE_SHEET_FEEDS_ENABLED);
        assertFalse(BuildConfig.ONLINE_AI_ENABLED);
        if ("validation".equals(BuildConfig.BUILD_TYPE)) {
            assertTrue(BuildConfig.OFFLINE_QWEN_ENABLED);
        } else {
            assertFalse(BuildConfig.OFFLINE_QWEN_ENABLED);
        }
    }
}
