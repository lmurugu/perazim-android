package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LevelsTest {

    @Test
    public void twelveLevelsInThreeBandsOfFour() {
        assertEquals(12, Levels.COUNT);
        assertEquals("easy", Levels.band(1));
        assertEquals("easy", Levels.band(4));
        assertEquals("medium", Levels.band(5));
        assertEquals("medium", Levels.band(8));
        assertEquals("hard", Levels.band(9));
        assertEquals("hard", Levels.band(12));
    }

    @Test
    public void outOfRangeLevelsAreClamped() {
        assertEquals(1, Levels.clamp(0));
        assertEquals(12, Levels.clamp(99));
        assertEquals("easy", Levels.band(-3));
        assertEquals("hard", Levels.band(40));
    }

    @Test
    public void eachLevelHasItsOwnFixedSeed() {
        assertEquals(Levels.seed(3), Levels.seed(3));
        assertNotEquals(Levels.seed(3), Levels.seed(4));
    }
}
