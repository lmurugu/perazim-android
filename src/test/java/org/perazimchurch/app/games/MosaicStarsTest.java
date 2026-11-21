package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.mosaic.MosaicStars;

import static org.junit.Assert.assertEquals;

public class MosaicStarsTest {
    @Test public void noWrongDropsIsThreeStars() {
        assertEquals(3, MosaicStars.RULE.stars(9, 9, -1));
    }

    @Test public void atMostOneInThreeWrongIsTwoStars() {
        assertEquals(2, MosaicStars.RULE.stars(8, 9, -1)); // 1 wrong of 9
        assertEquals(2, MosaicStars.RULE.stars(6, 9, -1)); // 3 wrong of 9, exactly a third
    }

    @Test public void moreThanAThirdWrongIsOneStar() {
        assertEquals(1, MosaicStars.RULE.stars(5, 9, -1)); // 4 wrong of 9
        assertEquals(1, MosaicStars.RULE.stars(0, 9, -1));
    }

    @Test public void scoreOutsideRangeIsClamped() {
        assertEquals(3, MosaicStars.RULE.stars(20, 9, -1));
        assertEquals(1, MosaicStars.RULE.stars(-4, 9, -1));
    }
}
