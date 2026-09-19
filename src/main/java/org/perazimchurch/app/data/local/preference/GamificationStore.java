package org.perazimchurch.app.data.local.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * Persistent SharedPreferences-backed storage for user gamification metrics:
 * Devotional Streaks, Spiritual XP, and Grace Points.
 * Ensures metrics survive process death, rotation, and cold restarts.
 */
public final class GamificationStore {

    private static final String PREF_NAME = "perazim_gamification";

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
     * Saves the devotional streak count and frozen status for a given user.
     */
    public static void saveStreak(Context context, String userId, int streak, boolean frozen) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt("streak_" + uid, streak)
                .putBoolean("frozen_" + uid, frozen)
                .commit();
    }

    /**
     * Retrieves the persisted devotional streak for a given user.
     */
    public static int getStreak(Context context, String userId, int defaultStreak) {
        if (context == null) return defaultStreak;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt("streak_" + uid, defaultStreak);
    }

    /**
     * Retrieves whether the devotional streak is currently frozen/protected for a given user.
     */
    public static boolean isStreakFrozen(Context context, String userId, boolean defaultFrozen) {
        if (context == null) return defaultFrozen;
        String uid = resolveUserId(userId);
        return getPrefs(context).getBoolean("frozen_" + uid, defaultFrozen);
    }

    /**
     * Saves the total spiritual XP earned by a given user.
     */
    public static void saveSpiritualXp(Context context, String userId, int xp) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt("xp_" + uid, xp)
                .commit();
    }

    /**
     * Retrieves the total spiritual XP for a given user.
     */
    public static int getSpiritualXp(Context context, String userId, int defaultXp) {
        if (context == null) return defaultXp;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt("xp_" + uid, defaultXp);
    }

    /**
     * Saves the current Grace Points balance for a given user.
     */
    public static void saveGracePoints(Context context, String userId, int points) {
        if (context == null) return;
        String uid = resolveUserId(userId);
        getPrefs(context).edit()
                .putInt("grace_" + uid, points)
                .commit();
    }

    /**
     * Retrieves the current Grace Points balance for a given user.
     */
    public static int getGracePoints(Context context, String userId, int defaultPoints) {
        if (context == null) return defaultPoints;
        String uid = resolveUserId(userId);
        return getPrefs(context).getInt("grace_" + uid, defaultPoints);
    }
}
