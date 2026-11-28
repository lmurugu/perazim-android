package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class LexiconEvidenceTest {

    private static ReferenceHit lexiconHit(String sourceId, String headword, String text) {
        return new ReferenceHit(ReferenceHit.KIND_LEXICON, sourceId, "Translators Brief Lexicon, Greek (STEP Bible)",
                "STEPBible.org, Tyndale House Cambridge", "CC BY 4.0 (STEP-Data)", "", headword, text, 0);
    }

    @Test
    public void cardIsLabelledLexicalAndNeverScripture() throws Exception {
        JSONObject item = LexiconEvidence.toEvidence(lexiconHit("step-tbesg",
                "G0026 αγάπη (agapē)", "Meaning: love. Entry: love, goodwill, esteem."));
        assertEquals("LEX-STEP-TBESG", item.getString("sourceId"));
        assertEquals("LEXICAL_REFERENCE", item.getString("authorityClass"));
        assertEquals("CC BY 4.0 (STEP-Data)", item.getString("licence"));
        assertEquals("G0026 αγάπη (agapē)", item.getString("location"));
        assertEquals("Word meaning from a lexicon, not Perazim's position. Scripture decides doctrine.",
                item.getString("notice"));
        assertTrue(item.getString("text").startsWith("Word: G0026"));
        assertTrue(item.getString("text").contains("Meaning: love."));
    }

    @Test
    public void cardsAreAtMost300CharactersAndCutAtAWord() throws Exception {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 200; i++) longText.append("word").append(i).append(' ');
        String text = LexiconEvidence.toEvidence(lexiconHit("step-tbesg", "G0026 x", longText.toString())).getString("text");
        assertTrue(text.length() <= LexiconEvidence.MAX_TEXT_CHARS);
        assertTrue(text.endsWith("..."));
        assertFalse(text.contains("  "));
    }

    @Test
    public void locationCannotCarryCitationCharacters() throws Exception {
        JSONObject item = LexiconEvidence.toEvidence(lexiconHit("step-tbesh", "H0430G] x@y:z", "Meaning: God."));
        String location = item.getString("location");
        assertFalse(location.contains("]"));
        assertFalse(location.contains("@"));
        assertFalse(location.contains(":"));
        assertEquals("LEX-STEP-TBESH", item.getString("sourceId"));
    }

    @Test
    public void appendKeepsScriptureFirstAndCapsLexiconCards() throws Exception {
        JSONArray scripture = new JSONArray();
        JSONObject verse = new JSONObject();
        verse.put("sourceId", "Bible");
        verse.put("authorityClass", "SCRIPTURE");
        verse.put("text", "In the beginning");
        scripture.put(verse);
        List<ReferenceHit> hits = Arrays.asList(
                lexiconHit("step-tbesg", "G0026 a", "Meaning: love."),
                lexiconHit("step-tbesh", "H0001G b", "Meaning: father."),
                lexiconHit("step-tbesg", "G0027 c", "Meaning: other."));
        JSONArray out = LexiconEvidence.append(scripture, hits, LexiconGuide.MAX_CARDS);
        assertEquals(1 + LexiconGuide.MAX_CARDS, out.length());
        assertEquals("SCRIPTURE", out.getJSONObject(0).getString("authorityClass"));
        assertEquals("LEXICAL_REFERENCE", out.getJSONObject(1).getString("authorityClass"));
        assertEquals(1, scripture.length());
    }

    @Test
    public void onlyApprovedLexiconSourcesAreQueried() {
        ReferenceApprovals approvals = ReferenceApprovals.parse(
                "{\"approvedSourceIds\": [\"step-tipnr\", \"step-tbesg\", \"macula-greek-sblgnt\", \"smith-bible-dictionary\"]}");
        assertEquals(Arrays.asList("step-tbesg"), LexiconIndex.approvedSources(approvals));
        assertTrue(LexiconIndex.approvedSources(ReferenceApprovals.none()).isEmpty());
    }

    @Test
    public void shippedApprovalsApproveTheReviewedLexiconsOnly() throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/reference/approvals.json")),
                StandardCharsets.UTF_8);
        ReferenceApprovals approvals = ReferenceApprovals.parse(json);
        assertTrue(approvals.isApproved("step-tbesg"));
        assertTrue(approvals.isApproved("step-tbesh"));
        assertTrue(approvals.isApproved("bdb-openscriptures"));
        assertFalse(approvals.isApproved("step-tipnr"));
        assertFalse(approvals.isApproved("macula-greek-sblgnt"));
    }
}
