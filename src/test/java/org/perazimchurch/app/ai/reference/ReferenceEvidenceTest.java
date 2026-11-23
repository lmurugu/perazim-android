package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ReferenceEvidenceTest {

    private static ReferenceHit dictionaryHit(String headword, String text) {
        return new ReferenceHit(ReferenceHit.KIND_DICTIONARY, "smith-bible-dictionary",
                "Smith's Bible Dictionary", "William Smith", "Public domain", "REFERENCE_ONLY", headword, text, 0);
    }

    @Test
    public void dictionaryEvidenceIsLabelledReferenceAndNeverScripture() throws Exception {
        JSONObject item = ReferenceEvidence.toEvidence(dictionaryHit("Aaron", "The brother of Moses."));
        assertEquals("REF-SMITH-BIBLE-DICTIONARY", item.getString("sourceId"));
        assertEquals("REFERENCE", item.getString("authorityClass"));
        assertEquals("Aaron", item.getString("location"));
        assertEquals("William Smith", item.getString("author"));
        assertEquals("Public domain", item.getString("licence"));
        assertEquals("Reference material, not Perazim's position.", item.getString("notice"));
        assertEquals("The brother of Moses.", item.getString("text"));
    }

    @Test
    public void reservedCitationCharactersAreRemovedFromLocation() throws Exception {
        JSONObject item = ReferenceEvidence.toEvidence(dictionaryHit("A]b@c:d", "text"));
        String location = item.getString("location");
        assertFalse(location.contains("]"));
        assertFalse(location.contains("@"));
        assertFalse(location.contains(":"));
    }

    @Test
    public void longTextIsBounded() throws Exception {
        StringBuilder long600 = new StringBuilder();
        for (int i = 0; i < 1000; i++) long600.append('x');
        JSONObject item = ReferenceEvidence.toEvidence(dictionaryHit("Long", long600.toString()));
        assertEquals(ReferenceEvidence.MAX_TEXT_CHARS, item.getString("text").length());
    }

    @Test
    public void crossReferenceTextNamesTheTargetAndVotes() throws Exception {
        ReferenceHit hit = new ReferenceHit(ReferenceHit.KIND_CROSS_REFERENCE, "openbible-xrefs",
                "OpenBible.info cross-references", "OpenBible.info", "CC BY 4.0", "NONE",
                "Romans 8:28", "Rom 8:28-Rom 8:29", 120);
        String text = ReferenceEvidence.toEvidence(hit).getString("text");
        assertTrue(text.contains("Romans 8:28"));
        assertTrue(text.contains("120 votes"));
    }

    @Test
    public void appendKeepsOriginalAndCapsAdditions() throws Exception {
        JSONArray original = new JSONArray();
        original.put(new JSONObject().put("sourceId", "KJV"));
        JSONArray result = ReferenceEvidence.append(original, Arrays.asList(
                dictionaryHit("A", "one"), dictionaryHit("B", "two"), dictionaryHit("C", "three")), 2);
        assertEquals(3, result.length());
        assertEquals(1, original.length());
        assertEquals("KJV", result.getJSONObject(0).getString("sourceId"));
    }

    @Test
    public void appendSkipsEmptyHitsAndNullInputs() throws Exception {
        assertEquals(0, ReferenceEvidence.append(null, null, 2).length());
        JSONArray result = ReferenceEvidence.append(new JSONArray(),
                Collections.singletonList(dictionaryHit("Empty", "  ")), 2);
        assertEquals(0, result.length());
    }

    @Test
    public void commentaryExcerptNamesAuthorAndTraditionBeforeItsText() throws Exception {
        ReferenceHit henry = new ReferenceHit(ReferenceHit.KIND_COMMENTARY, "matthew-henry-commentary",
                "Matthew Henry's Commentary on the Whole Bible", "Matthew Henry", "Public domain",
                "PURITAN_REFORMED_COMMENTARY", "Genesis 1", "In the beginning God created the heaven.", 0);
        JSONObject item = ReferenceEvidence.toEvidence(henry);
        assertEquals("REFERENCE", item.getString("authorityClass"));
        assertEquals("Matthew Henry (Puritan, Reformed), commentary on Genesis 1: In the beginning God created the heaven.",
                item.getString("text"));
        assertEquals("Reference material, not Perazim's position.", item.getString("notice"));
    }

    @Test
    public void wesleyExcerptIsLabelledArminianWesleyan() {
        assertEquals("Arminian, Wesleyan", ReferenceEvidence.tradition("ARMINIAN_WESLEYAN_COMMENTARY"));
        assertEquals("", ReferenceEvidence.tradition("REFERENCE_ONLY"));
        assertEquals("John Wesley (Arminian, Wesleyan), commentary on Mark 1: A note.",
                ReferenceEvidence.commentaryText("John Wesley", "ARMINIAN_WESLEYAN_COMMENTARY", "Mark 1", "A note."));
    }
}
