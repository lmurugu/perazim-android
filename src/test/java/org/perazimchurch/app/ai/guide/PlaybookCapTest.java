package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * GuideBlock cuts each playbook to PLAYBOOK_CAP characters. Anything past the cap never reaches the model, so every
 * bundled playbook must fit in full, with its most important rule first.
 */
public class PlaybookCapTest {

    @Test
    public void everyPlaybookFitsWithinTheGuideBlockCap() throws Exception {
        GuideDocs docs = GuideTestSupport.docs();
        for (GuideDocs.SkillEntry skill : docs.index()) {
            String text = docs.playbook(skill.playbook);
            assertTrue(skill.playbook + " is " + text.length() + " characters; the cap is "
                    + GuideBlock.PLAYBOOK_CAP, text.length() <= GuideBlock.PLAYBOOK_CAP);
        }
    }

    @Test
    public void crisisPlaybookKeepsEmergencyHelpFirst() throws Exception {
        String crisis = GuideTestSupport.docs().playbook("playbooks/crisis.md");
        assertTrue(crisis, crisis.startsWith("Urge emergency help now: 999 or 112 in Kenya"));
        assertTrue(crisis, crisis.contains("No confidentiality promises."));
    }
}
