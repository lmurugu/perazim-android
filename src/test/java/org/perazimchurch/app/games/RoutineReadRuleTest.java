package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.RoutineReadRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoutineReadRuleTest {
    @Test public void noChapterMeansReadStepNotDone() { assertFalse(RoutineReadRule.readStepDone(0)); }
    @Test public void oneChapterCompletesReadStep() { assertTrue(RoutineReadRule.readStepDone(1)); }
    @Test public void negativeCountIsNotDone() { assertFalse(RoutineReadRule.readStepDone(-2)); }
}
