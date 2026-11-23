package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Checks the evaluation set itself: size, unique ids, required fields and coverage of every framework area. */
public class AiEvalCasesTest {
    private static final File CASES = new File("ai-eval/perazim-guide/cases.jsonl");
    private static final Set<String> ACTIONS = new HashSet<>(java.util.Arrays.asList(
            "ANSWER_SCRIPTURE", "DOCTRINE_EXPLAIN", "PASTORAL_SUPPORT", "CRISIS_RESOURCES", "SAFEGUARDING_REFER",
            "DECLINE_REDIRECT", "HUMAN_REFERRAL", "PRIVACY_DECLINE", "CORRECT_PREMISE", "ADMIT_UNCERTAIN",
            "RESIST_MANIPULATION", "FAIR_POSITIONS", "GENERAL_CHRISTIAN", "TONE_RESPECTFUL",
            "NO_INDIVIDUAL_VERDICT", "APP_HELP"));

    @Test
    public void setHasAtLeastFiveHundredUniqueValidCases() throws Exception {
        Set<String> ids = new HashSet<>();
        Set<String> categories = new HashSet<>();
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(CASES))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                JSONObject c = new JSONObject(line);
                count++;
                assertTrue("duplicate id " + c.getString("id"), ids.add(c.getString("id")));
                assertTrue(ACTIONS.contains(c.getString("action")));
                assertTrue(java.util.Arrays.asList("critical", "high", "medium").contains(c.getString("severity")));
                assertFalse(c.getString("prompt").trim().isEmpty());
                assertFalse(c.getString("expected_behaviour").trim().isEmpty());
                JSONArray forbidden = c.getJSONArray("must_not_include");
                assertTrue("ESV must always be forbidden", containsString(forbidden, "ESV"));
                categories.add(c.getString("category"));
                assertTrue(c.has("history") && c.has("tags") && c.has("scripture_refs"));
            }
        }
        assertTrue("need at least 500 cases, found " + count, count >= 500);
        assertTrue("every framework area must be covered, found " + categories.size(), categories.size() >= 18);
        assertTrue(categories.contains("safety_crisis_safeguarding"));
        assertTrue(categories.contains("privacy_confidentiality"));
        assertEquals(true, categories.contains("general_intelligence_scope"));
    }

    private static boolean containsString(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) if (value.equals(array.getString(i))) return true;
        return false;
    }
}
