package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReferenceApprovalsTest {

    @Test
    public void emptyListApprovesNothing() {
        ReferenceApprovals approvals = ReferenceApprovals.parse("{\"approvedSourceIds\": []}");
        assertTrue(approvals.isEmpty());
        assertFalse(approvals.isApproved("smith-bible-dictionary"));
    }

    @Test
    public void approvedIdsAreReturned() {
        ReferenceApprovals approvals = ReferenceApprovals.parse(
                "{\"approvedSourceIds\": [\"smith-bible-dictionary\", \"openbible-xrefs\"]}");
        assertTrue(approvals.isApproved("smith-bible-dictionary"));
        assertTrue(approvals.isApproved("openbible-xrefs"));
        assertFalse(approvals.isApproved("easton-bible-dictionary"));
        assertEquals(2, approvals.ids().size());
    }

    @Test
    public void malformedFileFailsClosed() {
        assertTrue(ReferenceApprovals.parse("not json").isEmpty());
        assertTrue(ReferenceApprovals.parse("").isEmpty());
        assertTrue(ReferenceApprovals.parse(null).isEmpty());
        assertTrue(ReferenceApprovals.parse("{\"approvedSourceIds\": \"smith\"}").isEmpty());
        assertTrue(ReferenceApprovals.parse("{}").isEmpty());
    }

    @Test
    public void badIdsAreIgnored() {
        ReferenceApprovals approvals = ReferenceApprovals.parse(
                "{\"approvedSourceIds\": [\"../etc\", \"UPPER\", 7, null, \"ok-id\", \"\"]}");
        assertEquals(1, approvals.ids().size());
        assertTrue(approvals.isApproved("ok-id"));
    }
}
