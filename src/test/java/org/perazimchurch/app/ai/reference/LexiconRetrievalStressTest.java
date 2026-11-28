package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Runs the real lexicon path (packaged database, approvals, evidence) for word-meaning questions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LexiconRetrievalStressTest {

    /** ἀγάπη (agape), written with escapes so the source stays ASCII and NFC. */
    private static final String GREEK_AGAPE = "ἀγάπη";
    /** אָב (av, father) as stored in BDB. */
    private static final String HEBREW_AV = "אָב";

    private static JSONArray scripture(int count) throws Exception {
        JSONArray array = new JSONArray();
        for (int i = 0; i < count; i++) {
            array.put(new JSONObject()
                    .put("sourceId", "kjv")
                    .put("title", "King James Version")
                    .put("authorityClass", "SCRIPTURE")
                    .put("location", "John 3:" + (16 + i))
                    .put("text", "For God so loved the world."));
        }
        return array;
    }

    private static int countLexical(JSONArray evidence) throws Exception {
        int n = 0;
        for (int i = 0; i < evidence.length(); i++) {
            if ("LEXICAL_REFERENCE".equals(evidence.getJSONObject(i).getString("authorityClass"))) n++;
        }
        return n;
    }

    @Test
    public void strongsGreekNumberAddsLexicalCardAfterScripture() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, scripture(1), "What does G26 mean?");
        assertEquals("SCRIPTURE", evidence.getJSONObject(0).getString("authorityClass"));
        assertTrue("No lexical card for G26", countLexical(evidence) >= 1);
        JSONObject card = evidence.getJSONObject(evidence.length() - 1);
        assertTrue(card.getString("sourceId").startsWith("LEX-"));
        assertTrue(card.getString("text"), card.getString("text").contains("G26"));
    }

    @Test
    public void greekWordQuestionAddsLexicalCard() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, scripture(1), "What does " + GREEK_AGAPE + " mean?");
        assertTrue("No lexical card for the Greek word", countLexical(evidence) >= 1);
    }

    @Test
    public void pointedHebrewWordQuestionAddsLexicalCard() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, scripture(1), "What does " + HEBREW_AV + " mean?");
        assertTrue("No lexical card for the pointed Hebrew word", countLexical(evidence) >= 1);
    }

    /** Release composition: no Hebrew Strong's cards until TBESH or a Strong's map is approved (Q6). */
    @Test
    public void hebrewStrongsNumberAddsNoCardInReleasePack() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, scripture(1), "What does H1 mean?");
        assertEquals(1, evidence.length());
    }

    @Test
    public void strongsNumberDoesNotReturnLongerNumbers() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, new JSONArray(), "What does G1 mean?");
        for (int i = 0; i < evidence.length(); i++) {
            String text = evidence.getJSONObject(i).getString("text");
            assertFalse("G1 matched a longer number: " + text, text.matches("(?s).*Word: G1\\d.*"));
        }
    }

    @Test
    public void unknownStrongsNumberLeavesEvidenceUnchanged() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, scripture(1), "What does G9999 mean?");
        assertEquals(1, evidence.length());
    }

    @Test
    public void questionWithoutWordLeavesEvidenceUnchanged() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        JSONArray evidence = LexiconGuide.augment(context, scripture(1), "Tell me about prayer");
        assertEquals(1, evidence.length());
        assertEquals(0, countLexical(evidence));
    }
}
