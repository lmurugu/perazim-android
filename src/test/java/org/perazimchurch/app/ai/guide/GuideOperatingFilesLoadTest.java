package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * The guide must load its operating files: AGENTS.md (Agents.md), Skills.md, the playbooks they name, and
 * STATUS.md (Status.md), which must carry the NO-GO ZONES section that the evaluation reports against.
 */
public class GuideOperatingFilesLoadTest {
    private static final File GUIDE = new File("src/main/assets/ai/guide");

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(GUIDE, path).toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void agentsSkillsAndPlaybooksLoad() throws IOException {
        GuideDocs docs = GuideDocs.load(GuideOperatingFilesLoadTest::read);
        assertFalse("Agents.md must carry a version", docs.version().trim().isEmpty());
        assertFalse("Skills.md must index at least one skill", docs.index().isEmpty());
        for (GuideDocs.SkillEntry entry : docs.index()) {
            assertFalse("playbook " + entry.playbook + " must load", docs.playbook(entry.playbook) == null
                    || docs.playbook(entry.playbook).trim().isEmpty());
        }
    }

    @Test
    public void statusFileIsPresentAndDeclaresNoGoZones() throws IOException {
        String status = read("Status.md");
        assertTrue("Status.md must have a NO-GO ZONES section", status.contains("NO-GO ZONES"));
        assertTrue("Status.md must name the evaluation gates", status.contains("100%") && status.contains("98%"));
    }
}
