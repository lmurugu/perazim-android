package org.perazimchurch.app.data.local.session;

import androidx.annotation.Nullable;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small, process-only display cache. It is never used to authorize an operation. */
public final class MembershipSnapshotCache {
    private static final int MAX_ENTRIES = 12;
    private static final Map<String, Snapshot> VALUES = new LinkedHashMap<String, Snapshot>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Snapshot> eldest) { return size() > MAX_ENTRIES; }
    };

    private MembershipSnapshotCache() {}

    public static synchronized void put(String accountId, String endpoint, String status, long serverTime) {
        VALUES.put(key(accountId, endpoint), new Snapshot(status, serverTime));
    }

    @Nullable public static synchronized Snapshot get(String accountId, String endpoint) {
        return VALUES.get(key(accountId, endpoint));
    }

    public static synchronized void clear() { VALUES.clear(); }

    public static String key(String accountId, String endpoint) {
        return String.valueOf(accountId) + "\n" + normalizeEndpoint(endpoint);
    }

    public static String normalizeEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint.trim());
            int port = uri.getPort();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(java.util.Locale.ROOT);
            boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            String authority = host + (port >= 0 && !defaultPort ? ":" + port : "");
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            return scheme + "://" + authority + path;
        } catch (Exception ignored) {
            return endpoint == null ? "" : endpoint.trim().replaceAll("/+$", "");
        }
    }

    public static final class Snapshot {
        public final String status;
        public final long serverTime;
        Snapshot(String status, long serverTime) { this.status = status; this.serverTime = serverTime; }
    }
}
