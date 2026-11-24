package org.perazimchurch.app.domain.games.engine;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StarRatingTest {

    @Test
    public void perfectRunEarnsThreeStars() {
        assertEquals(3, StarRating.stars(5, 5, GameSession.NO_LIVES));
    }

    @Test
    public void sixtyPercentEarnsTwoStars() {
        assertEquals(2, StarRating.stars(3, 5, GameSession.NO_LIVES));
        assertEquals(2, StarRating.stars(4, 5, GameSession.NO_LIVES));
    }

    @Test
    public void belowSixtyPercentEarnsOneStar() {
        assertEquals(1, StarRating.stars(2, 5, GameSession.NO_LIVES));
        assertEquals(1, StarRating.stars(0, 5, GameSession.NO_LIVES));
    }

    @Test
    public void runOutOfLivesCapsAtTwoStars() {
        assertEquals(2, StarRating.stars(5, 5, 0));
    }

    @Test
    public void livesLeftDoNotChangeStars() {
        assertEquals(3, StarRating.stars(5, 5, 2));
    }

    @Test
    public void scoreOutsideRangeIsClamped() {
        assertEquals(3, StarRating.stars(9, 5, GameSession.NO_LIVES));
        assertEquals(1, StarRating.stars(-2, 5, GameSession.NO_LIVES));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyRunIsRejected() {
        StarRating.stars(0, 0, GameSession.NO_LIVES);
    }
}
