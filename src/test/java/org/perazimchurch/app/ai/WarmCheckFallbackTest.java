package org.perazimchurch.app.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/** Distress the crisis rules do not name gets the warm check with the helplines (Levi, 2026-11-26). */
public class WarmCheckFallbackTest {

    @Test
    public void firstPersonDistressGetsTheWarmCheck() {
        assertEquals("warm-check", PerazimAiEngine.preModelRoute("I feel so hopeless and alone in this church."));
        assertEquals("warm-check", PerazimAiEngine.preModelRoute("I am sad and lonely in this church."));
        assertEquals("warm-check", PerazimAiEngine.preModelRoute("Sina tumaini tena, nimechoka kabisa."));
    }

    @Test
    public void explicitCrisisStillGoesToCrisisNotTheWarmCheck() {
        assertEquals("crisis", PerazimAiEngine.preModelRoute("I want to die."));
    }

    @Test
    public void doctrineQuestionsAreNotWarmChecks() {
        assertNotEquals("warm-check", PerazimAiEngine.preModelRoute("What does the Bible say about the sovereignty of God?"));
        assertNotEquals("warm-check", PerazimAiEngine.preModelRoute("Is it wrong to be angry at God?"));
        assertNotEquals("warm-check", PerazimAiEngine.preModelRoute("Did Jesus weep at the tomb of Lazarus?"));
    }
}
