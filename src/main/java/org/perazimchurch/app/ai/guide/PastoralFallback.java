package org.perazimchurch.app.ai.guide;

import java.util.List;

/**
 * A fixed, cited King James verse for a pastoral question when no approved evidence was retrieved. It replaces the
 * "no verified source" error, so the member still hears a word from Scripture. Pure Java so the rules run in host tests.
 */
public final class PastoralFallback {

    /** Psalm 34:18 in the bundled KJV text (assets/bible/text/KJV.tsv.gz). Copied verbatim; the test checks the wording. */
    public static final String LOCATION = "Psalm 34:18";
    public static final String VERSE =
            "The LORD is nigh unto them that are of a broken heart; and saveth such as be of a contrite spirit.";

    private PastoralFallback() {}

    /** True when the question's strongest playbook match is the pastoral skill. */
    public static boolean applies(List<GuideDocs.SkillEntry> index, String question) {
        if (index == null || index.isEmpty() || question == null || question.trim().isEmpty()) return false;
        List<GuideDocs.SkillEntry> routed = GuideRouter.route(question, index, 1);
        return !routed.isEmpty() && "pastoral".equals(routed.get(0).id);
    }

    /** The fixed answer: the verse with its location, then a referral to a trusted leader. */
    public static String answer() {
        return LOCATION + " (King James Version): “" + VERSE + "” "
                + "God's promises do not depend on how you feel. Please also talk with a trusted church leader.";
    }
}
