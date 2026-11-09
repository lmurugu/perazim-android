package org.perazimchurch.app.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.security.NetworkSecurityPolicy;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.BuildConfig;

@RunWith(AndroidJUnit4.class)
public final class NetworkSecurityConfigTest {
    @Test
    public void cleartextIsRestrictedToLoopbackInValidationBuild() {
        assertTrue("instrumentation must use the validation build", "validation".equals(BuildConfig.BUILD_TYPE));
        NetworkSecurityPolicy policy = NetworkSecurityPolicy.getInstance();
        assertTrue(policy.isCleartextTrafficPermitted("localhost"));
        assertTrue(policy.isCleartextTrafficPermitted("127.0.0.1"));
        assertTrue(policy.isCleartextTrafficPermitted("::1"));
        assertFalse(policy.isCleartextTrafficPermitted("198.51.100.20"));
    }
}
