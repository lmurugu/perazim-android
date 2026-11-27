package org.perazimchurch.app.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PrivacyPresentationPolicyTest {
    private static final String NO_NOTICE = PrivacyPresentationPolicy.UNPUBLISHED_NOTICE_ERROR;
    private static final String LOOPBACK = "http://127.0.0.1:3000";

    private static boolean allow(String buildType, String baseUrl, int code, String error) {
        return PrivacyPresentationPolicy.allowsValidationWithoutNotice(buildType, baseUrl, code, error);
    }

    @Test public void validationLoopbackWithTheExactNoNoticeAnswerIsAllowed() {
        assertTrue(allow("validation", LOOPBACK, 503, NO_NOTICE));
        assertTrue(allow("validation", "http://localhost:3000", 503, NO_NOTICE));
        assertTrue(allow("validation", "http://[::1]:3000", 503, NO_NOTICE));
        assertTrue(allow("validation", "https://127.0.0.1:3000", 503, NO_NOTICE));
    }

    @Test public void releaseAndDebugNeverAllow() {
        assertFalse(allow("release", LOOPBACK, 503, NO_NOTICE));
        assertFalse(allow("debug", LOOPBACK, 503, NO_NOTICE));
        assertFalse(allow(null, LOOPBACK, 503, NO_NOTICE));
    }

    @Test public void remoteUrlsAreRefusedEvenForValidation() {
        assertFalse(allow("validation", "https://api.perazimchurch.example", 503, NO_NOTICE));
        assertFalse(allow("validation", "http://10.0.2.2:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "http://192.168.1.20:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "https://pilot.trycloudflare.com", 503, NO_NOTICE));
        assertFalse(allow("validation", "", 503, NO_NOTICE));
        assertFalse(allow("validation", null, 503, NO_NOTICE));
    }

    @Test public void lookalikeLoopbackHostsAreRefused() {
        assertFalse(allow("validation", "http://127.0.0.2:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "http://localhost.example.com:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "http://127.0.0.1.nip.io:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "http://127.0.0.1@evil.example:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "ftp://127.0.0.1:3000", 503, NO_NOTICE));
        assertFalse(allow("validation", "not a url", 503, NO_NOTICE));
    }

    @Test public void otherFailuresAreRefusedEvenOnLoopback() {
        assertFalse(allow("validation", LOOPBACK, 500, NO_NOTICE));
        assertFalse(allow("validation", LOOPBACK, 200, NO_NOTICE));
        assertFalse(allow("validation", LOOPBACK, -1, NO_NOTICE));
        assertFalse(allow("validation", LOOPBACK, 503, "Request failed with HTTP 503"));
        assertFalse(allow("validation", LOOPBACK, 503, "Privacy acknowledgement status is temporarily unavailable."));
        assertFalse(allow("validation", LOOPBACK, 503, ""));
        assertFalse(allow("validation", LOOPBACK, 503, null));
    }
}
