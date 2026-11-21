package org.perazimchurch.app.domain.games.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.engine.GameSession;

public class WhoAmIDefinitionTest {

    private final WhoAmIDefinition game = WhoAmIDefinition.create(0);

    @Test public void offersTwelveLevelsInThreeTiers() {
        assertEquals(12, game.levelCount());
        assertEquals("Easy 1", game.levelName(1));
        assertEquals("Medium 1", game.levelName(5));
        assertEquals("Hard 4", game.levelName(12));
        assertEquals(WhoAmIDefinition.Tier.EASY, WhoAmIDefinition.tier(4));
        assertEquals(WhoAmIDefinition.Tier.MEDIUM, WhoAmIDefinition.tier(8));
        assertEquals(WhoAmIDefinition.Tier.HARD, WhoAmIDefinition.tier(9));
    }

    @Test public void contentHasNoDuplicateNamesAndEnoughClues() {
        Set<String> names = new HashSet<>();
        for (WhoAmIBank.Person p : WhoAmIBank.PEOPLE) {
            assertTrue("duplicate: " + p.answer, names.add(BibleGames.normalize(p.answer)));
            assertTrue("fewer than 3 clues: " + p.answer, p.clues.size() >= 3);
            assertFalse("missing reference: " + p.answer, p.reference.isEmpty());
            assertFalse("missing hint: " + p.answer, p.hint.isEmpty());
            for (String alias : p.aliases) {
                assertTrue("alias clashes with another name: " + alias,
                        names.add(BibleGames.normalize(alias)) || alias.equals(p.answer));
            }
        }
        assertTrue(WhoAmIBank.PEOPLE.size() >= WhoAmIDefinition.LEVELS * WhoAmIDefinition.PER_LEVEL);
    }

    @Test public void eachRunHasDistinctPeopleOfTheRightSize() {
        for (int level = 1; level <= game.levelCount(); level++) {
            GameSession session = game.createSession(level, 42L);
            assertEquals(WhoAmIDefinition.PER_LEVEL, session.size());
            Set<String> seen = new HashSet<>();
            WhoAmISession run = (WhoAmISession) session;
            while (!run.finished()) {
                assertTrue(seen.add(run.currentAnswerForTest()));
                run.answer("");
            }
        }
    }

    @Test public void sameSeedGivesSameRun() {
        WhoAmISession a = (WhoAmISession) game.createSession(3, 7L);
        WhoAmISession b = (WhoAmISession) game.createSession(3, 7L);
        assertEquals(a.currentAnswerForTest(), b.currentAnswerForTest());
    }

    @Test public void acceptsNameAndAliasesIgnoringCaseAndPunctuation() {
        WhoAmISession run = new WhoAmISession(
                java.util.Collections.singletonList(find("Moses")), WhoAmIDefinition.Tier.EASY);
        assertEquals(GameSession.Outcome.CORRECT, run.answer("  moses! "));
        WhoAmISession alias = new WhoAmISession(
                java.util.Collections.singletonList(find("Moses")), WhoAmIDefinition.Tier.EASY);
        assertEquals(GameSession.Outcome.CORRECT, alias.answer("Musa"));
    }

    @Test public void wrongAnswerGivesGentleFeedbackAndNoDeduction() {
        WhoAmISession run = new WhoAmISession(
                java.util.Collections.singletonList(find("Moses")), WhoAmIDefinition.Tier.EASY);
        assertEquals(GameSession.Outcome.WRONG, run.answer("David"));
        assertEquals(0, run.score());
        assertTrue(run.lastFeedback().contains("Exodus"));
        assertTrue(run.finished());
        assertEquals(GameSession.Outcome.IGNORED, run.answer("Moses"));
    }

    @Test public void comboBonusResetsOnWrongAnswer() {
        WhoAmISession run = (WhoAmISession) game.createSession(1, 5L);
        int firstScore = answerCorrectly(run);
        assertEquals(WhoAmISession.BASE_POINTS, firstScore);
        int secondScore = answerCorrectly(run);
        assertEquals(WhoAmISession.BASE_POINTS + WhoAmISession.COMBO_STEP, secondScore);
        run.answer("not a name");
        assertEquals(0, run.combo());
        int afterReset = answerCorrectly(run);
        assertEquals(WhoAmISession.BASE_POINTS, afterReset);
    }

    @Test public void hintIsOfferedOnceOnEasyAndMediumOnly() {
        WhoAmISession easy = new WhoAmISession(
                java.util.Collections.singletonList(find("Moses")), WhoAmIDefinition.Tier.EASY);
        assertTrue(easy.hintAvailable());
        assertEquals(find("Moses").hint, easy.hint());
        assertFalse(easy.hintAvailable());
        WhoAmISession hard = new WhoAmISession(
                java.util.Collections.singletonList(find("Moses")), WhoAmIDefinition.Tier.HARD);
        assertFalse(hard.hintAvailable());
        assertNull(hard.hint());
    }

    private static int answerCorrectly(WhoAmISession run) {
        int before = run.score();
        run.answer(run.currentAnswerForTest());
        return run.score() - before;
    }

    private static WhoAmIBank.Person find(String answer) {
        for (WhoAmIBank.Person p : WhoAmIBank.PEOPLE) {
            if (p.answer.equals(answer)) return p;
        }
        throw new AssertionError("missing " + answer);
    }
}
