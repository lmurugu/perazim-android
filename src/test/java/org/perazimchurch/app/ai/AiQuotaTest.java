package org.perazimchurch.app.ai;

import org.junit.Test;
import org.perazimchurch.app.ai.remote.AiPreferences;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying daily AI question quota tracking, decrements, and midnight resets.
 */
public class AiQuotaTest {

    private static final int DEFAULT_LIMIT = 3;

    @Test
    public void testFullQuotaOnInitialInstallOrNewDay() {
        // When user has never asked a question
        int remaining = AiPreferences.calculateRemaining(DEFAULT_LIMIT, "2026-11-03", "", 0);
        assertEquals(3, remaining);

        // When date rolled over from yesterday
        remaining = AiPreferences.calculateRemaining(DEFAULT_LIMIT, "2026-11-03", "2026-11-02", 3);
        assertEquals(3, remaining);
    }

    @Test
    public void testQuotaDecrementSameDay() {
        String today = "2026-11-03";

        // After 1 question used today
        int remaining1 = AiPreferences.calculateRemaining(DEFAULT_LIMIT, today, today, 1);
        assertEquals(2, remaining1);

        // After 2 questions used today
        int remaining2 = AiPreferences.calculateRemaining(DEFAULT_LIMIT, today, today, 2);
        assertEquals(1, remaining2);
    }

    @Test
    public void testQuotaExhaustionFloorAtZero() {
        String today = "2026-11-03";

        // After 3 questions used today (quota reached)
        int remaining3 = AiPreferences.calculateRemaining(DEFAULT_LIMIT, today, today, 3);
        assertEquals(0, remaining3);

        // If count exceeds limit, remaining never goes negative
        int remainingOver = AiPreferences.calculateRemaining(DEFAULT_LIMIT, today, today, 5);
        assertEquals(0, remainingOver);
    }

    @Test
    public void testMidnightRolloverResetsQuota() {
        // Day 1: User exhausted all 3 questions
        String day1 = "2026-11-03";
        int remainingDay1 = AiPreferences.calculateRemaining(DEFAULT_LIMIT, day1, day1, 3);
        assertEquals(0, remainingDay1);

        // Day 2 (Midnight crosses): date changes to 2026-11-04
        String day2 = "2026-11-04";
        int remainingDay2 = AiPreferences.calculateRemaining(DEFAULT_LIMIT, day2, day1, 3);
        assertEquals(3, remainingDay2);
    }

    @Test
    public void testCustomRemoteDailyLimit() {
        String today = "2026-11-03";
        int customLimit = 5;

        assertEquals(5, AiPreferences.calculateRemaining(customLimit, today, "", 0));
        assertEquals(4, AiPreferences.calculateRemaining(customLimit, today, today, 1));
        assertEquals(1, AiPreferences.calculateRemaining(customLimit, today, today, 4));
        assertEquals(0, AiPreferences.calculateRemaining(customLimit, today, today, 5));
    }

    @Test
    public void testNairobiDateStringFormat() {
        String nairobiDate = AiPreferences.getCurrentNairobiDate();
        Pattern pattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
        assertTrue("Nairobi date must match YYYY-MM-DD: " + nairobiDate,
                pattern.matcher(nairobiDate).matches());
    }
}
