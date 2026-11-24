package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreakTest {

    @Test
    public void rightAnswersGrowTheStreakAndWrongResetsIt() {
        Streak streak = new Streak();
        streak.record(true);
        streak.record(true);
        streak.record(true);
        assertEquals(3, streak.current());
        streak.record(false);
        assertEquals(0, streak.current());
        assertEquals("best is kept after a reset", 3, streak.best());
    }

    @Test
    public void growFlagFollowsTheLastAnswer() {
        Streak streak = new Streak();
        streak.record(true);
        assertTrue(streak.grew());
        streak.record(false);
        assertFalse(streak.grew());
    }

    @Test
    public void bonusIsAwardedAtFiveInARowOnly() {
        Streak streak = new Streak();
        for (int i = 1; i <= 4; i++) {
            streak.record(true);
            assertFalse(streak.bonusAwarded());
        }
        assertEquals(0, streak.bonusPoints());
        streak.record(true);
        assertTrue(streak.bonusAwarded());
        assertEquals(Streak.BONUS_POINTS, streak.bonusPoints());
    }

    @Test
    public void bonusRepeatsEveryFiveAndWrongAnswerBreaksTheCount() {
        Streak streak = new Streak();
        for (int i = 0; i < 10; i++) streak.record(true);
        assertEquals(2 * Streak.BONUS_POINTS, streak.bonusPoints());
        streak.record(false);
        for (int i = 0; i < 4; i++) streak.record(true);
        assertEquals("four right after a reset earns no new bonus", 2 * Streak.BONUS_POINTS, streak.bonusPoints());
        assertFalse(streak.bonusAwarded());
    }
}
