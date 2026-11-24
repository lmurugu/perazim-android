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

/** Structural checks on the comparative knowledge base, run on the bundled file. */
public class ComparativeKbTest {

    private static final Pattern ANCHOR = Pattern.compile("^(?:[1-3] )?[A-Za-z ]+ \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$");
    private static final String[] MODEL_FIELDS = {"summary", "origin", "scripture"};
    /** All 27 comparative entries were individually reviewed and approved under Levi's delegated authority. */
    private static final Set<String> LEVI_APPROVED_IDS = new HashSet<>(java.util.Arrays.asList(
            "karma", "reincarnation", "nirvana_moksha", "purgatory", "saint_prayer", "mary_mediator", "icons_veneration", "soul_sleep", "conditional_immortality", "all_paths", "divine_self", "eternal_progression", "works_salvation", "ancestors", "trinity_denial", "jesus_created", "holy_spirit_force", "baptism_for_dead", "saturday_sabbath", "new_age_energy", "christian_science", "secular_worldview", "islam_no_crucifixion", "islam_son_denial", "canon_books", "church_identity_question", "group_question"));

    private static JSONObject kb() throws Exception {
        File f = new File("src/main/assets/ai/guide/comparative/comparative-kb.json");
        assertTrue("run tests from the project root", f.isFile());
        return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void everyDetectorConceptHasAnEntry() throws Exception {
        Set<String> ids = new HashSet<>();
        JSONArray entries = kb().getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) ids.add(entries.getJSONObject(i).getString("id"));
        for (String concept : DriftDetector.conceptIds()) {
            assertTrue("no KB entry for drift concept " + concept, ids.contains(concept));
        }
        assertEquals("every KB entry must be a drift concept", DriftDetector.conceptIds().size(), ids.size());
    }

    @Test
    public void entriesHaveRequiredFieldsAndValidAnchors() throws Exception {
        JSONArray entries = kb().getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String id = e.getString("id");
            for (String field : MODEL_FIELDS) {
                assertTrue(id + " missing " + field, e.optString(field).length() > 40);
            }
            String expectedStatus = LEVI_APPROVED_IDS.contains(id) ? "APPROVED_LEVI" : "DRAFT_PENDING_LEVI";
            assertEquals(id + " status must match the current reviewed approval register", expectedStatus, e.getString("status"));
            JSONArray anchors = e.getJSONArray("anchors");
            assertTrue(id + " needs 1 to 3 anchors", anchors.length() >= 1 && anchors.length() <= 3);
            for (int j = 0; j < anchors.length(); j++) {
                String ref = anchors.getString(j);
                assertTrue(id + " bad anchor " + ref, ANCHOR.matcher(ref).matches());
                String book = ref.replaceFirst(" \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$", "");
                assertTrue(id + " unknown book in " + ref, ReferenceKeys.bookNumber(book) > 0);
            }
        }
    }

    @Test
    public void onlyKjvScriptureAndNoEsvNivNkjv() throws Exception {
        String raw = new String(Files.readAllBytes(new File(
                "src/main/assets/ai/guide/comparative/comparative-kb.json").toPath()), StandardCharsets.UTF_8);
        assertFalse(raw.contains("ESV"));
        assertFalse(raw.contains("NIV"));
        assertFalse(raw.contains("NKJV"));
        assertFalse(raw.contains("New World Translation"));
    }

    @Test
    public void skillsIndexRoutesDriftToItsPlaybook() throws Exception {
        GuideDocs docs = GuideTestSupport.docs();
        boolean found = false;
        for (GuideDocs.SkillEntry skill : docs.index()) {
            if ("drift".equals(skill.id)) {
                found = true;
                assertEquals("playbooks/drift.md", skill.playbook);
                assertFalse(docs.playbook("playbooks/drift.md").isEmpty());
            }
        }
        assertTrue("drift skill missing from Skills.md", found);
    }
}
