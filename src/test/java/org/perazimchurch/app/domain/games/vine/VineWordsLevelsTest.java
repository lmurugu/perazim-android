package org.perazimchurch.app.domain.games.vine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class VineWordsLevelsTest {

    @Test public void readsOneLevelWithNullBonus() {
        String json = "{\"levels\":[{\"level\":1,\"band\":1,\"passage\":\"John 15:1\",\"seed\":7,"
                + "\"verses\":[{\"ref\":\"John 15:1\",\"text\":\"I am the true vine.\"}],"
                + "\"rows\":1,\"cols\":4,\"words\":[{\"word\":\"VINE\",\"row\":0,\"col\":0,\"across\":true}],"
                + "\"wheel\":\"EINV\",\"bonus\":null}]}";
        VineWordsLevel level = VineWordsLevels.parse(json).get(0);
        assertEquals(1, level.number());
        assertEquals("VINE", level.targetWords().get(0));
        assertNull(level.bonus());
        assertEquals(7L, level.seed());
    }

    @Test public void badFileFailsLoudly() {
        try {
            VineWordsLevels.parse("{\"levels\": 3}");
            fail("expected an error");
        } catch (IllegalArgumentException expected) {
            assertEquals(true, expected.getMessage().startsWith("Bad Vine Words level file"));
        }
    }
}
