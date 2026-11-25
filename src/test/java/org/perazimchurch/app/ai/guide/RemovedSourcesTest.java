package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * Sources removed on 2026-11-25 (Levi: only Bible-centred Protestant, Pentecostal and Evangelical material may be
 * answering or resource material). These tests fail if a removed source is bundled, referenced by a loader, or
 * listed as approved again.
 */
public class RemovedSourcesTest {

    private static final String[] REMOVED_FILES = {
            "src/main/assets/ai/guide/comparative/comparative-kb.json",
            "ai-eval/knowledge-package-unsigned/source/comparative-kb-1.5.22.json",
    };

    private static final String[] REMOVED_IDS = {
            "ante-nicene-fathers",
            "npnf",
            "comparative-kb-1.0.0",
            "comparative-kb-1.5.22",
    };

    @Test
    public void removedFilesAreNotBundled() {
        for (String path : REMOVED_FILES) {
            assertFalse("removed source is back: " + path, new File(path).exists());
        }
    }

    @Test
    public void loadersDoNotReadTheComparativeKnowledgeBase() throws Exception {
        String[] loaders = {
                "src/main/java/org/perazimchurch/app/ai/guide/DriftEvidence.java",
                "src/main/java/org/perazimchurch/app/ai/guide/DriftDetector.java",
                "src/main/java/org/perazimchurch/app/ai/local/OnDeviceAi.java",
        };
        for (String path : loaders) {
            String code = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
            assertFalse(path + " still reads the comparative knowledge base", code.contains("comparative-kb"));
            assertFalse(path + " still uses the comparative source id", code.contains("perazim-comparative-kb"));
        }
    }

    @Test
    public void removedSourcesAreNotApprovedForTheGuide() throws Exception {
        String approvals = new String(Files.readAllBytes(
                new File("src/main/assets/reference/approvals.json").toPath()), StandardCharsets.UTF_8);
        for (String id : REMOVED_IDS) {
            assertFalse("removed source approved again: " + id, approvals.contains("\"" + id + "\""));
        }
    }

    @Test
    public void registerRecordsEachRemovalWithAReason() throws Exception {
        String register = new String(Files.readAllBytes(
                new File("src/main/assets/ai/library/sources/register.json").toPath()), StandardCharsets.UTF_8);
        assertTrue("register must list removed sources", register.contains("\"removedSources\""));
        for (String id : REMOVED_IDS) {
            assertTrue("removal of " + id + " has no entry", register.contains("\"" + id + "\""));
        }
    }
}
