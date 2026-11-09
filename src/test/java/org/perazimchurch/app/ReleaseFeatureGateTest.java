package org.perazimchurch.app;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

/** Guard against accidentally restoring unreviewed client-side services. */
public class ReleaseFeatureGateTest {
    @Test public void unverifiedRemoteFeaturesStayDisabledInEveryVariant() {
        assertFalse(BuildConfig.REMOTE_SHEET_FEEDS_ENABLED);
        assertFalse(BuildConfig.ONLINE_AI_ENABLED);
        assertFalse(BuildConfig.OFFLINE_QWEN_ENABLED);
    }
}
