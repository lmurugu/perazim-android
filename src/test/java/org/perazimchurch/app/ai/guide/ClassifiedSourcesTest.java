package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * Source tiers and classes (Levi, 2026-11-25). Tier 1 Bible first. Tier 2 creeds and early Church Fathers only where they
 * agree with Bible-centred Protestant, Pentecostal and Evangelical teaching. Other faiths and groups are
 * FALSE_DOCTRINE_EXAMPLE only: loaded for drift refutation, never citable, never an approved source.
 */
public class ClassifiedSourcesTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void otherFaithMaterialIsNeverApproved() throws Exception {
        String approvals = read("src/main/assets/reference/approvals.json");
        for (String id : new String[] {"ante-nicene-fathers", "npnf", "comparative-kb-1.5.22"}) {
            assertFalse("must not be approved as a source: " + id, approvals.contains("\"" + id + "\""));
        }
    }

    @Test
    public void churchFathersAreTierTwoCandidatesOnly() throws Exception {
        String sources = read("scripts/reference/sources.json");
        assertTrue(sources.contains("\"tier\": \"TIER_2_CONDITIONAL\""));
        assertTrue(sources.contains("ante-nicene-fathers"));
        assertTrue(sources.contains("npnf"));
    }

    @Test
    public void comparativeMaterialIsOnlyReadByDriftEvidence() throws Exception {
        String[] loaders = {
                "src/main/java/org/perazimchurch/app/ai/guide/DriftDetector.java",
                "src/main/java/org/perazimchurch/app/ai/library/DoctrineLibrary.java",
                "src/main/java/org/perazimchurch/app/ai/reference/ReferenceIndex.java",
        };
        for (String path : loaders) {
            assertFalse(path + " reads the comparative knowledge base", read(path).contains("comparative-kb"));
        }
        assertTrue(read("src/main/java/org/perazimchurch/app/ai/guide/DriftEvidence.java").contains("comparative-kb.json"));
    }

    @Test
    public void otherFaithNotesAreNeverCitable() throws Exception {
        String code = read("src/main/java/org/perazimchurch/app/ai/guide/DriftEvidence.java");
        assertTrue(code.contains("\"FALSE_DOCTRINE_EXAMPLE\""));
        assertTrue(code.contains("item.put(\"citationEligible\", false)"));
        assertTrue(code.contains("MIN_PASSAGES = 2"));
    }
}
