package org.perazimchurch.app.data.remote;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MembershipAccessPolicyTest {
    @Test public void permissionsAreServerSnapshotAndExactScopeOnly() {
        List<MembershipAccessPolicy.Grant> grants=Arrays.asList(
                new MembershipAccessPolicy.Grant("membership.record.read","global",500,Long.MAX_VALUE),
                new MembershipAccessPolicy.Grant("membership.claim.review","claim-1",500,Long.MAX_VALUE),
                new MembershipAccessPolicy.Grant("membership.verification.revoke","user-1",500,999),
                new MembershipAccessPolicy.Grant("membership.verification.confirm","claim-2",1001,Long.MAX_VALUE));
        assertTrue(MembershipAccessPolicy.permits(grants,"membership.record.read",null,1000));
        assertTrue(MembershipAccessPolicy.permits(grants,"membership.claim.review","claim-1",1000));
        assertFalse(MembershipAccessPolicy.permits(grants,"membership.claim.review","claim-2",1000));
        assertFalse(MembershipAccessPolicy.permits(grants,"membership.verification.revoke","user-1",1000));
        assertFalse(MembershipAccessPolicy.permits(grants,"membership.verification.confirm","claim-2",1000));
        assertFalse(MembershipAccessPolicy.permits(grants,"membership.claim.review",null,1000));
        assertFalse(MembershipAccessPolicy.permits(Collections.singletonList(new MembershipAccessPolicy.Grant("membership.record.read","record-1",500,Long.MAX_VALUE)),"membership.record.read",null,1000));
    }

    @Test public void ordinaryRoleOrActivityCannotCreatePermission() {
        assertFalse(MembershipAccessPolicy.permits(Collections.emptyList(),"membership.claim.review",null,1000));
    }
}
