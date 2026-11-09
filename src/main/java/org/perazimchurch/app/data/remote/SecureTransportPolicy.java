package org.perazimchurch.app.data.remote;

import java.net.URL;

/** Pure policy for requiring HTTPS and limiting cleartext to validation loopback. */
public final class SecureTransportPolicy {
    private SecureTransportPolicy() {}

    public static boolean requiresSecureTransport(String path, boolean carriesCredentials) {
        return true;
    }

    public static boolean allowsProtectedTransport(URL url, String buildType) {
        if (url.getUserInfo() != null) return false;
        if ("https".equalsIgnoreCase(url.getProtocol())) return true;
        if (!"validation".equals(buildType) || !"http".equalsIgnoreCase(url.getProtocol())) return false;
        String host = url.getHost();
        return "127.0.0.1".equals(host) || "[::1]".equals(host) || "localhost".equalsIgnoreCase(host);
    }
}
