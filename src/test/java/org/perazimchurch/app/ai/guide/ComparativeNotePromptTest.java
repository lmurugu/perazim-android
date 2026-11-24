package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.perazimchurch.app.ai.local.LocalPrompt;
import org.perazimchurch.app.ai.remote.ConversationHistory;

/**
 * Integration checks on the final prompt: what the model actually sees for a comparative note. These run the real
 * path (DriftEvidence.noteItem, then LocalPrompt.buildBounded), not only ComparativeGate.noteText.
 */
public class ComparativeNotePromptTest {

    private static final String QUESTION = "Explain what the Catholic and Orthodox teaching on saints means for prayer.";

    private static JSONObject bundled() throws Exception {
        File f = new File("src/main/assets/ai/guide/comparative/comparative-kb.json");
        assertTrue("run tests from the project root", f.isFile());
        return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }

    /** The entry with the longest model-facing note: the hardest case for the prompt budget. */
    private static JSONObject longestEntry(JSONArray entries) {
        JSONObject best = null;
        int bestLength = -1;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            int length = ComparativeGate.noteText(e).length();
            if (length > bestLength) {
                best = e;
                bestLength = length;
            }
        }
        return best;
    }

    /** The text between the excerpt header and its END marker, exactly as the model reads it. */
    private static String materialOf(String prompt) {
        int header = prompt.indexOf("UNTRUSTED SOURCE EXCERPT [");
        assertTrue("no excerpt in prompt", header >= 0);
        int start = prompt.indexOf("]\n", header) + 2;
        int end = prompt.indexOf("\nEND EXCERPT", start);
        assertTrue("excerpt has no END marker", end >= 0);
        return prompt.substring(start, end);
    }

    private static String repeat(String unit, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) out.append(unit);
        return out.toString();
    }

    private static boolean endsAtSentence(String text) {
        String t = text.trim();
        return !t.isEmpty() && ".!?".indexOf(t.charAt(t.length() - 1)) >= 0;
    }

    @Test
    public void comparativeNoteReachesTheModelWithItsSummaryScriptureAndTitlesWhole() throws Exception {
        JSONObject kb = bundled();
        JSONObject entry = longestEntry(kb.getJSONArray("entries"));
        JSONArray evidence = new JSONArray().put(DriftEvidence.noteItem(entry, DriftEvidence.kbVersion(kb)));

        LocalPrompt.Built built = LocalPrompt.buildBounded(QUESTION, new ConversationHistory().snapshot(), evidence);
        String note = ComparativeGate.noteText(entry);

        assertEquals("the comparative note must be shown", 1, built.shownEvidence.length());
        String material = materialOf(built.prompt);
        assertEquals("the full note must reach the model, not a clipped copy", note, material);
        assertTrue(built.prompt.contains("context only, do not cite"));
        assertTrue(endsAtSentence(material));
        assertTrue(material.contains(ComparativeGate.firstSentence(entry.optString("summary").trim())));
        if (!entry.optString("scripture").trim().isEmpty()) {
            assertTrue(material.contains(ComparativeGate.firstSentence(entry.optString("scripture").trim())));
        }
    }

    @Test
    public void internalMaterialAndSourceUrlsNeverReachTheFinalPrompt() throws Exception {
        JSONObject kb = bundled();
        JSONObject entry = longestEntry(kb.getJSONArray("entries"));
        String internal = entry.optString("internal_note");
        JSONArray evidence = new JSONArray().put(DriftEvidence.noteItem(entry, DriftEvidence.kbVersion(kb)));
        String prompt = LocalPrompt.buildBounded(QUESTION, new ConversationHistory().snapshot(), evidence).prompt;

        if (!internal.isEmpty()) assertFalse(prompt.contains(internal));
        assertFalse("source URLs must not reach the model", prompt.contains("http://") || prompt.contains("https://"));
    }

    @Test
    public void excerptIsSkippedRatherThanClippedWhenNoWholeSentenceFits() throws Exception {
        String longRun = repeat("x", 800);
        JSONArray evidence = new JSONArray().put(new JSONObject()
                .put("sourceId", "synthetic-passage").put("version", "1").put("location", "p1")
                .put("title", "Synthetic").put("authorityClass", "REFERENCE").put("citationEligible", true)
                .put("text", longRun));
        LocalPrompt.Built built = LocalPrompt.buildBounded(QUESTION, new ConversationHistory().snapshot(), evidence);
        assertEquals(0, built.shownEvidence.length());
        assertFalse(built.prompt.contains("xxxxxxxxxx"));
    }

    @Test
    public void excerptCutsOnlyAtAWholeSentence() throws Exception {
        String text = "First sentence stands whole. " + repeat("y", 900);
        JSONArray evidence = new JSONArray().put(new JSONObject()
                .put("sourceId", "synthetic-passage").put("version", "1").put("location", "p1")
                .put("title", "Synthetic").put("authorityClass", "REFERENCE").put("citationEligible", true)
                .put("text", text));
        LocalPrompt.Built built = LocalPrompt.buildBounded(QUESTION, new ConversationHistory().snapshot(), evidence);
        assertEquals(1, built.shownEvidence.length());
        String material = materialOf(built.prompt);
        assertEquals("First sentence stands whole.", material);
        assertFalse(built.prompt.contains("yyyy"));
    }

    @Test
    public void longQuestionIsCutAtAWordBoundaryNotMidWord() throws Exception {
        String question = repeat("word ", 300);
        String prompt = LocalPrompt.build(question, new ConversationHistory().snapshot());
        int start = prompt.indexOf("USER QUESTION:\n") + "USER QUESTION:\n".length();
        int end = prompt.indexOf("<|im_end|>", start);
        String shown = prompt.substring(start, end);
        assertTrue(shown.length() <= 650);
        assertTrue("question must end on a whole word: " + shown, shown.endsWith("word"));
    }

    @Test
    public void knowledgeBaseVersionIsReadFromTheJsonNotAConstant() throws Exception {
        JSONObject kb = bundled();
        assertEquals(kb.getString("version"), DriftEvidence.kbVersion(kb));
        assertEquals(kb.getString("version"), DriftEvidence.noteItem(longestEntry(kb.getJSONArray("entries")),
                DriftEvidence.kbVersion(kb)).getString("version"));
    }
}
