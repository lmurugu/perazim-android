package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.GameSession.Outcome;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RoadDefinitionTest {

    private final RoadDefinition definition = new RoadDefinition(0);

    @Test
    public void hasTwelveLevelsInThreeTiers() {
        assertEquals(12, definition.levelCount());
        assertEquals("Easy 1", definition.levelName(1));
        assertEquals("Medium 1", definition.levelName(5));
        assertEquals("Hard 4", definition.levelName(12));
        assertEquals(RoadDefinition.Tier.HARD, RoadDefinition.tier(9));
    }

    @Test
    public void everyTierHasEnoughStagesForARun() {
        assertTrue(RoadBank.EASY.length >= RoadSession.STAGES);
        assertTrue(RoadBank.MEDIUM.length >= RoadSession.STAGES);
        assertTrue(RoadBank.HARD.length >= RoadSession.STAGES);
        assertTrue(RoadBank.EASY.length + RoadBank.MEDIUM.length + RoadBank.HARD.length >= 30);
    }

    @Test
    public void everyRowHasSevenFieldsAndDistinctOptions() {
        for (String[] rows : new String[][] {RoadBank.EASY, RoadBank.MEDIUM, RoadBank.HARD}) {
            Set<String> questions = new HashSet<>();
            for (String row : rows) {
                String[] f = row.split("~", -1);
                assertEquals(row, 7, f.length);
                assertTrue(questions.add(f[0]));
                Set<String> options = new HashSet<>();
                for (int i = 1; i <= 4; i++) options.add(f[i]);
                assertEquals(row, 4, options.size());
            }
        }
    }

    @Test
    public void runHasSixStagesAndOptionsContainTheAnswer() {
        for (int level = 1; level <= RoadDefinition.LEVELS; level++) {
            RoadSession run = (RoadSession) definition.createSession(level, 900L + level);
            assertEquals(RoadSession.STAGES, run.size());
            for (int stage = 0; stage < RoadSession.STAGES; stage++) {
                assertEquals(4, run.options().size());
                String answer = answerOf(run);
                assertTrue(run.options().contains(answer));
                run.answer(answer);
            }
            assertTrue(run.finished());
            assertNull(run.prompt());
        }
    }

    @Test
    public void correctStagesScoreWithComboAndWrongResetsIt() {
        GameSession run = definition.createSession(1, 5L);
        RoadSession road = (RoadSession) run;
        assertEquals(Outcome.WRONG, run.answer("no such place"));
        assertEquals(0, run.score());
        assertEquals(Outcome.CORRECT, run.answer(answerOf(road)));
        assertEquals(10, run.score());
        assertEquals(Outcome.CORRECT, run.answer(answerOf(road)));
        assertEquals(25, run.score());
        assertEquals(Outcome.WRONG, run.answer("no such place"));
        assertEquals(Outcome.CORRECT, run.answer(answerOf(road)));
        assertEquals(35, run.score());
    }

    @Test
    public void hintGivesTheInitialAndCostsPointsOncePerStage() {
        RoadSession road = (RoadSession) definition.createSession(1, 8L);
        String answer = answerOf(road);
        assertTrue(road.hintAvailable());
        assertEquals("The answer starts with " + answer.charAt(0) + ".", road.hint());
        assertFalse(road.hintAvailable());
        assertEquals(Outcome.CORRECT, road.answer(answer));
        assertEquals(10 - 3, road.score());
    }

    @Test
    public void ignoresAnswersAfterTheRun() {
        RoadSession road = (RoadSession) definition.createSession(1, 2L);
        for (int i = 0; i < RoadSession.STAGES; i++) road.answer(answerOf(road));
        assertEquals(Outcome.IGNORED, road.answer("anything"));
    }

    /** The correct answer for the current stage, read from the same seeded run order. */
    private static String answerOf(RoadSession run) {
        return run.answerForTest();
    }
}
