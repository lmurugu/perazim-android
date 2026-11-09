package org.perazimchurch.app.presentation.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ProfileAvatarHelperTest {
    @Test
    public void storedDefaultReferenceIsPreserved() {
        assertEquals(1, ProfileAvatarHelper.numberFor("default:1", "synthetic-user-a"));
        assertEquals(20, ProfileAvatarHelper.numberFor("default:20", "synthetic-user-b"));
        assertEquals(9, ProfileAvatarHelper.numberFor("default:09", "synthetic-user-c"));
    }

    @Test
    public void missingOrInvalidReferenceHasStableUserSpecificDefault() {
        int first = ProfileAvatarHelper.numberFor("", "synthetic-user-a");
        assertEquals(first, ProfileAvatarHelper.numberFor(null, "synthetic-user-a"));
        assertEquals(first, ProfileAvatarHelper.numberFor("default:21", "synthetic-user-a"));
        assertTrue(first >= 1 && first <= ProfileAvatarHelper.COUNT);
        assertTrue(ProfileAvatarHelper.defaultNumber("synthetic-user-b") >= 1);
        assertTrue(ProfileAvatarHelper.defaultNumber("synthetic-user-b") <= ProfileAvatarHelper.COUNT);
    }
}
