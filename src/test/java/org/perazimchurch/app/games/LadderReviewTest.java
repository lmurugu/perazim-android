package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.BibleGames.Card;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.ReviewScheduler;
import org.perazimchurch.app.domain.games.ReviewScheduler.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class LadderReviewTest {

    /** Prompts of rungs 1 to 7 of a default run. Those rungs always come from the easy tier. */
    private static List<String> easyPrompts(long seed) {
        LadderOfFaith run = new LadderOfFaith(seed);
        List<String> prompts = new ArrayList<>();
        for (int i = 0; i < LadderOfFaith.EASY_RUNGS; i++) {
            prompts.add(run.current().prompt);
            run.answer(run.current().answer);
            run.next();
        }
        return prompts;
    }

    private static List<String> allPrompts(LadderOfFaith run) {
        List<String> prompts = new ArrayList<>();
        while (!run.finished()) {
            Card card = run.current();
            prompts.add(card.prompt);
            run.answer(card.answer);
            if (!run.finished()) run.next();
        }
        return prompts;
    }

    @Test
    public void missedEasyQuestionsAreChosenFirstWhenMissedShareIsAll() {
        List<String> missed = easyPrompts(7L);
        Map<String, Record> history = new HashMap<>();
        for (String prompt : missed) history.put(prompt, new Record(0, 1, 0L));

        LadderOfFaith run = new LadderOfFaith(7L, history, 0, 100);
        Set<String> firstSeven = new HashSet<>();
        for (int i = 0; i < LadderOfFaith.EASY_RUNGS; i++) {
            firstSeven.add(run.current().prompt);
            run.answer(run.current().answer);
            run.next();
        }
        assertEquals(new HashSet<>(missed), firstSeven);
    }

    @Test
    public void historyAwareRunKeepsTwentyDistinctRungsAndIsRepeatable() {
        Map<String, Record> history = new HashMap<>();
        history.put(easyPrompts(3L).get(0), new Record(4, 0, 0L));

        List<String> first = allPrompts(new LadderOfFaith(3L, history, 50, 30));
        List<String> again = allPrompts(new LadderOfFaith(3L, history, 50, 30));

        assertEquals(LadderOfFaith.RUNGS, first.size());
        assertEquals(LadderOfFaith.RUNGS, new HashSet<>(first).size());
        assertEquals(first, again);
    }

    @Test
    public void nullHistoryBehavesAsNoHistory() {
        List<String> none = allPrompts(new LadderOfFaith(11L, null, 50, 30));
        assertEquals(LadderOfFaith.RUNGS, new HashSet<>(none).size());
    }

    @Test
    public void afterAnswerCountsCorrectAndMissed() {
        Record afterRight = ReviewScheduler.afterAnswer(null, true, 100L);
        assertEquals(1, afterRight.correct);
        assertEquals(0, afterRight.missed);
        assertEquals(100L, afterRight.lastSeenDay);

        Record afterMiss = ReviewScheduler.afterAnswer(afterRight, false, 101L);
        assertEquals(1, afterMiss.correct);
        assertEquals(1, afterMiss.missed);
        assertEquals(101L, afterMiss.lastSeenDay);
    }
}
