package org.perazimchurch.app.community.privacy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommunityPrivacyHelperTest {
    @Test public void reportLogContainsOnlyNonIdentifyingEvent() {
        assertEquals("Member safety report submitted", CommunityPrivacyHelper.reportLogMessage());
    }
}
