package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Composed-reply checks that need no model: profanity and sexual terms, and the English-only language policy. */
public class ConversationComposerSafetyTest {

    @Test
    public void smallModelSlangIsRejected() {
        assertNull(ConversationComposer.verify("Aite, kuma kuma.", "asante sana", null, false));
    }

    @Test
    public void englishProfanityIsRejected() {
        assertNull(ConversationComposer.verify("That is a fucking shame, my friend.", "hello", null, false));
        assertNull(ConversationComposer.verify("Don't be a bitch about it.", "hello", null, false));
    }

    @Test
    public void swahiliWordsAreRejectedForEnglishQuestions() {
        assertNull(ConversationComposer.verify("Karibu sana, nisaidie kwa swali lako.", "habari", null, false));
        assertNull(ConversationComposer.verify("Asante sana for asking.", "asante", null, false));
    }

    @Test
    public void swahiliIsAllowedOnlyWhenAskedFor() {
        assertFalse(ConversationComposer.asksForSwahili("hello"));
        assertFalse(ConversationComposer.asksForSwahili("Nipe maombi"));
        assertEquals(true, ConversationComposer.asksForSwahili("Please reply in Swahili: how are you?"));
        assertEquals("Karibu sana, nisaidie kwa swali lako.",
                ConversationComposer.verify("Karibu sana, nisaidie kwa swali lako.", "Reply in Swahili please.", null, false));
    }

    @Test
    public void cleanEnglishPasses() {
        assertEquals("Thank you for asking. I'm ready to help you with a passage or a prayer.",
                ConversationComposer.verify("Thank you for asking. I'm ready to help you with a passage or a prayer.",
                        "how are you", null, false));
    }

    @Test
    public void onlyTheCleanSentenceIsKept() {
        assertEquals("I am ready to help you with a prayer today.",
                ConversationComposer.verify("This is shit. I am ready to help you with a prayer today.",
                        "what can you do", null, false));
    }

    @Test
    public void sexualTermsAreRejected() {
        assertNull(ConversationComposer.verify("Let us talk about sex and porn.", "hello", null, false));
    }
}
