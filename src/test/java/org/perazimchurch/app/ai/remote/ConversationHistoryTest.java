package org.perazimchurch.app.ai.remote;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConversationHistoryTest {
    @Test public void historyKeepsCompleteBoundedExchanges() {
        ConversationHistory h = new ConversationHistory();
        for (int i = 0; i < 20; i++) h.addExchange("q" + i, "a" + i);
        assertEquals(12, h.snapshot().size());
        assertEquals("user", h.snapshot().get(0).role);
        assertEquals("q14", h.snapshot().get(0).text);
        assertEquals("a19", h.snapshot().get(11).text);
        h.clear();
        assertTrue(h.isEmpty());
    }
    @Test public void continuationRemovesOnlyGreeting() {
        assertEquals("Forgiveness takes practice.", ConversationHistory.withoutRepeatedGreeting("Shalom, friend! Forgiveness takes practice.", true));
        assertEquals("Shalom, friend! Forgiveness takes practice.", ConversationHistory.withoutRepeatedGreeting("Shalom, friend! Forgiveness takes practice.", false));
        assertEquals("Forgiveness takes practice.", ConversationHistory.withoutRepeatedGreeting("Hello! Forgiveness takes practice.", true));
        assertEquals("There is no easy answer.", ConversationHistory.withoutRepeatedGreeting("There is no easy answer.", true));
    }
    @Test public void oldRelayGetsBoundedContextWithoutRepeatingIntroduction() {
        ConversationHistory h = new ConversationHistory();
        h.addExchange("How can I forgive?", "Start by acknowledging the hurt, without excusing it.");
        String request = ConversationHistory.legacyQuestion("What next?", h.snapshot());
        assertTrue(request.contains("acknowledging the hurt"));
        assertTrue(request.contains("What next?"));
        assertTrue(request.length() <= 500);
    }
}
