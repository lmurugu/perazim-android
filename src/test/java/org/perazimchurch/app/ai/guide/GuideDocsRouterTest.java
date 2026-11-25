package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class GuideDocsRouterTest {

    @Test
    public void bundledOperatingFilesLoadWithVersionCoreAndPlaybooks() throws Exception {
        GuideDocs docs = GuideTestSupport.docs();
        assertEquals("1.2.0", docs.version());
        assertTrue(docs.core().startsWith("RULES (locked):"));
        assertFalse("markers must not reach the prompt", docs.core().contains("CORE:"));
        assertEquals(11, docs.index().size());
        for (GuideDocs.SkillEntry skill : docs.index()) {
            assertFalse("empty playbook " + skill.playbook, docs.playbook(skill.playbook).isEmpty());
        }
    }

    @Test
    public void coreRulesKeepTheHardLines() throws Exception {
        String core = GuideTestSupport.docs().core();
        assertTrue(core.contains("Never ESV, NIV or NKJV"));
        assertTrue(core.contains("999 or 112"));
        assertTrue(core.contains("No verdict on anyone's salvation"));
    }

    @Test
    public void routerPicksTheRightSkill() throws Exception {
        List<GuideDocs.SkillEntry> index = GuideTestSupport.docs().index();
        assertEquals("pastoral", first(index, "I'm so afraid about money and losing my job"));
        assertEquals("verse-lookup", first(index, "What does Romans 8 mean in its context?"));
        assertEquals("doctrine", first(index, "Do I have to speak in tongues to be filled with the Holy Spirit?"));
        assertEquals("privacy", first(index, "What do you remember about other members?"));
        assertEquals("human-decision", first(index, "Should I join another church? Tell me the decision."));
        assertEquals("church-info", first(index, "What are the service times at the campus?"));
        assertEquals("app-help", first(index, "Where is the sermon button in the app?"));
    }

    @Test
    public void crisisRanksFirstWhenAnotherSkillAlsoMatches() throws Exception {
        List<GuideDocs.SkillEntry> index = GuideTestSupport.docs().index();
        List<GuideDocs.SkillEntry> routed = GuideRouter.route("I feel afraid and I want to kill myself", index, 3);
        assertEquals("crisis", routed.get(0).id);
    }

    @Test
    public void offTopicQuestionMatchesNothing() throws Exception {
        List<GuideDocs.SkillEntry> index = GuideTestSupport.docs().index();
        assertTrue(GuideRouter.route("Who won the football match yesterday?", index, 2).isEmpty());
    }

    @Test
    public void wholeWordsOnly() throws Exception {
        List<GuideDocs.SkillEntry> index = GuideTestSupport.docs().index();
        assertFalse(GuideRouter.route("I ate a pineapple", index, 2).stream().anyMatch(s -> "crisis".equals(s.id)));
    }

    @Test
    public void textHelpersNormaliseCurlyQuotesAndPhrases() {
        assertEquals("don t worry", GuideText.normalise("Don’t worry!"));
        assertTrue(GuideText.hasPhrase("i am so afraid today", "so afraid"));
        assertFalse(GuideText.hasPhrase("i am so afraidness", "afraid"));
        assertNotNull(GuideText.clean("abc", 10));
    }

    private static String first(List<GuideDocs.SkillEntry> index, String q) {
        List<GuideDocs.SkillEntry> routed = GuideRouter.route(q, index, 2);
        assertFalse("no skill for: " + q, routed.isEmpty());
        return routed.get(0).id;
    }
}
