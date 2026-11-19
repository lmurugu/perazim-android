package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.GameStreak;

import static org.junit.Assert.assertEquals;

public class GameStreakTest {
    @Test public void firstPlayStartsAtOne() { assertEquals(1, GameStreak.next(-1, 100, 0)); }
    @Test public void samedayKeepsStreak() { assertEquals(4, GameStreak.next(100, 100, 4)); }
    @Test public void nextDayAddsOne() { assertEquals(5, GameStreak.next(99, 100, 4)); }
    @Test public void gapResetsToOne() { assertEquals(1, GameStreak.next(97, 100, 4)); }
}
