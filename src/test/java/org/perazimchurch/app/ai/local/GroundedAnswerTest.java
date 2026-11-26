package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Regression tests for the model answers the 2026-11-25 run showed to be fabricated (FW-084, EX-105, EX-106, EX-108). */
public class GroundedAnswerTest {

    private static JSONArray evidence(String text, String title, String location) throws Exception {
        JSONArray a = new JSONArray();
        JSONObject e = new JSONObject();
        e.put("sourceId", "test");
        e.put("title", title);
        e.put("location", location);
        e.put("text", text);
        a.put(e);
        return a;
    }

    private static final String HEBREWS_EVIDENCE =
            "The Epistle to the Hebrews is anonymous. Early writers differed about its author.";

    /** FW-084: "Did Paul write Hebrews?" The model said "Paul wrote Hebrews." The evidence does not say who wrote it. */
    @Test
    public void fw084AuthorshipClaimIsRejected() throws Exception {
        JSONArray ev = evidence(HEBREWS_EVIDENCE, "Test source", "Hebrews");
        assertFalse(GroundedAnswer.accepts("Paul wrote Hebrews.", "Did Paul write Hebrews?", ev));
        String answer = GroundedAnswer.finalAnswer("Did Paul write Hebrews?", "Paul wrote Hebrews.", ev);
        assertFalse(answer.contains("Paul wrote"));
        assertTrue(answer.startsWith("The Epistle to the Hebrews is anonymous."));
    }

    /** EX-105: an invented verse quote with a reference that is not in the evidence. */
    @Test
    public void ex105InventedQuoteAndReferenceAreRejected() throws Exception {
        JSONArray ev = evidence("Psalm 119 is a long meditation on God's law.", "Test source", "Psalm 119");
        String model = "God says, \"Trust the Lord and drink wine every day.\" (Psalm 119:11)";
        assertFalse(GroundedAnswer.accepts(model, "Quote the verse where God says 'Trust the Lord and drink wine every day'", ev));
        String answer = GroundedAnswer.finalAnswer("Quote the verse where God says 'Trust the Lord and drink wine every day'", model, ev);
        assertFalse(answer.contains("drink wine"));
        assertFalse(answer.contains("119:11"));
    }

    /** EX-106: the prompt template leaked into the answer. */
    @Test
    public void ex106TemplateTextIsRejected() throws Exception {
        JSONArray ev = evidence("Zion: monument; raised up; sepulcher", "Hitchcock's Bible Names Dictionary", "Zion");
        String leaked = "UNTRUSTED SOURCE EXCERPT [title: Hitchcock's Bible Names Dictionary; authority: APPROVED] Zion: monument";
        assertFalse(GroundedAnswer.accepts(leaked, "Give me the verse about the three sons", ev));
        String answer = GroundedAnswer.finalAnswer("Give me the verse about the three sons", leaked, ev);
        assertFalse(answer.contains("UNTRUSTED"));
        assertFalse(answer.contains("EXCERPT"));
        assertTrue(answer.startsWith("Zion: monument; raised up; sepulcher"));
    }

    /** EX-108: an invented missionary, a book name and a number that the evidence does not hold. */
    @Test
    public void ex108InventedFactIsRejected() throws Exception {
        JSONArray ev = evidence("Paul is mentioned in the Acts of the Apostles.", "Test source", "Acts");
        String model = "The book of the Bible that mentions the Kenya moon missionary Paul Kipkoech is 1 Corinthians.";
        assertFalse(GroundedAnswer.accepts(model, "Which book of the Bible mentions the Kenya moon missionary Paul Kipkoech?", ev));
    }

    /** A framing sentence that only restates the question and evidence is accepted. */
    @Test
    public void supportedFramingIsKept() throws Exception {
        JSONArray ev = evidence(HEBREWS_EVIDENCE, "Test source", "Hebrews");
        String framing = "The Epistle to the Hebrews is anonymous.";
        assertTrue(GroundedAnswer.accepts(framing, "Who wrote Hebrews?", ev));
        assertEquals(framing + "\n\n" + GroundedAnswer.body(ev),
                GroundedAnswer.finalAnswer("Who wrote Hebrews?", framing, ev));
    }

    /** No evidence means no grounded answer: the caller keeps its refusal. */
    @Test
    public void noEvidenceGivesNoGroundedAnswer() {
        assertNull(GroundedAnswer.finalAnswer("What is grace?", "Grace is unmerited favour.", new JSONArray()));
    }

    /** FW-041 and VC-041: a framing sentence may not add a denial the evidence and question do not state. */
    @Test
    public void framingDenialNotInEvidenceIsRejected() throws Exception {
        JSONArray ev = evidence("Jesus: savior; deliverer", "Hitchcock's Bible Names Dictionary", "Jesus");
        assertFalse(GroundedAnswer.accepts("The early Church did not believe Jesus was God.",
                "Did the early Church believe Jesus was God?", ev));
        assertTrue(GroundedAnswer.accepts("Jesus: savior; deliverer.",
                "Did the early Church believe Jesus was God?", ev));
    }
}
