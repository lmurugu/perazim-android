package org.perazimchurch.app.ai.local;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.perazimchurch.app.ai.remote.ConversationHistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Synthetic-only checks that the on-device prompt stays inside its budget and cites only excerpts it showed. */
public final class OfflinePromptBudgetTest {

    private static JSONObject item(String sourceId, String text) throws Exception {
        return new JSONObject()
                .put("sourceId", sourceId)
                .put("version", "1")
                .put("location", "p1")
                .put("title", "Synthetic " + sourceId)
                .put("authorityClass", "REFERENCE")
                .put("citationEligible", true)
                .put("text", text);
    }

    private static String repeat(String unit, int length) {
        StringBuilder builder = new StringBuilder();
        while (builder.length() < length) builder.append(unit);
        return builder.substring(0, length);
    }

    @Test public void worstCaseInputsStayWithinThePromptBudget() throws Exception {
        JSONArray evidence = new JSONArray();
        for (int i = 0; i < 6; i++) evidence.put(item("syn-" + i, repeat("Synthetic passage text. ", 900)));
        ConversationHistory history = new ConversationHistory();
        for (int i = 0; i < 4; i++) {
            history.addExchange(repeat("Earlier question words. ", 650), repeat("Earlier answer words. ", 650));
        }
        LocalPrompt.Built built = LocalPrompt.buildBounded(repeat("Question ", 650), history.snapshot(), evidence);
        assertTrue("prompt was " + built.prompt.length() + " characters",
                built.prompt.length() <= LocalPrompt.MAX_PROMPT_CHARS);
        assertTrue(built.prompt.endsWith("<|im_start|>assistant\n"));
    }

    @Test public void onlyExcerptsThatAreShownBecomeCitable() throws Exception {
        JSONArray evidence = new JSONArray();
        for (int i = 0; i < 6; i++) evidence.put(item("syn-" + i, repeat("Synthetic passage text. ", 900)));
        LocalPrompt.Built built = LocalPrompt.buildBounded("What does the passage say?",
                new ConversationHistory().snapshot(), evidence);
        assertTrue(built.shownEvidence.length() >= 1);
        assertTrue(built.shownEvidence.length() <= LocalPrompt.MAX_EXCERPTS);
        for (int i = 0; i < built.shownEvidence.length(); i++) {
            String sourceId = built.shownEvidence.getJSONObject(i).getString("sourceId");
            assertTrue("shown excerpt must appear in the prompt", built.prompt.contains(sourceId + "@1"));
        }
        // A source the model never saw must not be able to appear in the grammar that constrains its answer.
        String grammar = CitationGrammar.build(built.shownEvidence, 120);
        for (int i = built.shownEvidence.length(); i < evidence.length(); i++) {
            String unseen = evidence.getJSONObject(i).getString("sourceId") + "@1";
            assertFalse("unseen source leaked into grammar: " + unseen, grammar.contains(unseen));
        }
    }

    @Test public void shortGroundedQuestionKeepsAllExcerptsAndHistory() throws Exception {
        JSONArray evidence = new JSONArray()
                .put(item("syn-a", "The synthetic note says version A."))
                .put(item("syn-b", "The synthetic note says version B."));
        ConversationHistory history = new ConversationHistory();
        history.addExchange("Earlier synthetic question.", "Earlier synthetic answer.");
        LocalPrompt.Built built = LocalPrompt.buildBounded("What does the note say?", history.snapshot(), evidence);
        assertEquals(2, built.shownEvidence.length());
        assertTrue(built.prompt.contains("Earlier synthetic question."));
        assertTrue(built.prompt.contains("The synthetic note says version B."));
    }

    @Test public void historyIsDroppedBeforeEvidenceWhenBudgetIsTight() throws Exception {
        JSONArray evidence = new JSONArray().put(item("syn-a", repeat("Grounded passage. ", 700)));
        ConversationHistory history = new ConversationHistory();
        for (int i = 0; i < 4; i++) {
            history.addExchange(repeat("Old question. ", 650), repeat("Old answer. ", 650));
        }
        LocalPrompt.Built built = LocalPrompt.buildBounded(repeat("Question ", 650), history.snapshot(), evidence);
        assertEquals(1, built.shownEvidence.length());
        assertTrue(built.prompt.contains("Grounded passage."));
        assertTrue(built.prompt.length() <= LocalPrompt.MAX_PROMPT_CHARS);
    }
}
