package org.perazimchurch.app.data.local.session;

import org.junit.Test;

import static org.junit.Assert.*;

public class MembershipSnapshotCacheTest {
    @Test public void cacheIsIsolatedByAccountAndNormalizedEndpoint() {
        MembershipSnapshotCache.clear();
        MembershipSnapshotCache.put("account-a", "HTTPS://EXAMPLE.org:443/api/", "VERIFICATION_PENDING", 1234L);
        assertEquals("VERIFICATION_PENDING", MembershipSnapshotCache.get("account-a", "https://example.org/api").status);
        assertNull(MembershipSnapshotCache.get("account-b", "https://example.org/api"));
        assertNull(MembershipSnapshotCache.get("account-a", "https://other.example/api"));
        MembershipSnapshotCache.clear();
        assertNull(MembershipSnapshotCache.get("account-a", "https://example.org/api"));
    }
}
