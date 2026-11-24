package org.perazimchurch.app;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

/** Guard against accidentally restoring unreviewed client-side services. */
public class ReleaseFeatureGateTest {
    @Test public void remoteFeaturesStayDisabledAndQwenIsValidationOnly() {
        assertFalse(BuildConfig.REMOTE_SHEET_FEEDS_ENABLED);
        assertFalse(BuildConfig.ONLINE_AI_ENABLED);
        // On-phone Qwen is debug/validation-only: release must keep it off until gate L7 passes.
        if ("release".equals(BuildConfig.BUILD_TYPE)) {
            assertFalse(BuildConfig.OFFLINE_QWEN_ENABLED);
        }
    }
}
