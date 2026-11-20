package org.perazimchurch.app.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** The Profile, Home HUD and offline assistant all read levels from SpiritualLevel. */
public class SpiritualLevelTest {

    @Test
    public void levelsFollowTheProfileRuleOfFiveHundredXpPerLevel() {
        assertEquals(1, SpiritualLevel.levelFor(0));
        assertEquals(1, SpiritualLevel.levelFor(499));
        assertEquals(2, SpiritualLevel.levelFor(500));
        assertEquals(3, SpiritualLevel.levelFor(1000));
        assertEquals(3, SpiritualLevel.levelFor(1499));
    }

    @Test
    public void progressAndRemainingAreConsistentAtEveryBoundary() {
        assertEquals(0, SpiritualLevel.xpIntoLevel(500));
        assertEquals(500, SpiritualLevel.xpToNextLevel(500));
        assertEquals(499, SpiritualLevel.xpIntoLevel(999));
        assertEquals(1, SpiritualLevel.xpToNextLevel(999));
        assertEquals(250, SpiritualLevel.xpIntoLevel(750));
        assertEquals(250, SpiritualLevel.xpToNextLevel(750));
    }

    @Test
    public void negativeXpIsTreatedAsZero() {
        assertEquals(1, SpiritualLevel.levelFor(-40));
        assertEquals(0, SpiritualLevel.xpIntoLevel(-40));
        assertEquals(500, SpiritualLevel.xpToNextLevel(-40));
    }
}
