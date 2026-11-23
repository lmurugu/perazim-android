package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class DriftDetectorTest {

    private static List<String> concepts(String question) {
        List<String> ids = new java.util.ArrayList<>();
        for (DriftDetector.Hit hit : DriftDetector.detect(question)) ids.add(hit.conceptId);
        return ids;
    }

    private static void drifts(String question, String concept) {
        assertTrue("expected " + concept + " for: " + question, concepts(question).contains(concept));
    }

    @Test
    public void karmaAndSubtleKarmaDrift() {
        drifts("What is karma?", "karma");
        drifts("is karma just sowing and reaping?", "karma");
        drifts("Does my karma follow me into the next life?", "karma");
        drifts("Is there karma in Christianity?", "karma");
    }

    @Test
    public void afterlifeAndReincarnationDrift() {
        drifts("Do we come back as another person after we die? I've heard about reincarnation", "reincarnation");
        drifts("What is samsara and nirvana?", "reincarnation");
        drifts("How do I reach moksha?", "nirvana_moksha");
    }

    @Test
    public void purgatoryAndSaintPrayerDrift() {
        drifts("Does purgatory exist?", "purgatory");
        drifts("Should I pray to the saints for help?", "saint_prayer");
        drifts("Is Mary our co-redeemer?", "mary_mediator");
        drifts("Why do some people kiss icons and use a rosary?", "icons_veneration");
    }

    @Test
    public void heterodoxGroupProofTextsDrift() {
        drifts("Why do Mormons say after all we can do?", "works_salvation");
        drifts("Do Mormons believe in eternal progression to godhood?", "eternal_progression");
        drifts("Is Jesus the Father? I heard the Oneness teaching", "trinity_denial");
        drifts("My friend says Jesus was created as Michael the archangel", "jesus_created");
        drifts("Do we keep the sabbath on Saturday? Investigative judgment?", "saturday_sabbath");
        drifts("Is the soul dead until resurrection? soul sleep", "soul_sleep");
        drifts("Why do Christian Science teachers say sickness is an illusion?", "christian_science");
    }

    @Test
    public void otherReligionsAndNewAgeDrift() {
        drifts("Is it true that all paths lead to God?", "all_paths");
        drifts("I am God and everyone is divine", "divine_self");
        drifts("Does the law of attraction work? I heard universal energy heals", "new_age_energy");
        drifts("Is there no God, only science?", "secular_worldview");
        drifts("Did someone else die for Jesus on the cross?", "islam_no_crucifixion");
    }

    @Test
    public void canonAndChurchQuestionsRouteToScripture() {
        drifts("The Catholic Bible has more books, which is right?", "canon_books");
        drifts("What are the Apocrypha and Tobit?", "canon_books");
        drifts("Are Catholics going to heaven?", "church_identity_question");
        drifts("Is the Mormon church a cult?", "group_question");
    }

    @Test
    public void ordinaryScriptureQuestionsDoNotDrift() {
        assertFalse(DriftDetector.drifts("What does John 3:16 mean?"));
        assertFalse(DriftDetector.drifts("Explain Galatians 6:7 about sowing and reaping"));
        assertFalse(DriftDetector.drifts("Who was Mary Magdalene in the Bible?"));
        assertFalse(DriftDetector.drifts("How do I pray and grow in faith?"));
        assertFalse(DriftDetector.drifts("Which Bible version should I read for daily devotion?"));
        assertFalse(DriftDetector.drifts("What did Jesus say about the Sabbath in Mark 2?"));
        assertFalse(DriftDetector.drifts("Tell me about the Holy Spirit and spiritual gifts"));
        assertFalse(DriftDetector.drifts("I feel anxious about my job"));
        assertFalse(DriftDetector.drifts(null));
        assertFalse(DriftDetector.drifts("   "));
    }

    @Test
    public void curlyApostropheAndCaseAreNormalised() {
        drifts("Is it true that all paths lead to God? I’m curious about KARMA", "karma");
        assertTrue(DriftDetector.drifts("WHAT ABOUT PURGATORY"));
    }

    @Test
    public void eachConceptIsReportedOnce() {
        List<DriftDetector.Hit> hits = DriftDetector.detect("karma karma karma and more karma");
        assertEquals(1, hits.size());
    }

    @Test
    public void everyConceptIdIsUniqueAndNamed() {
        List<String> ids = DriftDetector.conceptIds();
        assertEquals(ids.size(), new java.util.HashSet<>(ids).size());
        for (String id : ids) assertTrue(id.matches("[a-z_]+"));
    }

    @Test
    public void driftSkillRoutesFirstButCrisisStillRanksAboveIt() throws Exception {
        List<GuideDocs.SkillEntry> index = GuideTestSupport.docs().index();
        assertEquals("drift", GuideRouter.route("What about karma and purgatory?", index, 1).get(0).id);
        assertEquals("crisis", GuideRouter.route("I want to kill myself, is karma real?", index, 1).get(0).id);
        List<GuideDocs.SkillEntry> plain = GuideRouter.route("What does Mark 1:1 say?", index, 1);
        assertTrue(plain.isEmpty() || !"drift".equals(plain.get(0).id));
    }
}
