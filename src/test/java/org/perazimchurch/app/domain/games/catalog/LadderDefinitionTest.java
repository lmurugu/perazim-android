package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.GameSession.Outcome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LadderDefinitionTest {

    private final LadderDefinition definition = new LadderDefinition(0);

    @Test
    public void hasTwelveLevelsInThreeTiers() {
        assertEquals(12, definition.levelCount());
        assertEquals("Easy 1", definition.levelName(1));
        assertEquals("Easy 4", definition.levelName(4));
        assertEquals("Medium 1", definition.levelName(5));
        assertEquals("Hard 4", definition.levelName(12));
        assertEquals("Easy 1", definition.levelName(0));
        assertEquals("Hard 4", definition.levelName(99));
    }

    @Test
    public void tiersFollowTheLevelNumber() {
        assertEquals(LadderDefinition.Tier.EASY, LadderDefinition.tier(1));
        assertEquals(LadderDefinition.Tier.EASY, LadderDefinition.tier(4));
        assertEquals(LadderDefinition.Tier.MEDIUM, LadderDefinition.tier(5));
        assertEquals(LadderDefinition.Tier.MEDIUM, LadderDefinition.tier(8));
        assertEquals(LadderDefinition.Tier.HARD, LadderDefinition.tier(9));
        assertEquals(LadderDefinition.Tier.HARD, LadderDefinition.tier(12));
    }

    @Test
    public void everyTierMixFitsTwentyRungsAndIsDeliveredExactly() {
        for (LadderDefinition.Tier tier : LadderDefinition.Tier.values()) {
            int easy = LadderDefinition.easyRungsFor(tier);
            int medium = LadderDefinition.mediumRungsFor(tier);
            int hard = LadderOfFaith.RUNGS - easy - medium;
            assertTrue(easy >= 0 && medium >= 0 && hard >= 0);
            LadderOfFaith run = new LadderOfFaith(7L, easy, medium);
            assertEquals(easy, run.rungsOfTier(1));
            assertEquals(medium, run.rungsOfTier(2));
            assertEquals(hard, run.rungsOfTier(3));
        }
    }

    @Test
    public void hardLevelsHaveMostHardRungs() {
        assertTrue(LadderDefinition.easyRungsFor(LadderDefinition.Tier.HARD)
                < LadderDefinition.easyRungsFor(LadderDefinition.Tier.EASY));
    }

    @Test
    public void correctAnswersScoreWithComboBonus() {
        LadderOfFaith reference = new LadderOfFaith(42L, 10, 6);
        GameSession session = definition.createSession(1, 42L);

        assertEquals(Outcome.CORRECT, session.answer(reference.current().answer));
        assertEquals(10, session.score());
        reference.answer(reference.current().answer);
        reference.next();

        assertEquals(Outcome.CORRECT, session.answer(reference.current().answer));
        assertEquals(25, session.score());
    }

    @Test
    public void wrongAnswerCostsALifeAndResetsCombo() {
        LadderOfFaith reference = new LadderOfFaith(42L, 10, 6);
        GameSession session = definition.createSession(1, 42L);

        assertEquals(Outcome.WRONG, session.answer("no such answer at all"));
        assertEquals(LadderOfFaith.LIVES - 1, session.lives());
        assertEquals(0, session.score());
        reference.answer("no such answer at all");
        reference.next();

        assertEquals(Outcome.CORRECT, session.answer(reference.current().answer));
        assertEquals(10, session.score());
    }

    @Test
    public void hintIsTheVersePeekAndCostsPointsOnce() {
        LadderOfFaith reference = new LadderOfFaith(42L, 10, 6);
        GameSession session = definition.createSession(1, 42L);

        assertTrue(session.hintAvailable());
        assertEquals(reference.current().reference, session.hint());
        assertEquals(1, session.hintsUsed());
        assertFalse(session.hintAvailable());

        assertEquals(Outcome.CORRECT, session.answer(reference.current().answer));
        assertEquals(10 - 3, session.score());
        assertEquals(0, session.hintsUsed());
    }

    @Test
    public void peekIsOncePerRunSoLaterQuestionsLoseTheHint() {
        LadderOfFaith reference = new LadderOfFaith(42L, 10, 6);
        GameSession session = definition.createSession(1, 42L);

        session.answer(reference.current().answer);
        reference.answer(reference.current().answer);
        reference.next();
        assertTrue(session.hintAvailable());
        assertEquals(reference.current().reference, session.hint());

        session.answer(reference.current().answer);
        reference.answer(reference.current().answer);
        reference.next();
        assertFalse(session.hintAvailable());
        assertNull(session.hint());
    }

    @Test
    public void everyRungOffersItsAnswerAsAnOption() {
        for (int level = 1; level <= LadderDefinition.LEVELS; level++) {
            LadderSession session = (LadderSession) definition.createSession(level, 100L + level);
            LadderOfFaith reference = new LadderOfFaith(100L + level,
                    LadderDefinition.easyRungsFor(LadderDefinition.tier(level)),
                    LadderDefinition.mediumRungsFor(LadderDefinition.tier(level)));
            for (int rung = 0; rung < LadderOfFaith.RUNGS; rung++) {
                assertTrue(session.options().contains(reference.current().answer));
                assertEquals(4, session.options().size());
                session.answer(reference.current().answer);
                reference.answer(reference.current().answer);
                reference.next();
            }
            assertTrue(session.finished());
            assertNull(session.prompt());
        }
    }
}
