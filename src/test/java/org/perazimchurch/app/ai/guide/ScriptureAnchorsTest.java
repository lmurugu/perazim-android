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
 * Structural checks on the drift scripture anchors, run on the bundled file. The file holds Bible references only.
 * Other faiths' descriptions were removed on 2026-11-25 and must not come back.
 */
public class ScriptureAnchorsTest {

    static final String ASSET = "src/main/assets/ai/guide/drift/scripture-anchors.json";
    private static final Pattern ANCHOR = Pattern.compile("^(?:[1-3] )?[A-Za-z ]+ \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$");

    static JSONObject anchors() throws Exception {
        File f = new File(ASSET);
        assertTrue("run tests from the project root", f.isFile());
        return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void everyDetectorConceptHasExactlyOneEntry() throws Exception {
        JSONArray entries = anchors().getJSONArray("entries");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            assertTrue("duplicate entry " + entries.getJSONObject(i).getString("id"),
                    ids.add(entries.getJSONObject(i).getString("id")));
        }
        for (String concept : DriftDetector.conceptIds()) {
            assertTrue("no anchors for drift concept " + concept, ids.contains(concept));
        }
        assertEquals(DriftDetector.conceptIds().size(), ids.size());
    }

    @Test
    public void everyEntryHasOneOrTwoValidKjvReferences() throws Exception {
        JSONArray entries = anchors().getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String id = e.getString("id");
            JSONArray refs = e.getJSONArray("anchors");
            assertTrue(id + " needs 1 or 2 anchors", refs.length() >= 1 && refs.length() <= 3);
            for (int j = 0; j < refs.length(); j++) {
                String ref = refs.getString(j);
                assertTrue(id + " bad reference " + ref, ANCHOR.matcher(ref).matches());
                String book = ref.replaceFirst(" \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$", "");
                assertTrue(id + " unknown book in " + ref, ReferenceKeys.bookNumber(book) > 0);
            }
        }
    }

    @Test
    public void fileHoldsNoDescriptionsOfOtherTraditions() throws Exception {
        // Only the keys and references may be present. Any summary, origin, scripture or sources field is a removed source.
        JSONArray entries = anchors().getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            assertEquals(e.getString("id") + " has an extra field", 2, e.length());
            assertFalse(e.has("summary"));
            assertFalse(e.has("origin"));
            assertFalse(e.has("scripture"));
            assertFalse(e.has("sources"));
        }
    }

    @Test
    public void onlyKjvScriptureAndNoEsvNivNkjvOrOtherTranslations() throws Exception {
        String raw = new String(Files.readAllBytes(new File(ASSET).toPath()), StandardCharsets.UTF_8);
        assertFalse(raw.contains("ESV"));
        assertFalse(raw.contains("NIV"));
        assertFalse(raw.contains("NKJV"));
        assertFalse(raw.contains("New World Translation"));
        assertFalse(raw.contains("Qur"));
    }

    @Test
    public void removedComparativeKnowledgeBaseIsGone() {
        assertFalse("comparative knowledge base must stay removed",
                new File("src/main/assets/ai/guide/comparative/comparative-kb.json").exists());
    }
}
