package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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

    /** Levi's explicit approvals (owner PR 76 comments 7115973498 and 7116082949). Nothing else is approved. */
    private static final Set<String> LEVI_APPROVED = new HashSet<>(Arrays.asList(
            "eternal_progression", "holy_spirit_force", "baptism_for_dead", "church_identity_question"));

    private static JSONArray bundledEntries() throws Exception {
        String json = new String(Files.readAllBytes(new java.io.File(
                "src/main/assets/ai/guide/comparative/comparative-kb.json").toPath()), StandardCharsets.UTF_8);
        return new JSONObject(json).getJSONArray("entries");
    }

    /** The gate passes exactly the four Levi-approved entries from the bundled knowledge base, and no others. */
    @Test
    public void onlyTheFourLeviApprovedEntriesReachTheModel() throws Exception {
        JSONArray entries = bundledEntries();
        for (String id : DriftDetector.conceptIds()) {
            if (LEVI_APPROVED.contains(id)) {
                assertNotNull("approved entry must reach the model: " + id,
                        ComparativeGate.approvedEntry(entries, id));
            } else {
                assertNull("pending entry must not reach the model: " + id,
                        ComparativeGate.approvedEntry(entries, id));
            }
        }
        assertEquals("exactly four approved entries in the bundled knowledge base", 4,
                countApproved(entries));
    }

    /** Negative test: a pending entry with complete, valid content and sources is still excluded. */
    @Test
    public void pendingEntryWithValidContentAndSourcesIsExcluded() throws Exception {
        JSONObject complete = new JSONObject()
                .put("id", "pending_with_valid_content")
                .put("status", "DRAFT_PENDING_LEVI")
                .put("summary", "A complete, carefully sourced summary of the teaching that is long enough to pass every check.")
                .put("origin", "A complete origin paragraph that is long enough to pass every check for the note.")
                .put("scripture", "A complete Scripture evaluation paragraph that is long enough to pass every check here.")
                .put("anchors", new JSONArray().put("John 1:1"))
                .put("sources", new JSONArray().put(new JSONObject().put("title", "Primary source")));
        JSONArray entries = new JSONArray().put(complete);
        assertNull("a valid-looking pending entry must not be approved",
                ComparativeGate.approvedEntry(entries, "pending_with_valid_content"));
    }

    /** Negative test: flipping any bundled pending entry to the approved status is the only way it could reach the model. */
    @Test
    public void approvalRequiresTheExactStatusNotAnyOtherSpelling() throws Exception {
        JSONArray entries = new JSONArray().put(new JSONObject().put("id", "karma")
                .put("status", "APPROVED_LEVI_PENDING"));
        assertNull(ComparativeGate.approvedEntry(entries, "karma"));
    }

    private static int countApproved(JSONArray entries) {
        int n = 0;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry != null && ComparativeGate.APPROVED_STATUS.equals(entry.optString("status"))) n++;
        }
        return n;
    }
}
