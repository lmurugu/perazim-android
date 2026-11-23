package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.Test;

public class GuideMemoryTest {
    private static final long T0 = 1_000_000L;

    private static GuideMemory open(File dir) throws Exception {
        return GuideMemory.open(GuideTestSupport.docs(), new GuideVault(dir, GuideTestSupport.testCipher()));
    }

    @Test
    public void contextCarriesRulesPlaybookAndStyleWithinBudget() throws Exception {
        GuideMemory memory = open(GuideTestSupport.tempDir());
        GuideBlock block = memory.contextFor("I feel afraid about my job and money", T0);
        assertTrue(block.text.startsWith("GUIDE RULES (locked, always apply):"));
        assertTrue(block.text.contains("PLAYBOOK playbooks/pastoral.md"));
        assertTrue(block.text.length() <= GuideBlock.BUDGET_CHARS);
    }

    @Test
    public void sensitiveQuestionsAreNeverRecorded() throws Exception {
        File dir = GuideTestSupport.tempDir();
        GuideMemory memory = open(dir);
        memory.afterAnswer("My husband hit me again and I'm scared", java.util.Collections.<String>emptyList(), true, T0);
        memory.afterAnswer("I want to die", java.util.Collections.<String>emptyList(), false, T0);
        memory.feedback(false, "I feel depressed and hopeless", "A long answer.", T0);
        String described = memory.describe(T0);
        assertTrue(described.contains("Topics asked about (counts only): none yet"));
        assertFalse(new File(dir, GuideMemory.LESSONS_FILE).exists());
    }

    @Test
    public void ordinaryQuestionsRecordTopicCountsAndMood() throws Exception {
        GuideMemory memory = open(GuideTestSupport.tempDir());
        memory.contextFor("What is the meaning of Romans 8 in context?", T0);
        memory.afterAnswer("What is the meaning of Romans 8 in context?", java.util.Collections.<String>emptyList(), false, T0);
        memory.afterAnswer("Thank you, that is helpful", java.util.Collections.<String>emptyList(), false, T0);
        String described = memory.describe(T0);
        assertTrue(described.contains("verse-lookup 1"));
    }

    @Test
    public void explicitStyleRequestChangesStyleLine() throws Exception {
        GuideMemory memory = open(GuideTestSupport.tempDir());
        memory.contextFor("Please make it shorter", T0);
        memory.afterAnswer("Please make it shorter", java.util.Collections.<String>emptyList(), false, T0);
        GuideBlock block = memory.contextFor("What is grace?", T0 + 1);
        assertTrue(block.text.contains("short replies"));
    }

    @Test
    public void correctionsMustBeStyleOnly() throws Exception {
        GuideMemory memory = open(GuideTestSupport.tempDir());
        memory.contextFor("What is grace?", T0);
        assertEquals(GuideMemory.Outcome.REJECTED_NOT_STYLE,
                memory.correction("What is grace?", "Grace is earned by good works, not faith.", T0));
        assertEquals(GuideMemory.Outcome.SAVED,
                memory.correction("What is grace?", "Please answer in shorter sentences.", T0));
        assertTrue(memory.describe(T0).contains("Please answer in shorter sentences."));
        assertFalse(memory.describe(T0).contains("earned by good works"));
    }

    @Test
    public void thumbsDownRemovesLessonsThatKeepFailing() throws Exception {
        File dir = GuideTestSupport.tempDir();
        GuideMemory memory = open(dir);
        memory.correction("Explain grace", "Keep answers to plain words.", T0);
        for (int i = 0; i < 3; i++) {
            memory.contextFor("Explain grace simply", T0 + i);
            memory.feedback(false, "Explain grace simply", "An answer.", T0 + i);
        }
        assertFalse(memory.describe(T0 + 5).contains("Keep answers to plain words."));
    }

    @Test
    public void lessonsPersistAcrossReopenAndAreEncryptedAtRest() throws Exception {
        File dir = GuideTestSupport.tempDir();
        GuideMemory first = open(dir);
        first.correction("Explain grace", "Keep answers to plain words.", T0);
        first.setDisplayName("Asha");
        GuideMemory second = open(dir);
        assertTrue(second.describe(T0).contains("Keep answers to plain words."));
        assertTrue(second.describe(T0).contains("Name to use: Asha"));
        for (File f : dir.listFiles()) {
            byte[] raw = Files.readAllBytes(f.toPath());
            assertFalse("plaintext found in " + f.getName(),
                    new String(raw, "UTF-8").contains("Keep answers to plain words."));
            assertFalse(new String(raw, "UTF-8").contains("Asha"));
        }
    }

    @Test
    public void forgetAllRemovesPersonalFilesButKeepsBundledDocs() throws Exception {
        File dir = GuideTestSupport.tempDir();
        GuideMemory memory = open(dir);
        memory.correction("Explain grace", "Keep answers to plain words.", T0);
        memory.setDisplayName("Asha");
        memory.forgetAll();
        assertEquals(0, dir.listFiles().length);
        GuideMemory reopened = open(dir);
        assertFalse(reopened.describe(T0).contains("Keep answers to plain words."));
        assertTrue(reopened.describe(T0).contains("Name to use: not set"));
        assertTrue(reopened.contextFor("What is grace?", T0).text.startsWith("GUIDE RULES"));
    }

    @Test
    public void damagedVaultFilesAreTreatedAsEmpty() throws Exception {
        File dir = GuideTestSupport.tempDir();
        GuideMemory memory = open(dir);
        memory.correction("Explain grace", "Keep answers to plain words.", T0);
        File lessons = new File(dir, GuideMemory.LESSONS_FILE);
        Files.write(lessons.toPath(), new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17});
        GuideMemory reopened = open(dir);
        assertNotNull(reopened.describe(T0));
        assertFalse(reopened.describe(T0).contains("Keep answers to plain words."));
    }

    @Test
    public void nameAndToneAreSanitised() throws Exception {
        GuideMemory memory = open(GuideTestSupport.tempDir());
        memory.setDisplayName("Asha<script>1</script>");
        assertTrue(memory.describe(T0).contains("Name to use: Ashascript"));
        memory.setTone("shouting");
        assertTrue(memory.describe(T0).contains("Tone: warm"));
    }
}
