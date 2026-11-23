package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class GroupSaintsLeakageTest {

    private static JSONArray bundledEntries() throws Exception {
        File f = new File("src/main/assets/ai/guide/comparative/comparative-kb.json");
        assertTrue("run tests from the project root", f.isFile());
        return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)).getJSONArray("entries");
    }

    private static List<DriftDetector.Hit> hits(String q) {
        return DriftDetector.detect(q);
    }

    // Ordinary Bible questions that use the word "saints" must not be treated as drift.
    @Test
    public void ordinaryBibleUseOfSaintsIsNotDrift() {
        assertFalse(DriftDetector.drifts("Who are the saints mentioned in Ephesians 1:1?"));
        assertFalse(DriftDetector.drifts("Paul greets the saints at Philippi and prays for the church."));
        assertFalse(DriftDetector.drifts("Pray to the Lord for the saints, as Paul asks in Ephesians 6:18."));
    }

    @Test
    public void praySaintsQuestionsStillDetectDrift() {
        assertTrue(DriftDetector.drifts("Should I pray to the saints?"));
        assertTrue(DriftDetector.drifts("Do the saints intercede for us?"));
    }

    // Group and church questions route to the policy and group concepts, not to a verdict.
    @Test
    public void groupQuestionsAreDetected() {
        assertTrue(hits("Are Mormons a cult?").toString(), hits("Are Mormons a cult?").stream()
                .anyMatch(h -> h.conceptId.equals("group_question")));
        assertTrue(hits("Do Jehovah's Witnesses teach that Jesus is Michael?").stream()
                .anyMatch(h -> h.conceptId.equals("jesus_created") || h.conceptId.equals("group_question")));
    }

    // The model note carries the approved fields: official summary, Scripture evaluation, origin and approved source
    // titles. Internal notes, reviewer material and source URLs never appear.
    @Test
    public void noteTextLeaksNoInternalFields() throws Exception {
        JSONObject entry = new JSONObject()
                .put("id", "x").put("origin", "ORIGIN-TEXT").put("scripture", "SCRIPTURE-TEXT")
                .put("summary", "SUMMARY-TEXT").put("internal_note", "INTERNAL-SECRET").put("sources", "SOURCE-LINK");
        String note = ComparativeGate.noteText(entry);
        assertTrue(note, note.contains("ORIGIN-TEXT") && note.contains("SCRIPTURE-TEXT"));
        assertFalse(note, note.contains("INTERNAL-SECRET"));
        assertFalse(note, note.contains("SOURCE-LINK"));
    }

    // Source titles are model-visible; source URLs and any other source field are not.
    @Test
    public void noteTextShowsSourceTitlesButNeverSourceUrls() throws Exception {
        JSONObject entry = new JSONObject()
                .put("id", "x").put("summary", "SUMMARY-TEXT").put("scripture", "SCRIPTURE-TEXT")
                .put("sources", new JSONArray().put(new JSONObject()
                        .put("title", "TITLE-OK").put("url", "https://example.invalid/PRIVATE-URL")
                        .put("licence", "PRIVATE-LICENCE")));
        String note = ComparativeGate.noteText(entry);
        assertTrue(note, note.contains("Sources: TITLE-OK"));
        assertFalse(note, note.contains("PRIVATE-URL"));
        assertFalse(note, note.contains("https://"));
        assertFalse(note, note.contains("PRIVATE-LICENCE"));
    }

    @Test
    public void bundledEntriesLeakNoInternalNotesIntoTheNote() throws Exception {
        JSONArray entries = bundledEntries();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String internal = e.optString("internal_note");
            if (internal.isEmpty()) continue;
            String note = ComparativeGate.noteText(e);
            assertFalse("internal note leaked for " + e.optString("id"), note.contains(internal));
        }
    }
}
