package org.perazimchurch.app.domain.games.lampslide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.catalog.LampSlideDefinition;

import java.util.Collections;
import java.util.List;

public class LampSlideLevelsAndSessionTest {

    private static final LampSlideScene SCENE = new LampSlideScene("test", "game_art_quiz",
            "Psalm 23:1", "The LORD is my shepherd; I shall not want.", "");

    @Test
    public void boardSizeFollowsTheSpecBands() {
        assertEquals(3, LampSlideLevels.boardSize(1));
        assertEquals(3, LampSlideLevels.boardSize(10));
        assertEquals(4, LampSlideLevels.boardSize(11));
        assertEquals(4, LampSlideLevels.boardSize(25));
        assertEquals(5, LampSlideLevels.boardSize(26));
        assertEquals(5, LampSlideLevels.boardSize(40));
        assertEquals(40, LampSlideLevels.LEVEL_COUNT);
    }

    @Test
    public void levelCountHasAtLeastThirtyLevels() {
        assertTrue(new LampSlideDefinition(0, Collections::emptyList).levelCount() >= 30);
    }

    @Test
    public void sessionSizeIsParAndPositionCapsAtPar() {
        LampSlideSession session = new LampSlideSession(LampSlidePuzzle.scrambled(3, 1), SCENE);
        assertEquals(30, session.size());
        assertEquals(1, session.position());
        assertEquals(GameSession.NO_LIVES, session.lives());
        assertFalse(session.finished());
        assertEquals(0, session.score());
    }

    @Test
    public void legalSlideReturnsMovedAndIllegalReturnsIgnored() {
        LampSlideSession session = new LampSlideSession(
                LampSlidePuzzle.ofBoard(3, new int[] {1, 2, 3, 4, 5, 6, 7, 0, 8}, 2), SCENE);
        assertEquals(GameSession.Outcome.IGNORED, session.answer("up"));
        assertEquals(GameSession.Outcome.IGNORED, session.answer("sideways"));
        assertEquals(GameSession.Outcome.MOVED, session.answer("right"));
        assertEquals(GameSession.Outcome.MOVED, session.answer("left"));
        assertFalse(session.finished());
        assertEquals(0, session.score());
    }

    @Test
    public void hintLowersStarsAndNullWhenSolved() {
        LampSlideSession session = new LampSlideSession(
                LampSlidePuzzle.ofBoard(3, new int[] {1, 2, 3, 4, 5, 6, 7, 0, 8}, 1), SCENE);
        session.recordPeek();
        assertEquals(1, session.hintsUsed());
        assertTrue(session.hintAvailable());
        assertEquals(GameSession.Outcome.CORRECT, session.answer("left"));
        assertEquals(2, session.score());
        assertNull(session.hint());
    }

    @Test
    public void definitionUsesSceneListAndFallsBackWhenEmpty() {
        LampSlideDefinition empty = new LampSlideDefinition(0, Collections::emptyList);
        GameSession session = empty.createSession(1, 5L);
        assertTrue(session.size() > 0);

        List<LampSlideScene> one = Collections.singletonList(SCENE);
        LampSlideDefinition with = new LampSlideDefinition(0, () -> one);
        assertEquals("lamp_slide", with.id());
        assertTrue(with.createSession(12, 9L).size() > 0);
    }
}
