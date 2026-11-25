package org.perazimchurch.app.ai.library;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdentifyRefuteTest {

    private static KjvVerseText kjv() throws Exception {
        return KjvVerseText.fromFiles(new File("src/main/assets/bible/text/KJV.tsv.gz"),
                new File("src/main/assets/bible/kjv_manifest.json"));
    }

    private static JSONObject entry(String id) throws Exception {
        JSONObject root = new JSONObject(new String(java.nio.file.Files.readAllBytes(
                new File("src/main/assets/ai/guide/comparative/comparative-kb.json").toPath()), "UTF-8"));
        JSONArray entries = root.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            if (id.equals(entries.getJSONObject(i).optString("id"))) return entries.getJSONObject(i);
        }
        throw new AssertionError("no entry " + id);
    }

    @Test
    public void jehovahsWitnessAnswerIdentifiesTeachingAndQuotesKjv() throws Exception {
        String answer = IdentifyRefute.answerFor("Are Jehovah's Witnesses real Christians?", entry("jesus_created"), kjv());
        assertNotNull(answer);
        assertTrue(answer.startsWith("What Jehovah's Witnesses teaches, in its own words:"));
        assertTrue(answer.contains("John 1:1: “In the beginning was the Word"));
        assertTrue(answer.contains("Colossians 1:16:"));
        assertTrue(answer.contains(IdentifyRefute.CLOSING));
    }

    @Test
    public void answerNeverNamesTheGuideOrPerazim() throws Exception {
        for (String id : new String[] {"jesus_created", "eternal_progression", "purgatory", "mary_mediator"}) {
            String q = id.equals("jesus_created") ? "Jehovah's Witnesses"
                    : id.equals("eternal_progression") ? "Latter-day Saints" : id.equals("purgatory") ? "Catholic purgatory" : "Catholic Mary";
            String answer = IdentifyRefute.answerFor(q, entry(id), kjv());
            assertNotNull(id, answer);
            assertFalse(id, answer.toLowerCase().contains("guide"));
            assertFalse(id, answer.contains("Perazim"));
            assertFalse(id, answer.contains("ESV") || answer.contains("NIV") || answer.contains("NKJV"));
        }
    }

    @Test
    public void latterDaySaintAnswerHasAtLeastTwoVerses() throws Exception {
        String answer = IdentifyRefute.answerFor("Is the Latter-day Saint church Christian?", entry("eternal_progression"), kjv());
        assertNotNull(answer);
        assertTrue(answer.contains("Isaiah 43:10:"));
        assertTrue(answer.contains("Romans 6:23:"));
    }

    @Test
    public void nonFalseDoctrineEntryOrUnknownTopicGivesNoAnswer() throws Exception {
        assertNull(IdentifyRefute.answerFor("Are Jehovah's Witnesses real Christians?", entry("karma"), kjv()));
        assertNull(IdentifyRefute.answerFor("What is the weather?", entry("jesus_created"), kjv()));
    }

    @Test
    public void topicMapsToApprovedEntry() {
        assertEquals("jesus_created", IdentifyRefute.entryIdFor("Are Jehovah's Witnesses saved?"));
        assertEquals("eternal_progression", IdentifyRefute.entryIdFor("What do Mormons teach?"));
        assertEquals("purgatory", IdentifyRefute.entryIdFor("What do Catholics believe about purgatory?"));
        assertNull(IdentifyRefute.entryIdFor("Explain the parable of the sower"));
    }

    @Test
    public void metaSentencesAreRemovedFromTeaching() {
        String teaching = IdentifyRefute.teachingFrom("Group X teaches Y. The guide explains this before evaluating it. Group X also says Z.");
        assertTrue(teaching.contains("Group X teaches Y."));
        assertFalse(teaching.toLowerCase().contains("guide"));
    }

    @Test
    public void gateIsOffUntilLeviApproves() {
        assertFalse(IdentifyRefute.ENABLED);
    }

    @Test
    public void printSamplesForReview() throws Exception {
        System.out.println("SAMPLE_JW\n" + IdentifyRefute.answerFor("Are Jehovah's Witnesses real Christians?", entry("jesus_created"), kjv()));
        System.out.println("SAMPLE_LDS\n" + IdentifyRefute.answerFor("Is the Latter-day Saint church Christian?", entry("eternal_progression"), kjv()));
        System.out.println("SAMPLE_CATHOLIC\n" + IdentifyRefute.answerFor("What do Catholics believe about purgatory?", entry("purgatory"), kjv()));
    }
}
