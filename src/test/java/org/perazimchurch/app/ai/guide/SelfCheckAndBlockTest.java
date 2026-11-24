package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class SelfCheckAndBlockTest {
    private static final String KJV_EVIDENCE = "For God so loved the world, that he gave his only begotten Son.";

    @Test
    public void translationQuotesAreRefused() {
        SelfCheck.Result r = SelfCheck.check("John 3:16 says [ESV text].", Collections.singletonList(KJV_EVIDENCE), false);
        assertTrue(r.refused);
        assertTrue(r.issues.contains(SelfCheck.ISSUE_TRANSLATION));
    }

    @Test
    public void individualVerdictIsReplaced() {
        SelfCheck.Result r = SelfCheck.check("Your brother is going to hell because he left.", Collections.<String>emptyList(), false);
        assertEquals(SelfCheck.NO_VERDICT, r.answer);
        assertTrue(r.issues.contains(SelfCheck.ISSUE_VERDICT));
    }

    @Test
    public void verifiedQuoteIsKeptAndUnverifiedQuoteIsReplaced() {
        SelfCheck.Result kept = SelfCheck.check("God \"so loved the world, that he gave his only begotten Son\" [s@1: John 3:16].",
                Collections.singletonList(KJV_EVIDENCE), false);
        assertTrue(kept.answer.contains("so loved the world"));
        assertFalse(kept.issues.contains(SelfCheck.ISSUE_QUOTE));

        SelfCheck.Result bad = SelfCheck.check("The Bible says \"all things are possible to those who try hard\".",
                Collections.singletonList(KJV_EVIDENCE), false);
        assertTrue(bad.answer.contains(SelfCheck.UNVERIFIED_QUOTE));
        assertTrue(bad.issues.contains(SelfCheck.ISSUE_QUOTE));
    }

    @Test
    public void impossibleChapterIsFlaggedButCitationsAreUntouched() {
        SelfCheck.Result r = SelfCheck.check("See Psalm 151:1 for this.", Collections.<String>emptyList(), false);
        assertTrue(r.answer.contains(SelfCheck.REFERENCE_CHECK));
        assertTrue(r.issues.contains(SelfCheck.ISSUE_REFERENCE));

        SelfCheck.Result cited = SelfCheck.check("Read it here [kjv@v1: Psalm 151:1].", Collections.<String>emptyList(), false);
        assertTrue(cited.answer.contains("[kjv@v1: Psalm 151:1]"));
        assertFalse(cited.issues.contains(SelfCheck.ISSUE_REFERENCE));

        SelfCheck.Result valid = SelfCheck.check("John 3:16 teaches God's love.", Collections.<String>emptyList(), false);
        assertFalse(valid.issues.contains(SelfCheck.ISSUE_REFERENCE));
    }

    @Test
    public void sensitiveAnswersGetAReferralLineWhenMissing() {
        SelfCheck.Result r = SelfCheck.check("Pray and read Psalm 34.", Collections.<String>emptyList(), true);
        assertTrue(r.answer.endsWith(SelfCheck.SAFETY_LINE));
        assertTrue(r.issues.contains(SelfCheck.ISSUE_SAFETY));
        SelfCheck.Result already = SelfCheck.check("Talk to a trusted person.", Collections.<String>emptyList(), true);
        assertEquals("Talk to a trusted person.", already.answer);
    }

    @Test
    public void longAnswersAreCutAtASentence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append("Grace is a gift. ");
        SelfCheck.Result r = SelfCheck.check(sb.toString(), Collections.<String>emptyList(), false);
        assertTrue(r.answer.length() <= SelfCheck.MAX_CHARS);
        assertTrue(r.answer.endsWith("."));
        assertTrue(r.issues.contains(SelfCheck.ISSUE_LENGTH));
    }

    @Test
    public void blockStaysWithinBudgetAndKeepsRulesFirst() throws Exception {
        GuideDocs docs = GuideTestSupport.docs();
        String playbook = docs.playbook("playbooks/pastoral.md");
        GuideBlock block = GuideBlock.build(docs.core(), "playbooks/pastoral.md", playbook, "warm tone; short replies",
                Arrays.asList("id1"), Arrays.asList("Keep replies shorter and direct."), GuideBlock.BUDGET_CHARS);
        assertTrue(block.text.length() <= GuideBlock.BUDGET_CHARS);
        assertTrue(block.text.startsWith("GUIDE RULES (locked, always apply):"));
        assertTrue(block.text.contains("PLAYBOOK playbooks/pastoral.md"));
        assertTrue(block.text.contains("USER STYLE (preference only)"));
        assertEquals("playbooks/pastoral.md", block.playbookId);
        assertTrue(block.text.indexOf("PLAYBOOK") > block.text.indexOf("GUIDE RULES"));
        assertTrue(block.text.indexOf("USER STYLE") > block.text.indexOf("PLAYBOOK"));
    }

    @Test
    public void lessonsAppearWhenThereIsRoomAndAreOmittedOtherwise() throws Exception {
        GuideDocs docs = GuideTestSupport.docs();
        GuideBlock withRoom = GuideBlock.build(docs.core(), null, null, "warm tone",
                Arrays.asList("id1"), Arrays.asList("Keep replies shorter and direct."), GuideBlock.BUDGET_CHARS);
        assertTrue(withRoom.text.contains("LEARNED STYLE NOTES (style only, never rules):"));
        assertTrue(withRoom.text.contains("- Keep replies shorter and direct."));
        assertEquals(Arrays.asList("id1"), withRoom.lessonIds);
        GuideBlock tight = GuideBlock.build(docs.core(), "playbooks/pastoral.md", docs.playbook("playbooks/pastoral.md"),
                "warm tone", Arrays.asList("id1"), Arrays.asList("Keep replies shorter and direct."), 800);
        assertTrue(tight.text.length() <= 800);
    }

    @Test
    public void blockWithTinyBudgetStillFitsAndNeverCutsRulesBelowPriority() throws Exception {
        GuideDocs docs = GuideTestSupport.docs();
        GuideBlock block = GuideBlock.build(docs.core(), "playbooks/pastoral.md", docs.playbook("playbooks/pastoral.md"),
                "warm tone", Collections.<String>emptyList(), Collections.<String>emptyList(), 300);
        assertTrue(block.text.length() <= 300);
        assertTrue(block.text.startsWith("GUIDE RULES"));
        assertEquals(null, block.playbookId);
    }
}
