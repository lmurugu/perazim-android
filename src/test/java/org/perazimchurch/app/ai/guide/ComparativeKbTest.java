package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.perazimchurch.app.ai.reference.ReferenceKeys;

/**
 * Structural checks on the comparative knowledge base (Levi-approved 1.5.22). Every entry is classified:
 * FALSE_DOCTRINE_EXAMPLE (another faith, church or group, used only to refute it from Scripture) or POLICY_RULE.
 * Every entry rests on at least two distinct KJV references (multi-passage rule, 2026-11-25).
 */
public class ComparativeKbTest {

    static final String ASSET = "src/main/assets/ai/guide/comparative/comparative-kb.json";
    private static final Pattern ANCHOR = Pattern.compile("^(?:[1-3] )?[A-Za-z ]+ \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$");
    private static final Set<String> POLICY_IDS = new HashSet<>(java.util.Arrays.asList(
            "church_identity_question", "group_question"));

    static JSONObject kb() throws Exception {
        File f = new File(ASSET);
        assertTrue("run tests from the project root", f.isFile());
        return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void everyDetectorConceptHasExactlyOneEntry() throws Exception {
        JSONArray entries = kb().getJSONArray("entries");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            assertTrue("duplicate entry", ids.add(entries.getJSONObject(i).getString("id")));
        }
        for (String concept : DriftDetector.conceptIds()) {
            assertTrue("no KB entry for drift concept " + concept, ids.contains(concept));
        }
        assertEquals(DriftDetector.conceptIds().size(), ids.size());
    }

    @Test
    public void everyEntryIsApprovedAndClassified() throws Exception {
        JSONArray entries = kb().getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String id = e.getString("id");
            assertEquals(id + " status", "APPROVED_LEVI", e.getString("status"));
            String expected = POLICY_IDS.contains(id) ? "POLICY_RULE" : "FALSE_DOCTRINE_EXAMPLE";
            assertEquals(id + " classification", expected, e.getString("classification"));
        }
    }

    @Test
    public void everyEntryRestsOnAtLeastTwoDistinctValidKjvReferences() throws Exception {
        JSONArray entries = kb().getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String id = e.getString("id");
            JSONArray refs = e.getJSONArray("anchors");
            Set<String> distinct = new HashSet<>();
            for (int j = 0; j < refs.length(); j++) {
                String ref = refs.getString(j);
                assertTrue(id + " bad reference " + ref, ANCHOR.matcher(ref).matches());
                String book = ref.replaceFirst(" \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$", "");
                assertTrue(id + " unknown book in " + ref, ReferenceKeys.bookNumber(book) > 0);
                distinct.add(ref);
            }
            assertTrue(id + " needs two distinct passages, has " + distinct.size(), distinct.size() >= DriftEvidence.MIN_PASSAGES);
        }
    }

    @Test
    public void onlyKjvScriptureAndNoEsvNivNkjv() throws Exception {
        String raw = new String(Files.readAllBytes(new File(ASSET).toPath()), StandardCharsets.UTF_8);
        assertFalse(raw.contains("ESV"));
        assertFalse(raw.contains("NIV"));
        assertFalse(raw.contains("NKJV"));
        assertFalse(raw.contains("New World Translation"));
    }
}
