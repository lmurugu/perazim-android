package org.perazimchurch.app.ai.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.AppConfigEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Manages device-level AI consent, device identification, and daily client-side quota tracking.
 * Timezone: Africa/Nairobi (UTC+3) ensures reset aligns with church calendar and server quota.
 */
public final class AiPreferences {

    private static final String PREF_NAME = "perazim_ai_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_CONSENT_ACCEPTED = "ai_question_only_consent_v1";
    private static final String KEY_OPT_IN_NAME = "ai_opt_in_name";
    private static final String KEY_LAST_QUESTION_DATE = "last_question_date";
    private static final String KEY_TODAY_QUESTION_COUNT = "today_question_count";

    public static final int DEFAULT_DAILY_LIMIT = 3;
    private static final TimeZone NAIROBI_TZ = TimeZone.getTimeZone("Africa/Nairobi");

    private AiPreferences() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns or creates a persistent random UUID device ID.
     */
    @NonNull
    public static synchronized String getDeviceId(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null || id.trim().isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    /**
     * Checks if user has accepted the plain-words AI consent disclosure.
     */
    public static boolean hasConsented(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_CONSENT_ACCEPTED, false);
    }

    /**
     * Sets user consent state.
     */
    public static void setConsent(@NonNull Context context, boolean accepted) {
        getPrefs(context).edit().putBoolean(KEY_CONSENT_ACCEPTED, accepted).apply();
    }

    /**
     * Checks if user has opted in to sharing their first name with the AI.
     */
    public static boolean isFirstNameOptedIn(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_OPT_IN_NAME, true);
    }

    /**
     * Toggles first name opt-in.
     */
    public static void setFirstNameOptedIn(@NonNull Context context, boolean optedIn) {
        getPrefs(context).edit().putBoolean(KEY_OPT_IN_NAME, optedIn).apply();
    }

    /**
     * Gets current date string in Africa/Nairobi timezone (YYYY-MM-DD).
     */
    @NonNull
    public static String getCurrentNairobiDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(NAIROBI_TZ);
        return sdf.format(new Date());
    }

    /**
     * Returns how many AI questions the user has remaining today.
     */
    public static int getRemainingQuestions(@NonNull Context context) {
        int limit = getDailyLimit(context);
        SharedPreferences prefs = getPrefs(context);
        String today = getCurrentNairobiDate();
        String lastDate = prefs.getString(KEY_LAST_QUESTION_DATE, "");
        int used = prefs.getInt(KEY_TODAY_QUESTION_COUNT, 0);

        return calculateRemaining(limit, today, lastDate, used);
    }

    /**
     * Pure calculation helper for testing daily quota decrement and midnight reset logic.
     */
    public static int calculateRemaining(int dailyLimit, String todayDate, String lastRecordedDate, int usedCount) {
        if (todayDate == null || !todayDate.equals(lastRecordedDate)) {
            return dailyLimit;
        }
        return Math.max(0, dailyLimit - usedCount);
    }

    /**
     * Increments the count of questions used today.
     */
    public static synchronized void recordQuestionUsed(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        String today = getCurrentNairobiDate();
        String lastDate = prefs.getString(KEY_LAST_QUESTION_DATE, "");

        int currentCount = 0;
        if (today.equals(lastDate)) {
            currentCount = prefs.getInt(KEY_TODAY_QUESTION_COUNT, 0);
        }

        prefs.edit()
                .putString(KEY_LAST_QUESTION_DATE, today)
                .putInt(KEY_TODAY_QUESTION_COUNT, currentCount + 1)
                .apply();
    }

    /**
     * Reads configured daily limit from AppConfig in Room (key: ai_daily_limit), default 3.
     */
    public static int getDailyLimit(@NonNull Context context) {
        try {
            PerazimDatabase db = PerazimDatabase.getInstance(context);
            String val = db.appConfigDao().getConfigValue("ai_daily_limit");
            if (val != null) {
                int parsed = Integer.parseInt(val.trim());
                if (parsed > 0) return parsed;
            }
        } catch (Throwable ignored) {
        }
        return DEFAULT_DAILY_LIMIT;
    }

    /**
     * Checks if AI is enabled via remote kill-switch in AppConfig (key: ai_enabled), default true.
     */
    public static boolean isAiEnabled(@NonNull Context context) {
        try {
            PerazimDatabase db = PerazimDatabase.getInstance(context);
            String val = db.appConfigDao().getConfigValue("ai_enabled");
            if (val != null) {
                String clean = val.trim().toLowerCase(Locale.US);
                return !clean.equals("false") && !clean.equals("0") && !clean.equals("no") && !clean.equals("off");
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}
