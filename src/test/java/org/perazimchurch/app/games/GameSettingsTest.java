package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.GameSettings;

import static org.junit.Assert.assertEquals;

public class GameSettingsTest {
    @Test public void defaultsMatchTheAgreedValues() {
        GameSettings d = GameSettings.defaults();
        assertEquals(50, d.dailyStarCap);
        assertEquals(50, d.unseenPercent);
        assertEquals(30, d.missedPercent);
    }

    @Test public void missingValuesUseDefaults() {
        GameSettings s = GameSettings.of(null, null, null);
        assertEquals(50, s.dailyStarCap);
        assertEquals(30, s.missedPercent);
    }

    @Test public void validValuesAreKept() {
        GameSettings s = GameSettings.of(80, 40, 20);
        assertEquals(80, s.dailyStarCap);
        assertEquals(40, s.unseenPercent);
        assertEquals(20, s.missedPercent);
    }

    @Test public void outOfRangeCapFallsBack() {
        assertEquals(50, GameSettings.of(999, null, null).dailyStarCap);
        assertEquals(50, GameSettings.of(-1, null, null).dailyStarCap);
    }

    @Test public void sharesOverOneHundredFallBackTogether() {
        GameSettings s = GameSettings.of(60, 70, 60);
        assertEquals(50, s.unseenPercent);
        assertEquals(30, s.missedPercent);
        assertEquals(60, s.dailyStarCap);
    }
}
