package org.perazimchurch.app.ai.local;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The fair tradition-comparison answer is for two Protestant, Pentecostal or Evangelical traditions only. Any
 * non-Protestant name sends the question to the other-groups decline instead, so the answer never puts such a group on
 * equal footing.
 */
public class TraditionComparisonTest {

    @Test
    public void protestantAndPentecostalComparisonsGetTheFairAnswer() {
        assertTrue(PreModel.comparesTraditions("Are Pentecostals less biblical than Presbyterians?"));
        assertTrue(PreModel.comparesTraditions("Are Baptists or Methodists more biblical?"));
        assertTrue(PreModel.comparesTraditions("Is Anglican teaching true or Lutheran teaching?"));
    }

    @Test
    public void anyNonProtestantNameSendsTheQuestionElsewhere() {
        assertFalse(PreModel.comparesTraditions("Are Catholics less biblical than Pentecostals?"));
        assertFalse(PreModel.comparesTraditions("Is Orthodox teaching more biblical than Baptist teaching?"));
        assertFalse(PreModel.comparesTraditions("Are Mormons or Baptists right about salvation?"));
        assertFalse(PreModel.comparesTraditions("Is Jehovah's Witness teaching better than Presbyterian?"));
    }

    @Test
    public void oneTraditionOnlyIsNotAComparison() {
        assertFalse(PreModel.comparesTraditions("Are Pentecostals biblical?"));
        assertFalse(PreModel.comparesTraditions("What do Presbyterians teach about baptism?"));
    }
}
