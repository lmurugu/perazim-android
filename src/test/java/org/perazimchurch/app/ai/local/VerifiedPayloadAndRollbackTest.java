package org.perazimchurch.app.ai.local;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Synthetic tests for S2-01 canonical quoting, the signed governance payload contract and the rollback guard.
 * Expected quoting strings were produced by Node JSON.stringify, the package signer. No real data is used.
 */
public class VerifiedPayloadAndRollbackTest {

    private static final long PUBLISHED = 1_700_000_100_000L;

    // ---- S2-01: canonical quoting must match the Node signer -------------------------------------------

    @Test public void curlyQuotesDashesAndControlRangesQuoteAsNodeDoes() {
        assertEquals("\"Peace \u2019 grace \u2014 hope\"", CanonicalJson.jsonString("Peace \u2019 grace \u2014 hope"));
        assertEquals("\"x\u0080y\u009f\"", CanonicalJson.jsonString("x\u0080y\u009f"));
        assertEquals("\"\u2000\u20ff ok\"", CanonicalJson.jsonString("\u2000\u20ff ok"));
    }

    @Test public void slashAfterLessThanIsNotEscaped() {
        assertEquals("\"a</b>\"", CanonicalJson.jsonString("a</b>"));
    }

    @Test public void standardEscapesAreUnchanged() {
        assertEquals("\"tab\\there\\nnl \\\"q\\\" back\\\\slash \\u0001\"",
                CanonicalJson.jsonString("tab\there\nnl \"q\" back\\slash \u0001"));
    }

    // ---- Signed governance array ------------------------------------------------------------------------

    private static JSONObject validEntry() throws Exception {
        return new JSONObject().put("id", "syn-1").put("version", "1")
                .put("category", SourceGovernance.CATEGORY_CHURCH_APPROVED)
                .put("approvedBy", "synthetic-approver").put("approvedAt", 1_700_000_000_000L)
                .put("scope", "OFFLINE:OFFLINE").put("sha256", "a".repeat(64));
    }

    private static JSONArray arr(JSONObject... items) {
        JSONArray array = new JSONArray();
        for (JSONObject item : items) array.put(item);
        return array;
    }

    @Test public void absentGovernanceMeansNoAuthority() {
        assertTrue(SourceGovernance.fromVerifiedPayload(null, PUBLISHED).isEmpty());
    }

    @Test public void validEntryParsesWithApprovalTimeAsEpochMillis() throws Exception {
        List<SourceGovernance.Source> out = SourceGovernance.fromVerifiedPayload(arr(validEntry()), PUBLISHED);
        assertEquals(1, out.size());
        assertEquals("1700000000000", out.get(0).approvedAt);
        assertEquals("synthetic-approver", out.get(0).approvedBy);
    }

    @Test public void stringApprovalTimeRejectsThePackage() throws Exception {
        JSONObject entry = validEntry().put("approvedAt", "2026-11-20");
        assertRejected(arr(entry));
    }

    @Test public void fractionalApprovalTimeRejectsThePackage() throws Exception {
        JSONObject entry = validEntry().put("approvedAt", 1.7e12);
        assertRejected(arr(entry));
    }

    @Test public void approvalAfterPublicationRejectsThePackage() throws Exception {
        JSONObject entry = validEntry().put("approvedAt", PUBLISHED + 1);
        assertRejected(arr(entry));
    }

    @Test public void nonBooleanRevokedRejectsThePackage() throws Exception {
        JSONObject entry = validEntry().put("revoked", "true");
        assertRejected(arr(entry));
    }

    @Test public void missingIdentityRejectsThePackage() throws Exception {
        JSONObject entry = validEntry();
        entry.remove("version");
        assertRejected(arr(entry));
    }

    @Test public void nonObjectEntryRejectsThePackage() throws Exception {
        assertRejected(new JSONArray().put("not-an-object"));
    }

    private static void assertRejected(JSONArray governance) {
        try {
            SourceGovernance.fromVerifiedPayload(governance, PUBLISHED);
            fail("expected the package to be rejected");
        } catch (SecurityException expected) {
            // fail closed
        }
    }

    // ---- Rollback guard ---------------------------------------------------------------------------------

    private static JSONArray docs(String sourceId, String version) throws Exception {
        return new JSONArray().put(new JSONObject().put("sourceId", sourceId).put("version", version)
                .put("text", "synthetic"));
    }

    private static List<SourceGovernance.Source> gov(JSONObject... entries) {
        return SourceGovernance.fromVerifiedPayload(arr(entries), PUBLISHED);
    }

    @Test public void cleanRollbackTargetIsAllowed() throws Exception {
        GovernedEvidence.assertRollbackAllowed(gov(validEntry()), docs("syn-1", "1"), new HashSet<String>());
    }

    @Test public void rollbackToRevokedSourceIsRefused() throws Exception {
        JSONObject revoked = validEntry().put("revoked", true);
        assertRollbackRefused(gov(revoked), docs("syn-1", "1"), new HashSet<String>());
    }

    @Test public void rollbackToSupersededSourceIsRefused() throws Exception {
        JSONObject superseded = validEntry().put("supersededBy", "syn-2");
        assertRollbackRefused(gov(superseded), docs("syn-1", "1"), new HashSet<String>());
    }

    @Test public void rollbackToWithdrawnDocumentIsRefused() throws Exception {
        HashSet<String> withdrawn = new HashSet<>(Collections.singleton("syn-1\u00001"));
        assertRollbackRefused(gov(validEntry()), docs("syn-1", "1"), withdrawn);
    }

    @Test public void rollbackWithUnreadableWithdrawalStatusIsRefusedWhenItHasDocuments() throws Exception {
        assertRollbackRefused(gov(validEntry()), docs("syn-1", "1"), null);
    }

    @Test public void emptySnapshotRollbackIsAllowedEvenWithoutWithdrawalStatus() throws Exception {
        GovernedEvidence.assertRollbackAllowed(gov(), new JSONArray(), null);
    }

    private static void assertRollbackRefused(List<SourceGovernance.Source> governance, JSONArray documents,
                                              java.util.Set<String> withdrawn) {
        try {
            GovernedEvidence.assertRollbackAllowed(governance, documents, withdrawn);
            fail("expected rollback to be refused");
        } catch (SecurityException expected) {
            // fail closed
        }
    }
}
