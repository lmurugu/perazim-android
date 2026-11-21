package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.mosaic.MosaicStars;

import static org.junit.Assert.assertEquals;

public class MosaicStarsTest {
    @Test public void noWrongDropsIsThreeStars() {
        assertEquals(3, MosaicStars.starsFor(9, 9));
    }

    @Test public void atMostOneInThreeWrongIsTwoStars() {
        assertEquals(2, MosaicStars.starsFor(8, 9)); // 1 wrong of 9
        assertEquals(2, MosaicStars.starsFor(6, 9)); // 3 wrong of 9, exactly a third
    }

    @Test public void moreThanAThirdWrongIsOneStar() {
        assertEquals(1, MosaicStars.starsFor(5, 9)); // 4 wrong of 9
        assertEquals(1, MosaicStars.starsFor(0, 9));
    }

    @Test public void scoreOutsideRangeIsClamped() {
        assertEquals(3, MosaicStars.starsFor(20, 9));
        assertEquals(1, MosaicStars.starsFor(-4, 9));
    }
}
