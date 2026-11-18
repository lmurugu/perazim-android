package org.perazimchurch.app.ai.local;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OfflineKnowledgeStorePolicyTest {
    @Test public void barePassageAndVerseFollowUpsNeedContext() {
        assertTrue(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion("What does this passage mean?"));
        assertTrue(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion("Can you explain this verse?"));
        assertTrue(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion("What does Scripture say?"));
    }

    @Test public void explicitReferencesAndTopicalQuestionsCanRetrieveEvidence() {
        assertFalse(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion("Explain John 3:16"));
        assertFalse(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion(
                "What does this passage say about forgiveness?"));
    }

    @Test public void emptyQuestionIsNotAnUnresolvedReference() {
        assertFalse(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion(null));
        assertFalse(OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion("   "));
    }
}
