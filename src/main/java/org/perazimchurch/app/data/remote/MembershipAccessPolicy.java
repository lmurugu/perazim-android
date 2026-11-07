package org.perazimchurch.app.data.remote;

import java.util.Collection;

/** Client presentation gate for a just-fetched permission response; the server remains authoritative. */
public final class MembershipAccessPolicy {
    private MembershipAccessPolicy() {}

    public static boolean permits(Collection<Grant> grants, String code, String exactResourceScope, long serverTime) {
        for (Grant grant : grants) {
            if (grant == null || !code.equals(grant.code) || grant.effectiveAt > serverTime || grant.expiresAt <= serverTime) continue;
            if ("global".equals(grant.scope) || exactResourceScope != null && exactResourceScope.equals(grant.scope)) return true;
        }
        // The flattened permissions list loses scope, so it is intentionally absent from this policy.
        return false;
    }

    public static final class Grant {
        public final String code, scope;
        public final long effectiveAt, expiresAt;
        public Grant(String code, String scope, long effectiveAt, long expiresAt) {
            this.code = code; this.scope = scope; this.effectiveAt = effectiveAt; this.expiresAt = expiresAt;
        }
    }
}
