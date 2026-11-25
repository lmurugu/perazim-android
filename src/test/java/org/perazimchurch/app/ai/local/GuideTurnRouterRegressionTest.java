package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Regression tests for the three phone failures of v23 (Levi, 2026-11-25), through {@link GuideTurnRouter#respond}, the same
 * entry the guide dialog uses, with the conversation so far passed in as history.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GuideTurnRouterRegressionTest {

    private Context context;
    private PerazimAiEngine engine;

    @Before
    public void startConversation() {
        context = ApplicationProvider.getApplicationContext();
        SoftSafety.reset();
        engine = new PerazimAiEngine(context);
    }

    /** Earlier messages of the same session, with the replies the guide gave, as the dialog's history holds them. */
    private List<ConversationHistory.Turn> earlierConversation() {
        String[] earlier = {
                "My husband beats me and I want to kill myself.",
                "Mume wangu ananipiga kila siku. Nifanyeje?",
                "My brother texted me, 'I'm ending my life tonight'. What should I do?",
                "Jesus wept",
                "hello",
                "far from God",
        };
        List<ConversationHistory.Turn> history = new ArrayList<>();
        for (String message : earlier) {
            GuideTurnRouter.Turn turn = GuideTurnRouter.route(context, engine, message);
            history.add(new ConversationHistory.Turn("user", message));
            if (turn.response != null) {
                history.add(new ConversationHistory.Turn("assistant", turn.response.getMessageText()));
            }
        }
        return history;
    }

    @Test
    public void baptismQuestionGetsTheApprovedBaptismAnswerThroughTheChatPath() {
        GuideTurnRouter.Turn turn = GuideTurnRouter.respond(context, engine,
                "What does the Bible teach about baptism?", earlierConversation());
        String text = turn.response == null ? "" : turn.response.getMessageText();
        assertEquals("routed:approved-baptism-believers", turn.status);
        assertTrue(text, text.contains("believer's baptism"));
        assertFalse(text, text.contains("authorised to compare"));
        assertFalse(text, text.contains("verify a general claim"));
    }

    @Test
    public void capabilityReplyIsTheWholeApprovedTextWithNoEndMarker() {
        GuideTurnRouter.Turn expected = GuideTurnRouter.route(context, engine, "What can you do");
        GuideTurnRouter.Turn turn = GuideTurnRouter.respond(context, engine, "What can you do", earlierConversation());
        String text = turn.response == null ? "" : turn.response.getMessageText();
        assertEquals("routed:conversational-capability", turn.status);
        assertEquals(expected.response.getMessageText(), text);
        assertFalse(text, text.contains("end of text"));
        assertFalse(text, text.contains("<|"));
    }

    @Test
    public void swahiliNenoGodQuestionGetsKjvPassagesNotTheNoSourceReply() {
        GuideTurnRouter.Turn turn = GuideTurnRouter.respond(context, engine, "Neno gani na God?", earlierConversation());
        String text = turn.response == null ? "" : turn.response.getMessageText();
        assertEquals("routed:approved-neno-god-word", turn.status);
        assertTrue(text, text.contains("In the beginning was the Word"));
        assertTrue(text, text.contains("And the Word was made flesh"));
    }

    @Test
    public void modelEndMarkersAreRemovedEverywhereTheyAreCleaned() {
        assertEquals("Thank you for asking.", ModelOutput.stripEndMarkers("Thank you for asking.[end of text]"));
        assertEquals("Hello", ModelOutput.stripEndMarkers("  Hello<|im_end|> "));
        assertEquals("ok", ModelOutput.stripEndMarkers("ok</s>"));
        assertEquals(null, ModelOutput.stripEndMarkers(null));
        String composed = ConversationComposer.verify("Thank you for sharing this with me.[end of text]",
                "hello there", null, false);
        assertNotNull(composed);
        assertFalse(composed, composed.contains("end of text"));
    }
}
