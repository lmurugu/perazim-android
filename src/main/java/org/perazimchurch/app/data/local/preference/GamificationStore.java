package org.perazimchurch.app.data.local.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Persistent SharedPreferences-backed storage for user gamification metrics:
 * Devotional Streaks, Spiritual XP, Grace Points, Routine Completion, Lifetime Days,
 * and Milestone Badges.
 * Ensures metrics survive process death, rotation, and cold restarts.
 */
public final class GamificationStore {

    private static final String PREF_NAME = "perazim_gamification";

    public static final String KEY_PREFIX_STREAK = "streak_";
    public static final String KEY_PREFIX_FROZEN = "frozen_";
    public static final String KEY_PREFIX_XP = "xp_";
    public static final String KEY_PREFIX_GRACE = "grace_";
    public static final String KEY_PREFIX_LAST_ROUTINE_DATE = "last_routine_date_";
    public static final String KEY_PREFIX_LIFETIME_ROUTINE_DAYS = "lifetime_routine_days_";
    public static final String KEY_PREFIX_BADGE_FIRST_ROUTINE = "badge_first_routine_";
    public static final String KEY_PREFIX_BADGE_STREAK_7 = "badge_streak_7_";

    public static final String TIMEZONE_NAIROBI = "Africa/Nairobi";

    private GamificationStore() {
        // Utility class
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String resolveUserId(@Nullable String userId) {
        return (userId == null || userId.trim().isEmpty()) ? "user_me" : userId;
    }

    /**
     * Returns today's date formatted as yyyy-MM-dd in the Africa/Nairobi timezone.
     */
    public static String getTodayDateNairobi() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone(TIMEZONE_NAIROBI));
        return sdf.format(new Date());
    }

    /**
     * Saves the devotional streak count and frozen status for a given user.
     */
    public static void saveStreak(Context context, String userId, int streak, boolean frozen) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt(KEY_PREFIX_STREAK + uid, streak)
                .putBoolean(KEY_PREFIX_FROZEN + uid, frozen)
                .commit();
    }

    /**
     * Retrieves the persisted devotional streak for a given user.
     */
    public static int getStreak(Context context, String userId, int defaultStreak) {
        if (context == null) return defaultStreak;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt(KEY_PREFIX_STREAK + uid, defaultStreak);
    }

    /**
     * Retrieves whether the devotional streak is currently frozen/protected for a given user.
     */
    public static boolean isStreakFrozen(Context context, String userId, boolean defaultFrozen) {
        if (context == null) return defaultFrozen;
        String uid = resolveUserId(userId);
        return getPrefs(context).getBoolean(KEY_PREFIX_FROZEN + uid, defaultFrozen);
    }

    /**
     * Saves the total spiritual XP earned by a given user.
     */
    public static void saveSpiritualXp(Context context, String userId, int xp) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt(KEY_PREFIX_XP + uid, xp)
                .commit();
    }

    /**
     * Retrieves the total spiritual XP for a given user.
     */
    public static int getSpiritualXp(Context context, String userId, int defaultXp) {
        if (context == null) return defaultXp;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt(KEY_PREFIX_XP + uid, defaultXp);
    }

    /**
     * Saves the current Grace Points balance for a given user.
     */
    public static void saveGracePoints(Context context, String userId, int points) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt(KEY_PREFIX_GRACE + uid, points)
                .commit();
    }

    /**
     * Retrieves the current Grace Points balance for a given user.
     */
    public static int getGracePoints(Context context, String userId, int defaultPoints) {
        if (context == null) return defaultPoints;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt(KEY_PREFIX_GRACE + uid, defaultPoints);
    }

    /**
     * Saves the last calendar date on which the daily routine was completed (yyyy-MM-dd).
     */
    public static void saveLastRoutineDate(Context context, String userId, String date) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putString(KEY_PREFIX_LAST_ROUTINE_DATE + uid, date)
                .commit();
    }

    /**
     * Retrieves the last routine completion date (yyyy-MM-dd) or defaultDate if not recorded.
     */
    public static String getLastRoutineDate(Context context, String userId, @Nullable String defaultDate) {
        if (context == null) return defaultDate;
        String uid = resolveUserId(userId);
        return getPrefs(context).getString(KEY_PREFIX_LAST_ROUTINE_DATE + uid, defaultDate);
    }

    public static String getLastRoutineDate(Context context, String userId) {
        return getLastRoutineDate(context, userId, null);
    }

    /**
     * Checks if the user has completed the routine today according to Africa/Nairobi time.
     */
    public static boolean isRoutineCompletedToday(Context context, String userId) {
        String today = getTodayDateNairobi();
        String last = getLastRoutineDate(context, userId, null);
        return today.equals(last);
    }

    /**
     * Checks if the user completed the routine on a specific date (yyyy-MM-dd).
     */
    public static boolean isRoutineCompletedForDate(Context context, String userId, String dateString) {
        if (context == null || dateString == null) return false;
        String last = getLastRoutineDate(context, userId, null);
        return dateString.equals(last);
    }

    /**
     * Saves the total lifetime routine days completed.
     */
    public static void saveLifetimeRoutineDays(Context context, String userId, int days) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt(KEY_PREFIX_LIFETIME_ROUTINE_DAYS + uid, days)
                .commit();
    }

    /**
     * Retrieves the total lifetime routine days completed.
     */
    public static int getLifetimeRoutineDays(Context context, String userId, int defaultDays) {
        if (context == null) return defaultDays;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt(KEY_PREFIX_LIFETIME_ROUTINE_DAYS + uid, defaultDays);
    }

    public static int getLifetimeRoutineDays(Context context, String userId) {
        return getLifetimeRoutineDays(context, userId, 0);
    }

    /**
     * Sets whether the user has earned the First Routine badge (one-shot).
     */
    public static void setBadgeFirstRoutine(Context context, String userId, boolean earned) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putBoolean(KEY_PREFIX_BADGE_FIRST_ROUTINE + uid, earned)
                .commit();
    }

    /**
     * Checks if the user has earned the First Routine badge.
     */
    public static boolean hasBadgeFirstRoutine(Context context, String userId) {
        if (context == null) return false;
        String uid = resolveUserId(userId);
        return getPrefs(context).getBoolean(KEY_PREFIX_BADGE_FIRST_ROUTINE + uid, false);
    }

    /**
     * Sets whether the user has earned the 7-Day Streak badge (one-shot).
     */
    public static void setBadgeStreak7(Context context, String userId, boolean earned) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putBoolean(KEY_PREFIX_BADGE_STREAK_7 + uid, earned)
                .commit();
    }

    /**
     * Checks if the user has earned the 7-Day Streak badge.
     */
    public static boolean hasBadgeStreak7(Context context, String userId) {
        if (context == null) return false;
        String uid = resolveUserId(userId);
        return getPrefs(context).getBoolean(KEY_PREFIX_BADGE_STREAK_7 + uid, false);
    }

    /**
     * Records completion of the daily routine for today (Africa/Nairobi).
     * - Sets last_routine_date = today
     * - If last_routine_date was not already today, increments lifetime_routine_days once
     * - Evaluates badges (first routine, 7-day streak) and sets booleans if not already set
     * - Does NOT grant extra XP for badges.
     * - Does NOT double-award lifetime days if tapped multiple times on the same day.
     *
     * @param context Active context
     * @param userId User identifier
     * @param currentStreak Current streak to evaluate for 7-day badge
     * @return true if this was a new completion for today, false if already completed today
     */
    public static boolean recordDailyRoutineCompleted(Context context, String userId, int currentStreak) {
        return recordDailyRoutineCompletedForDate(context, userId, getTodayDateNairobi(), currentStreak);
    }

    public static boolean recordDailyRoutineCompleted(Context context, String userId) {
        return recordDailyRoutineCompleted(context, userId, getStreak(context, userId, 0));
    }

    /**
     * Core date-parameterized implementation allowing deterministic testing across date boundaries.
     */
    public static boolean recordDailyRoutineCompletedForDate(Context context, String userId, String dateString, int currentStreak) {
        if (context == null || dateString == null || dateString.trim().isEmpty()) return false;
        String uid = resolveUserId(userId);
        String lastDate = getLastRoutineDate(context, uid, null);
        boolean isNewDay = !dateString.equals(lastDate);

        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_PREFIX_LAST_ROUTINE_DATE + uid, dateString);

        if (isNewDay) {
            int currentLifetime = getLifetimeRoutineDays(context, uid, 0);
            editor.putInt(KEY_PREFIX_LIFETIME_ROUTINE_DAYS + uid, currentLifetime + 1);
        }

        if (!hasBadgeFirstRoutine(context, uid)) {
            editor.putBoolean(KEY_PREFIX_BADGE_FIRST_ROUTINE + uid, true);
        }

        if (currentStreak >= 7 && !hasBadgeStreak7(context, uid)) {
            editor.putBoolean(KEY_PREFIX_BADGE_STREAK_7 + uid, true);
        }

        editor.commit();
        return isNewDay;
    }
}
