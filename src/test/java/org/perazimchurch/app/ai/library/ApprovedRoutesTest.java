package org.perazimchurch.app.ai.library;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/** Levi's 2026-11-25 decisions: each route answers only its question shapes, quotes KJV, and keeps its labels. */
public class ApprovedRoutesTest {

    private static KjvVerseText kjv() throws Exception {
        return KjvVerseText.fromFiles(new File("src/main/assets/bible/text/KJV.tsv.gz"),
                new File("src/main/assets/bible/kjv_manifest.json"));
    }

    private static ApprovedRoutes.Answer route(String question) throws Exception {
        return ApprovedRoutes.answerFor(question, kjv());
    }

    @Test
    public void israelAnswerCarriesTheThreeApprovedCorrections() throws Exception {
        ApprovedRoutes.Answer a = route("What does the Bible say about Israel and the end times?");
        assertNotNull(a);
        assertTrue(a.routeId.equals("israel-end-times"));
        assertTrue(a.text.contains("under review"));
        assertTrue(a.text.contains("Revelation 13:11–14"));
        assertTrue(a.text.contains("Philadelphia"));
        assertTrue(a.text.contains("Daniel 2:34–35"));
        assertTrue(a.text.contains("Romans 11:25:"));
        assertTrue(a.text.contains("Revelation 3:10:"));
        assertTrue(a.text.contains("not a political endorsement"));
    }

    @Test
    public void israelRouteNeverAnswersWithoutTwoVerifiedPassages() throws Exception {
        assertNull(ApprovedRoutes.answerFor("Israel and the end times", null));
    }

    @Test
    public void cessationismIsAnsweredOnlyWhenAskedAndNeverCallsCessationistsHeretics() throws Exception {
        ApprovedRoutes.Answer a = route("What do cessationists believe about the gifts of the Spirit?");
        assertNotNull(a);
        assertTrue(a.routeId.equals("cessationism"));
        assertTrue(a.text.contains("Some cessationists teach that certain sign gifts"));
        assertTrue(a.text.contains("working interpretation"));
        assertTrue(a.text.contains("1 Corinthians 14:1:"));
        assertTrue(a.text.contains("Ephesians 4:11–13"));
        assertTrue(a.text.contains("Acts 2:17–18"));
        assertTrue(a.text.contains("1 Thessalonians 5:19–21"));
        assertTrue(a.text.contains("do not call cessationists heretics"));
        assertFalse(a.text.contains("cessationists are wrong"));
    }

    @Test
    public void generalHolySpiritQuestionDoesNotGetTheCessationistComparison() throws Exception {
        assertNull(route("What is the Holy Spirit and what are His gifts?"));
    }

    @Test
    public void apostasyKeepsAssuranceAndWarningsAndTheUnderReviewView() throws Exception {
        ApprovedRoutes.Answer a = route("Can a Christian lose their salvation?");
        assertNotNull(a);
        assertTrue(a.routeId.equals("apostasy-assurance"));
        assertTrue(a.text.contains("John 10:27–29"));
        assertTrue(a.text.contains("Romans 8:38–39"));
        assertTrue(a.text.contains("Hebrews 6:4–6"));
        assertTrue(a.text.contains("Hebrews 10:26–29"));
        assertTrue(a.text.contains("2 Peter 2:20–22"));
        assertTrue(a.text.contains("genuinely saved person can freely and finally abandon Christ"));
        assertTrue(a.text.contains("under review"));
        assertTrue(a.text.contains("John 10:28:"));
        assertTrue(a.text.contains("We do not judge any person's eternal destiny."));
        assertFalse(a.text.contains("cannot fall away"));
        assertFalse(a.text.contains("will not finally fall away"));
    }

    @Test
    public void electionIsUnderReviewAndDoesNotCancelPreaching() throws Exception {
        ApprovedRoutes.Answer a = route("What is unconditional election?");
        assertNotNull(a);
        assertTrue(a.routeId.equals("unconditional-election"));
        assertTrue(a.text.contains("working interpretation, under review"));
        assertTrue(a.text.contains("does not cancel our responsibility to preach the gospel to everyone"));
    }

    @Test
    public void graceAndObedienceAndSovereigntyAreApprovedOutlines() throws Exception {
        ApprovedRoutes.Answer grace = route("Is it grace and obedience, or works?");
        assertNotNull(grace);
        assertTrue(grace.routeId.equals("grace-and-obedience"));
        assertTrue(grace.text.contains("Ephesians 2:8–9"));
        assertTrue(grace.text.contains("Titus 2:11–12"));
        assertTrue(grace.text.contains("James 2:17, 26"));
        assertTrue(grace.text.contains("Ephesians 2:8:"));
        assertTrue(grace.text.contains("James 2:26:"));
        ApprovedRoutes.Answer sov = route("How do sovereignty and human responsibility fit together?");
        assertNotNull(sov);
        assertTrue(sov.routeId.equals("sovereignty-and-responsibility"));
        assertTrue(sov.text.contains("Ezekiel 18:20"));
        assertTrue(sov.text.contains("Philippians 2:12"));
        assertTrue(sov.text.contains("not use God's sovereignty to excuse sin"));
    }

    @Test
    public void holySpiritEntryNowCarriesTheApprovedGiftsSentenceAfterSpiritBaptism() throws Exception {
        String entries = new String(Files.readAllBytes(new File("src/main/assets/ai/library/doctrine/entries.json").toPath()),
                StandardCharsets.UTF_8);
        String positions = new String(Files.readAllBytes(new File("src/main/assets/ai/library/doctrine/positions.json").toPath()),
                StandardCharsets.UTF_8);
        DoctrineLibrary library = DoctrineLibrary.fromJson(entries, positions);
        String answer = library.answerFor("What is the Holy Spirit and speaking in tongues?");
        assertNotNull(answer);
        assertTrue(answer.startsWith("Spirit baptism is an experience distinct from conversion."));
        assertTrue(answer.contains("The gifts of the Holy Spirit continue to operate in the Church today."));
        assertTrue(answer.contains("This does not mean every believer must speak in tongues."));
        assertTrue(answer.contains("1 Corinthians 12:4–11; Acts 2:38–39; 1 Corinthians 14:1"));
    }

    @Test
    public void marySummaryIsNarrowedToAdditionalMediatorOfGrace() throws Exception {
        String answer = IdentifyRefute.answerFor("What do Catholics teach about Mary?", entry("mary_mediator"), kjv());
        assertNotNull(answer);
        assertTrue(answer.contains("additional mediator of grace"));
        assertTrue(answer.contains("1 Timothy 2:1:"));
        assertTrue(answer.contains("Hebrews 7:25:"));
        assertFalse(answer.contains("Mother of God"));
    }

    @Test
    public void identifyRefuteRemovesNiceneWordingFromTheTeachingSection() throws Exception {
        String answer = IdentifyRefute.answerFor("Are Jehovah's Witnesses Christians?", entry("jesus_created"), kjv());
        assertNotNull(answer);
        assertTrue(answer.contains("John 1:3:"));
        assertFalse(answer.contains("Nicene"));
    }

    private static org.json.JSONObject entry(String id) throws IOException, org.json.JSONException {
        org.json.JSONObject root = new org.json.JSONObject(new String(Files.readAllBytes(
                new File("src/main/assets/ai/guide/comparative/comparative-kb.json").toPath()), StandardCharsets.UTF_8));
        org.json.JSONArray entries = root.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            if (id.equals(entries.getJSONObject(i).optString("id"))) return entries.getJSONObject(i);
        }
        throw new AssertionError("no entry " + id);
    }
}
