package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Rules for the model-facing comparative note, checked on synthetic entries and on the bundled knowledge base. */
public class GuideNoteRulesTest {

    private static final Set<String> GROUPS = new HashSet<>(Arrays.asList(
            "african_traditional", "bible_canon", "christian_tradition_internal", "disputed_christian_doctrine",
            "heterodox_group", "new_age", "policy", "secular", "world_religion"));
    private static final String[] LABELS = {"Official teaching: ", "Scripture evaluation: ", "Local practice: ",
            "Origin: ", "Sources: "};

    private static JSONArray bundledEntries() throws Exception {
        File f = new File("src/main/assets/ai/guide/comparative/comparative-kb.json");
        assertTrue("run tests from the project root", f.isFile());
        return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8))
                .getJSONArray("entries");
    }

    private static JSONObject synthetic(String group, String summary, String scripture) throws Exception {
        return new JSONObject().put("id", "test_entry").put("group", group).put("status", "DRAFT_PENDING_LEVI")
                .put("summary", summary).put("scripture", scripture).put("origin", "Origin text.")
                .put("sources", new JSONArray().put(new JSONObject().put("title", "Primary source")));
    }

    private static String firstSentence(String text) {
        int end = text.indexOf(". ");
        return end < 0 ? text.trim() : text.substring(0, end + 1).trim();
    }

    /** Asserts that a cut field ends at a sentence end: the segment is either the whole value or ends in punctuation. */
    private static void assertNoMidSentenceCut(JSONObject e, int budget) throws Exception {
        String note = ComparativeGate.noteText(e, budget);
        assertTrue(e.optString("id") + " over budget: " + note.length(), note.length() <= budget);
        if (ComparativeGate.noteText(e, 100000).length() <= budget) return;
        for (String label : LABELS) {
            int at = note.indexOf(" " + label);
            if (at < 0) continue;
            int start = at + 1 + label.length();
            int end = note.length();
            for (String other : LABELS) {
                int n = note.indexOf(" " + other, start);
                if (n >= 0 && n < end) end = n;
            }
            String segment = note.substring(start, end).trim();
            String whole = valueFor(e, label);
            if (segment.equals(whole)) continue;
            assertTrue(e.optString("id") + " cut mid-sentence in '" + label + "': " + segment,
                    segment.endsWith(".") || segment.endsWith("!") || segment.endsWith("?"));
        }
    }

    private static String valueFor(JSONObject e, String label) {
        switch (label) {
            case "Official teaching: ": return e.optString("summary").trim();
            case "Scripture evaluation: ": return e.optString("scripture").trim();
            case "Local practice: ": return e.optString("practice").trim();
            case "Origin: ": return e.optString("origin").trim();
            default: return ComparativeGate.sourceTitles(e.optJSONArray("sources"));
        }
    }

    // 1. Catholic and Orthodox teachings can be named and described; the note does not hide the source.
    @Test
    public void catholicAndOrthodoxTeachingsCanBeNamedAndDescribed() throws Exception {
        JSONObject e = synthetic("christian_tradition_internal",
                "The Roman Catholic Church teaches that purgatory is a state of cleansing. The Eastern Orthodox Church "
                        + "teaches veneration of icons.", "1 John 1:7 is the Scripture evaluation.");
        String note = ComparativeGate.noteText(e);
        assertTrue(note, note.contains("Roman Catholic Church"));
        assertTrue(note, note.contains("Eastern Orthodox Church"));
    }

    // 2. Groups are distinguished in the note by their category.
    @Test
    public void groupsAreDistinguished() throws Exception {
        JSONArray entries = bundledEntries();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String group = e.getString("group");
            assertTrue(e.getString("id") + " has unknown group " + group, GROUPS.contains(group));
            assertTrue(e.getString("id") + " is not labelled with its category",
                    ComparativeGate.noteText(e).contains("Category: " + group.replace('_', ' ') + "."));
        }
        assertFalse(ComparativeGate.categoryLabel("christian_tradition_internal")
                .equals(ComparativeGate.categoryLabel("heterodox_group")));
    }

    // 3. Official teaching and local practice are separate, labelled, and in that order.
    @Test
    public void officialTeachingAndLocalPracticeAreSeparatelyLabelled() throws Exception {
        JSONObject e = synthetic("world_religion", "Official summary.", "Scripture text.")
                .put("practice", "Some families do this at home.");
        String note = ComparativeGate.noteText(e);
        assertTrue(note, note.contains("Official teaching: Official summary."));
        assertTrue(note, note.contains("Local practice: Some families do this at home."));
        assertTrue(note, note.indexOf("Official teaching:") < note.indexOf("Local practice:"));
        assertFalse(ComparativeGate.noteText(synthetic("world_religion", "S.", "T.")).contains("Local practice"));
    }

    // 4. Internal notes and reviewer material never reach the model.
    @Test
    public void internalNotesNeverReachTheModel() throws Exception {
        JSONObject e = synthetic("heterodox_group", "Summary.", "Scripture.")
                .put("internal_note", "REVIEWER-ONLY-MATERIAL").put("sources", new JSONArray());
        assertFalse(ComparativeGate.noteText(e).contains("REVIEWER-ONLY-MATERIAL"));
        JSONArray entries = bundledEntries();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject b = entries.getJSONObject(i);
            String internal = b.optString("internal_note");
            if (internal.isEmpty()) continue;
            assertFalse(b.getString("id"), ComparativeGate.noteText(b).contains(internal));
        }
    }

    // 5. Both the official summary and the Scripture evaluation reach the model, for every bundled entry.
    @Test
    public void summaryAndScriptureBothReachTheModel() throws Exception {
        JSONArray entries = bundledEntries();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String note = ComparativeGate.noteText(e);
            assertTrue(e.getString("id") + " summary missing",
                    note.contains(firstSentence(e.getString("summary"))));
            assertTrue(e.getString("id") + " scripture missing",
                    note.contains(firstSentence(e.getString("scripture"))));
        }
    }

    // 6. No silent mid-sentence cuts, at any budget the prompt might use.
    @Test
    public void noteNeverEndsMidSentence() throws Exception {
        JSONArray entries = bundledEntries();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            for (int budget : new int[] {250, 400, 600, 900}) {
                assertNoMidSentenceCut(e, budget);
            }
        }
    }

    // 7. Only APPROVED_LEVI entries are eligible for the model.
    @Test
    public void onlyApprovedEntriesReachTheModel() throws Exception {
        JSONArray entries = new JSONArray()
                .put(new JSONObject().put("id", "karma").put("status", "DRAFT_PENDING_LEVI"))
                .put(new JSONObject().put("id", "karma").put("status", "APPROVED_LEVI"));
        JSONObject approved = ComparativeGate.approvedEntry(entries, "karma");
        assertEquals("APPROVED_LEVI", approved.getString("status"));
        assertNull(ComparativeGate.approvedEntry(new JSONArray().put(
                new JSONObject().put("id", "x").put("status", "DRAFT_PENDING_LEVI")), "x"));
    }

    // 8. Ordinary Bible uses of "saints" are not treated as drift.
    @Test
    public void biblicalSaintsAreNotMisclassified() {
        assertFalse(DriftDetector.drifts("Who are the saints mentioned in Ephesians 1:1?"));
        assertFalse(DriftDetector.drifts("Paul writes to the saints at Philippi, greeting them in Christ."));
    }

    // Synthetic check of the cut itself: a long summary is cut only at a sentence end, never mid-sentence.
    @Test
    public void longFieldIsCutOnlyAtASentenceEnd() throws Exception {
        String summary = "First sentence is short. Second sentence is considerably longer and continues for a while "
                + "so that the budget runs out before it finishes its thought entirely.";
        JSONObject e = synthetic("secular", summary, "Scripture line.");
        int budget = 170; // the header and category use 60; the first sentence fits; the second cannot end in room
        String note = ComparativeGate.noteText(e, budget);
        assertTrue(note, note.contains("First sentence is short."));
        assertFalse(note, note.contains("Second sentence"));
        assertNoMidSentenceCut(e, budget);
    }
}
