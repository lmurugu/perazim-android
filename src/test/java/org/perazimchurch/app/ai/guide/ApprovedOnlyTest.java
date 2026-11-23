package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Only entries whose status is exactly APPROVED_LEVI may reach the model. Drafts and pending entries never do. */
public class ApprovedOnlyTest {

    @Test
    public void draftAndPendingEntriesAreExcluded() throws Exception {
        JSONArray entries = new JSONArray()
                .put(new JSONObject().put("id", "karma").put("status", "DRAFT_PENDING_LEVI"))
                .put(new JSONObject().put("id", "saint_prayer").put("status", "PENDING"))
                .put(new JSONObject().put("id", "purgatory").put("status", "approved_levi"));
        assertNull(ComparativeGate.approvedEntry(entries, "karma"));
        assertNull(ComparativeGate.approvedEntry(entries, "saint_prayer"));
        assertNull("status match is exact, case included", ComparativeGate.approvedEntry(entries, "purgatory"));
    }

    @Test
    public void approvedEntryIsReturned() throws Exception {
        JSONArray entries = new JSONArray()
                .put(new JSONObject().put("id", "karma").put("status", ComparativeGate.APPROVED_STATUS));
        assertNotNull(ComparativeGate.approvedEntry(entries, "karma"));
        assertNull(ComparativeGate.approvedEntry(entries, "reincarnation"));
        assertNull(ComparativeGate.approvedEntry(null, "karma"));
        assertNull(ComparativeGate.approvedEntry(entries, null));
    }

    @Test
    public void bundledKnowledgeBaseHasNothingApprovedYet() throws Exception {
        String json = new String(Files.readAllBytes(new java.io.File(
                "src/main/assets/ai/guide/comparative/comparative-kb.json").toPath()), StandardCharsets.UTF_8);
        JSONArray entries = new JSONObject(json).getJSONArray("entries");
        for (String id : DriftDetector.conceptIds()) {
            assertNull("no entry may be approved until Levi approves it: " + id,
                    ComparativeGate.approvedEntry(entries, id));
        }
    }
}
