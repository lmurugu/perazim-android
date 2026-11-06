package org.perazimchurch.app.gamification;

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.presentation.ui.GivingDialog;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test suite verifying GamificationStore completeness:
 * 1. Date boundaries (today vs yesterday in Africa/Nairobi timezone).
 * 2. Lifetime routine days increment exactly once per calendar day.
 * 3. One-shot milestone badges (badge_first_routine, badge_streak_7) awarded once without extra XP.
 * 4. Giving path (GivingDialog) does not award or modify Spiritual XP.
 */
public class GamificationStoreTest {

    private FakeSharedPreferences fakePrefs;
    private static final String USER_ID = "test_user_perazim";

    @Before
    public void setUp() {
        fakePrefs = new FakeSharedPreferences();
        GamificationStore.setPreferencesForTesting(fakePrefs);
    }

    @After
    public void tearDown() {
        GamificationStore.setPreferencesForTesting(null);
    }

    @Test
    public void newMemberStartsWithZeroBalances() {
        org.perazimchurch.app.data.local.entity.UserEntity member =
                new org.perazimchurch.app.data.local.entity.UserEntity(
                        "new-member", "Member", "member@example.test", "", "MEMBER",
                        "", "", 0L, 0L, true);
        assertEquals(0, member.getStreakCount());
        assertEquals(0, member.getSpiritualXp());
        assertEquals(0, member.getGracePoints());
    }

    // =========================================================================
    // 1. DATE BOUNDARY & TIMEZONE (AFRICA/NAIROBI)
    // =========================================================================

    @Test
    public void testAfricaNairobiTimezoneFormatting() {
        // Africa/Nairobi is UTC+3 with no daylight saving time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            // 2026-11-03 22:30:00 UTC is 2026-11-04 01:30:00 in Africa/Nairobi (next day)
            Date lateNightUtc = sdf.parse("2026-11-03 22:30:00");

            SimpleDateFormat nairobiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            nairobiFormat.setTimeZone(TimeZone.getTimeZone(GamificationStore.TIMEZONE_NAIROBI));

            String nairobiDate = nairobiFormat.format(lateNightUtc);
            assertEquals("2026-11-04", nairobiDate);

            // 2026-11-03 20:00:00 UTC is 2026-11-03 23:00:00 in Africa/Nairobi (same day)
            Date eveningUtc = sdf.parse("2026-11-03 20:00:00");
            assertEquals("2026-11-03", nairobiFormat.format(eveningUtc));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDoneTodayVsYesterdayDateBoundary() {
        String yesterday = "2026-11-02";
        String today = "2026-11-03";

        // Step 1: User completed routine yesterday
        GamificationStore.saveLastRoutineDate(null, USER_ID, yesterday);
        assertEquals(yesterday, GamificationStore.getLastRoutineDate(null, USER_ID));

        // Step 2: Querying today's completion must return false
        assertFalse(GamificationStore.isRoutineCompletedForDate(null, USER_ID, today));

        // Step 3: Complete today's routine
        boolean isNewDay = GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, today, 1);
        assertTrue("First completion on today must report new day", isNewDay);

        // Step 4: Querying today's completion now returns true
        assertTrue(GamificationStore.isRoutineCompletedForDate(null, USER_ID, today));
        assertEquals(today, GamificationStore.getLastRoutineDate(null, USER_ID));
    }

    // =========================================================================
    // 2. LIFETIME ROUTINE DAYS (INCREMENT ONCE PER CALENDAR DAY)
    // =========================================================================

    @Test
    public void testLifetimeIncrementOncePerDay() {
        assertEquals(0, GamificationStore.getLifetimeRoutineDays(null, USER_ID));

        String day1 = "2026-11-01";
        String day2 = "2026-11-02";

        // Day 1: First completion
        boolean day1First = GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, day1, 1);
        assertTrue(day1First);
        assertEquals(1, GamificationStore.getLifetimeRoutineDays(null, USER_ID));

        // Day 1: User re-runs routine on the same day (should NOT double increment)
        boolean day1Second = GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, day1, 1);
        assertFalse("Duplicate completion on same day must not report new day", day1Second);
        assertEquals("Lifetime days must remain 1 after second tap today",
                1, GamificationStore.getLifetimeRoutineDays(null, USER_ID));

        // Day 1: Third tap on same day
        boolean day1Third = GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, day1, 1);
        assertFalse(day1Third);
        assertEquals(1, GamificationStore.getLifetimeRoutineDays(null, USER_ID));

        // Day 2: First completion on new calendar day
        boolean day2First = GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, day2, 2);
        assertTrue(day2First);
        assertEquals("Lifetime days must increment to 2 on next calendar day",
                2, GamificationStore.getLifetimeRoutineDays(null, USER_ID));

        // Day 2: Re-run on Day 2
        boolean day2Second = GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, day2, 2);
        assertFalse(day2Second);
        assertEquals(2, GamificationStore.getLifetimeRoutineDays(null, USER_ID));
    }

    // =========================================================================
    // 3. BADGES SET ONCE & NO EXTRA XP AWARDED
    // =========================================================================

    @Test
    public void testBadgesSetOnceAndNoExtraXp() {
        // Initial state: no badges earned
        assertFalse(GamificationStore.hasBadgeFirstRoutine(null, USER_ID));
        assertFalse(GamificationStore.hasBadgeStreak7(null, USER_ID));

        // Set baseline XP
        GamificationStore.saveSpiritualXp(null, USER_ID, 100);
        assertEquals(100, GamificationStore.getSpiritualXp(null, USER_ID, 0));

        // Complete Day 1 routine (streak = 1)
        GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, "2026-11-01", 1);

        // badge_first_routine must be awarded
        assertTrue("First routine badge must be earned", GamificationStore.hasBadgeFirstRoutine(null, USER_ID));
        assertFalse("Streak 7 badge must not be earned yet", GamificationStore.hasBadgeStreak7(null, USER_ID));

        // Badges MUST NOT award extra XP directly
        assertEquals("XP must remain unchanged by badge acquisition", 100, GamificationStore.getSpiritualXp(null, USER_ID, 0));

        // Advance to streak 6: badge_streak_7 still false
        GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, "2026-11-06", 6);
        assertFalse("Streak 7 badge must still be false at streak 6", GamificationStore.hasBadgeStreak7(null, USER_ID));
        assertEquals(100, GamificationStore.getSpiritualXp(null, USER_ID, 0));

        // Reach streak 7: badge_streak_7 awarded
        GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, "2026-11-07", 7);
        assertTrue("Streak 7 badge must now be earned", GamificationStore.hasBadgeStreak7(null, USER_ID));
        assertTrue("First routine badge remains earned", GamificationStore.hasBadgeFirstRoutine(null, USER_ID));
        assertEquals("XP must still be 100 (badges do not give XP)", 100, GamificationStore.getSpiritualXp(null, USER_ID, 0));

        // Day 8 (streak = 8): Badges stay earned, one-shot booleans remain stable
        GamificationStore.recordDailyRoutineCompletedForDate(null, USER_ID, "2026-11-08", 8);
        assertTrue(GamificationStore.hasBadgeFirstRoutine(null, USER_ID));
        assertTrue(GamificationStore.hasBadgeStreak7(null, USER_ID));
        assertEquals(100, GamificationStore.getSpiritualXp(null, USER_ID, 0));
    }

    // =========================================================================
    // 4. GIVING PATH DOES NOT CHANGE XP
    // =========================================================================

    @Test
    public void testGivingPathDoesNotChangeXp() throws Exception {
        // Verify user starts with 250 XP
        GamificationStore.saveSpiritualXp(null, USER_ID, 250);
        assertEquals(250, GamificationStore.getSpiritualXp(null, USER_ID, 0));

        // Static code inspection verification:
        // GivingDialog.java must not import GamificationStore or award Spiritual XP
        File sourceFile = new File("src/main/java/org/perazimchurch/app/presentation/ui/GivingDialog.java");
        assertTrue("GivingDialog.java must exist", sourceFile.exists());
        String code = new String(Files.readAllBytes(sourceFile.toPath()));

        assertFalse("GivingDialog must not import GamificationStore",
                code.contains("import org.perazimchurch.app.data.local.preference.GamificationStore;"));
        assertFalse("GivingDialog must not award +20 XP",
                code.contains("+20 XP"));
        assertFalse("GivingDialog must not call saveSpiritualXp",
                code.contains("saveSpiritualXp"));
        assertFalse("GivingDialog must not reference Spiritual XP",
                code.toLowerCase(Locale.ROOT).contains("spiritual xp") || code.contains("SpiritualXp"));

        // User XP remains exactly 250
        assertEquals(250, GamificationStore.getSpiritualXp(null, USER_ID, 0));
    }

    // =========================================================================
    // IN-MEMORY FAKE SHAREDPREFERENCES IMPLEMENTATION FOR UNIT TESTS
    // =========================================================================

    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = values.get(key);
            return (v instanceof String) ? (String) v : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = values.get(key);
            return (v instanceof Set) ? (Set<String>) v : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = values.get(key);
            return (v instanceof Integer) ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = values.get(key);
            return (v instanceof Long) ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = values.get(key);
            return (v instanceof Float) ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = values.get(key);
            return (v instanceof Boolean) ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor(this);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }

    private static class FakeEditor implements SharedPreferences.Editor {
        private final FakeSharedPreferences prefs;
        private final Map<String, Object> temp = new HashMap<>();
        private boolean clear = false;

        FakeEditor(FakeSharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            temp.put(key, values);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            temp.put(key, this); // sentinel
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            clear = true;
            return this;
        }

        @Override
        public boolean commit() {
            if (clear) {
                prefs.values.clear();
            }
            for (Map.Entry<String, Object> entry : temp.entrySet()) {
                if (entry.getValue() == this) {
                    prefs.values.remove(entry.getKey());
                } else {
                    prefs.values.put(entry.getKey(), entry.getValue());
                }
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
