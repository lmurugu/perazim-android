package org.perazimchurch.app.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/** A child with a substance and ongoing use goes to the drug-safety reply (KE-044, 2026-11-25). Doctrine questions stay on their routes. */
public class ChildSubstanceRouteTest {

    @Test
    public void childSubstanceUseGoesToDrugSafety() {
        assertEquals("drug-safety", PerazimAiEngine.preModelRoute("Kijana wangu anatumia bangi na pombe kila siku. Nifanyeje?"));
        assertEquals("drug-safety", PerazimAiEngine.preModelRoute("My son smokes weed every day. What should I do?"));
    }

    @Test
    public void doctrineWordingWithAChildIsNotDrugSafety() {
        assertNotEquals("drug-safety", PerazimAiEngine.preModelRoute("Is it a sin for my son to drink alcohol?"));
    }
}
