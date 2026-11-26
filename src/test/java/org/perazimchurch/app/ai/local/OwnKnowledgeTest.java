package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.perazimchurch.app.ai.library.KjvVerseText;
import org.perazimchurch.app.ai.library.ScriptureRangeTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Own-knowledge answers: which questions qualify, and which model replies the checks accept (round 48). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OwnKnowledgeTest {

    @Test
    public void onlyInScopeFaithQuestionsQualify() {
        assertTrue(OwnKnowledge.allowed("Can a Christian join a chama that charges interest?"));
        assertTrue(OwnKnowledge.allowed("What does the Bible say about patience at work?"));
        assertFalse(OwnKnowledge.allowed("Explain the causes of unemployment in Kenya."));
        assertFalse(OwnKnowledge.allowed("What is the best M-Pesa paybill for paying KPLC?"));
        assertFalse("general science mentioning the Bible",
                OwnKnowledge.allowed("Explain a scientific theory that appears to contradict the Bible."));
        assertFalse(OwnKnowledge.allowed(null));
    }

    @Test
    public void acceptsOnlyReplyThatPassesReferenceAndQuoteChecks() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        KjvVerseText kv = KjvVerseText.load(c);
        ScriptureRangeTable table = OwnKnowledge.loadTable(c);
        assertNotNull(OwnKnowledge.accept("Jesus wept at the tomb of Lazarus, and this shows His compassion (John 11:35). "
                + "Christians can bring their grief to Him in prayer, as Hebrews 4:16 invites us to do.", kv, table));
        assertNotNull(OwnKnowledge.accept("God so loved the world that He gave His Son (John 3:16). He loves us first, "
                + "and our response is faith, as Ephesians 2:8 teaches us plainly, and the verse “For God so loved the world” shows it.", kv, table));
        assertNull("one reference only", OwnKnowledge.accept("Jesus wept at the tomb of Lazarus, and this shows His compassion "
                + "(John 11:35). Christians can bring their grief to Him in prayer and trust His care for them.", kv, table));
        assertNull("a verdict on a person", OwnKnowledge.accept("God is punishing you because your business has failed (Job 5:17). "
                + "Scripture is clear about this, as Proverbs 3:11 also shows in the way the Lord corrects His children.", kv, table));
        assertNull("a reference that does not exist", OwnKnowledge.accept("Read Romans 99:1 for this teaching, which is clear and "
                + "very helpful for every believer who is looking for comfort in the Bible today.", kv, table));
        assertNull("another translation named", OwnKnowledge.accept("The ESV puts this clearly in John 3:16, which every believer "
                + "should read and remember and talk about with their pastor in the church.", kv, table));
        assertNull("a quote that is not KJV wording", OwnKnowledge.accept("The Bible says “God helps those who help themselves” "
                + "in several places, so Christians should act with diligence and trust God for the result.", kv, table));
        assertNull("an official-position claim", OwnKnowledge.accept("The official Perazim position is that every believer must "
                + "pay a fixed amount of money to the church, as the Bible requires in this matter.", kv, table));
        assertNull("too short", OwnKnowledge.accept("Yes, God loves you.", kv, table));
    }

    /** The gap log reads literal statuses, not reply text (coordinator, 2026-11-25). */
    @Test
    public void literalStatusesForNoSourceAndScriptureOnly() {
        assertEquals("refusal status", "refused:no-verified-source", ReplyStatus.of(OnDeviceAi.NO_SOURCE_MESSAGE));
        assertEquals("scripture-only status", "scripture-only",
                ReplyStatus.of(OwnKnowledge.SCRIPTURE_ONLY_LEAD + "John 1:1 (King James Version): x\nA fuller explanation is being prepared."));
        assertNull("an ordinary answer has no literal fallback status", ReplyStatus.of("The Lord is my shepherd."));
    }

    /**
     * Chat-path regression (coordinator, 2026-11-25): an unquoted English word search must return the KJV concordance, not the
     * no-source reply. The phone answered "Where is the word grace found?" with the refusal after the early-return fix was missing.
     */
    @Test
    public void unquotedWordSearchGetsConcordanceNotRefusal() {
        Context c = ApplicationProvider.getApplicationContext();
        PreModel.Decision d = PreModel.decide(c, "Where is the word grace found?");
        assertNotNull("word search must not fall through to the no-source reply", d);
        assertTrue("status was " + d.status, d.status.startsWith("routed:word-search"));
        assertTrue(d.answer.contains("grace"));
    }

    /**
     * Chat-path regression for the phone wordings (coordinator, 2026-11-25). The on-phone fallback must answer from the library
     * passages, not refuse with the no-source line, and the zones must not decline general faith questions.
     */
    @Test
    public void phoneWordingsGetScriptureAnswersNotRefusals() {
        Context c = ApplicationProvider.getApplicationContext();
        String purgatory = OwnKnowledge.scriptureOnlyReply(c, "Does the Bible teach purgatory?");
        assertNotNull("purgatory wording must get the library's Scripture-only reply", purgatory);
        assertTrue(purgatory.contains("Hebrews 9:27"));
        assertNotNull(OwnKnowledge.scriptureOnlyReply(c, "What is purgatory?"));
        assertTrue(PreModel.scriptureDoctrineQuestion("Does the Bible teach purgatory?"));
        assertTrue(PreModel.generalFaithQuestion("Is it wrong to be angry at God?"));
        assertFalse(PreModel.generalFaithQuestion("I am angry at God because my job failed."));
        assertFalse(PreModel.generalFaithQuestion("Can I send money to the prophet to break a curse?"));
        PreModel.Decision holySpirit = PreModel.decide(c, "Is the Holy Spirit a person or only a force?");
        assertTrue("a Holy Spirit question is not a no-go decline",
                holySpirit == null || !holySpirit.status.startsWith("routed:no-go"));
    }
}
