package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.ReviewScheduler;
import org.perazimchurch.app.domain.games.ReviewScheduler.Record;
import org.perazimchurch.app.domain.games.StarCap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReviewSchedulerTest {
    private static final List<String> IDS = Arrays.asList("a", "b", "c");

    @Test public void emptyCandidatesGiveNothing() {
        assertNull(ReviewScheduler.pick(Collections.emptyList(), new HashMap<>(), new Random(1)));
    }

    @Test public void unseenQuestionIsPickedWhenNothingIsKnown() {
        assertTrue(IDS.contains(ReviewScheduler.pick(IDS, new HashMap<>(), new Random(7))));
    }

    @Test public void missedQuestionsComeBackOverManyPicks() {
        Map<String, Record> history = new HashMap<>();
        history.put("a", new Record(0, 3, 10));   // missed more than correct
        history.put("b", new Record(5, 0, 10));   // known
        history.put("c", new Record(4, 1, 10));   // known
        int missedPicks = 0, knownPicks = 0;
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            String pick = ReviewScheduler.pick(Arrays.asList("a", "b", "c"), history, random);
            if ("a".equals(pick)) missedPicks++;
            else knownPicks++;
        }
        // No unseen questions here, so the unseen share is spread over the other groups. Both still appear.
        assertTrue("missed question came back " + missedPicks, missedPicks > 300 && missedPicks < 700);
        assertTrue("known questions still come up " + knownPicks, knownPicks > 300);
    }

    @Test public void fallsBackWhenChosenGroupIsEmpty() {
        Map<String, Record> history = new HashMap<>();
        history.put("a", new Record(3, 0, 1));
        history.put("b", new Record(3, 0, 1));
        // Only known questions exist; a pick must still succeed.
        String pick = ReviewScheduler.pick(Arrays.asList("a", "b"), history, new Random(3));
        assertTrue(pick.equals("a") || pick.equals("b"));
    }

    @Test public void stepUpNeedsFourOfLastFive() {
        assertTrue(ReviewScheduler.shouldStepUp(Arrays.asList(false, true, true, true, true)));
        assertFalse(ReviewScheduler.shouldStepUp(Arrays.asList(true, true, true, false, false)));
    }

    @Test public void stepUpNeedsAFullWindow() {
        assertFalse(ReviewScheduler.shouldStepUp(Arrays.asList(true, true, true, true)));
        assertFalse(ReviewScheduler.shouldStepUp(null));
    }

    @Test public void cachedSharesFallBackToBuiltInWhenInvalid() {
        // Invalid shares give the same picks as the built-in ones for the same seed.
        List<String> ids = Arrays.asList("a", "b", "c");
        for (int seed = 0; seed < 50; seed++) {
            assertEquals(ReviewScheduler.pick(ids, new HashMap<>(), new Random(seed)),
                    ReviewScheduler.pick(ids, new HashMap<>(), new Random(seed), 80, 80));
        }
    }

    @Test public void cachedSharesChangeTheMix() {
        // With every pick from unseen questions, only unseen ids come back.
        List<String> ids = Arrays.asList("a", "b");
        Map<String, Record> history = new HashMap<>();
        history.put("a", new Record(3, 0, 1));
        for (int seed = 0; seed < 50; seed++) {
            assertEquals("b", ReviewScheduler.pick(ids, history, new Random(seed), 100, 0));
        }
    }

    @Test public void starCapLimitsTheDay() {
        assertEquals(50, StarCap.allowed(0, 80, StarCap.DEFAULT_DAILY_CAP));
        assertEquals(10, StarCap.allowed(40, 20, StarCap.DEFAULT_DAILY_CAP));
        assertEquals(0, StarCap.allowed(50, 5, StarCap.DEFAULT_DAILY_CAP));
        assertEquals(0, StarCap.allowed(10, -4, StarCap.DEFAULT_DAILY_CAP));
        assertEquals(0, StarCap.allowed(0, 5, 0));
    }
}
