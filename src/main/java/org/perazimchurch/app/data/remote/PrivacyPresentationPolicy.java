package org.perazimchurch.app.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Decides whether a failed privacy presentation may let a sign-in continue WITHOUT showing or
 * recording a notice. All of these must hold, and otherwise the caller keeps the sign-out:
 *   1. the build is the validation build;
 *   2. the API base URL is loopback only (127.0.0.1, localhost or [::1]; the USB adb-reverse path),
 *      with no user info, and not a remote host;
 *   3. the answer is HTTP 503 with the server's exact "no notice is published" error text
 *      (server/src/routes/privacy.js, POST /presentation).
 * Release and debug builds always keep the sign-out. No consent is recorded in any case.
 *
 * Pure Java, no Android types, so it can be unit-tested on the JVM.
 */
public final class PrivacyPresentationPolicy {
    /** Exact text of the server's 503 error when no privacy notice is PUBLISHED. */
    public static final String UNPUBLISHED_NOTICE_ERROR = "Published privacy document unavailable";
    public static final String VALIDATION_BUILD = "validation";

    /** Shown to the user for a validation session that skipped the notice. Says that nothing was consented to. */
    public static final String VALIDATION_BANNER =
            "Privacy notice not yet published. This is a validation session. No consent is recorded.";

    private PrivacyPresentationPolicy() {}

    public static boolean allowsValidationWithoutNotice(@Nullable String buildType, @Nullable String baseUrl,
                                                        int statusCode, @Nullable String errorMessage) {
        return VALIDATION_BUILD.equals(buildType)
                && isLoopbackBaseUrl(baseUrl)
                && statusCode == 503
                && UNPUBLISHED_NOTICE_ERROR.equals(errorMessage);
    }

    /** True only for http(s) URLs whose host is exactly 127.0.0.1, localhost or [::1], with no user info. */
    public static boolean isLoopbackBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) return false;
        try {
            URL url = new URL(baseUrl.trim());
            if (url.getUserInfo() != null) return false;
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) return false;
            String host = url.getHost();
            return "127.0.0.1".equals(host) || "[::1]".equals(host) || "localhost".equalsIgnoreCase(host);
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
