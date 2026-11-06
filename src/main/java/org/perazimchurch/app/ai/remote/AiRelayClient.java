package org.perazimchurch.app.ai.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.session.SessionManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous HTTP client communicating with the Cloudflare Worker AI Relay.
 * Handles background context gathering, timeouts (15s), device ID authentication,
 * daily quota checking, and fail-soft fallback signalling.
 */
public final class AiRelayClient {

    private static final String TAG = "AiRelayClient";
    private static final int TIMEOUT_MS = 30000;
    private static final String USER_AGENT = "Perazim-Android/2.2.0";

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface RelayCallback {
        /**
         * Called when the Cloudflare Worker + Gemini returns a successful answer.
         */
        void onSuccess(@NonNull String answer, int remainingQuestionsToday);

        /**
         * Called when the daily quota of questions has been exhausted (HTTP 429 or client limit).
         */
        void onQuotaExceeded(int remainingQuestionsToday);

        /**
         * Called on any error (network failure, timeout, 5xx, or remote killswitch).
         * @param shouldFallbackOffline true if caller should seamlessly fallback to on-device PerazimAiEngine.
         */
        void onError(@NonNull String errorMessage, boolean shouldFallbackOffline);
    }

    private AiRelayClient() {}

    /**
     * Dispatches a user question to the AI relay asynchronously.
     */
    public static void askQuestion(@NonNull final Context context,
                                   @NonNull final String rawQuestion,
                                   @NonNull final RelayCallback callback) {
        askQuestion(context, rawQuestion, Collections.emptyList(), callback);
    }

    public static void askQuestion(@NonNull final Context context,
                                   @NonNull final String rawQuestion,
                                   @NonNull final List<ConversationHistory.Turn> history,
                                   @NonNull final RelayCallback callback) {
        final Context appContext = context.getApplicationContext();
        final List<ConversationHistory.Turn> snapshot = new ArrayList<>(history);

        // 1. Consent check
        if (!AiPreferences.hasConsented(appContext)) {
            postError(callback, "AI consent disclosure has not been accepted.", false);
            return;
        }

        // 2. Kill switch check
        if (!AiPreferences.isAiEnabled(appContext)) {
            Log.i(TAG, "AI feature disabled via remote AppConfig kill-switch");
            postError(callback, "AI service is currently resting.", true);
            return;
        }

        // 3. Client quota pre-check
        int remaining = AiPreferences.getRemainingQuestions(appContext);
        if (remaining <= 0) {
            Log.i(TAG, "Client daily question quota reached");
            postQuotaExceeded(callback, 0);
            return;
        }

        // 4. Background network execution
        EXECUTOR.execute(() -> executeRequest(appContext, rawQuestion, snapshot, callback));
    }

    private static void executeRequest(Context appContext, String rawQuestion, List<ConversationHistory.Turn> history, RelayCallback callback) {
        HttpURLConnection conn = null;
        try {
            // Gather contextual details
            String deviceId = AiPreferences.getDeviceId(appContext);

            String firstName = null;
            int streak = 0;
            if (AiPreferences.isFirstNameOptedIn(appContext)) {
                try {
                    UserEntity user = SessionManager.getInstance(appContext).getCurrentUser();
                    if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                        firstName = user.getName().trim().split("\\s+")[0];
                        streak = user.getStreakCount();
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Failed reading user profile for AI context", t);
                }
            }

            String todayVerse = "2 Samuel 5:20";
            try {
                PerazimDatabase db = PerazimDatabase.getInstance(appContext);
                String todayDate = AiPreferences.getCurrentNairobiDate();
                DailyVerseEntity dve = db.dailyVerseDao().getVerseForDate(todayDate);
                if (dve != null && dve.getReference() != null && !dve.getReference().trim().isEmpty()) {
                    todayVerse = dve.getReference().trim();
                    if (dve.getText() != null && !dve.getText().trim().isEmpty()) {
                        todayVerse += " — " + dve.getText().trim();
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed reading today's verse for AI context", t);
            }

            List<String> passages = BibleContextExtractor.extractRelevantPassages(appContext, rawQuestion);

            // Build JSON payload
            JSONObject root = new JSONObject();
            root.put("deviceId", deviceId);
            root.put("question", ConversationHistory.legacyQuestion(rawQuestion.trim(), history));
            root.put("currentQuestion", rawQuestion.trim());
            JSONArray historyJson = new JSONArray();
            int from = Math.max(0, history.size() - ConversationHistory.MAX_TURNS);
            for (int i = from; i < history.size(); i++) {
                ConversationHistory.Turn turn = history.get(i);
                JSONObject message = new JSONObject();
                message.put("role", turn.role);
                message.put("text", turn.text);
                historyJson.put(message);
            }
            root.put("history", historyJson);
            if (firstName != null && !firstName.isEmpty()) {
                root.put("firstName", firstName);
            }

            JSONObject ctxObj = new JSONObject();
            ctxObj.put("streak", streak);
            ctxObj.put("verse", todayVerse);
            JSONArray passagesArr = new JSONArray();
            for (String p : passages) {
                passagesArr.put(p);
            }
            ctxObj.put("passages", passagesArr);
            root.put("context", ctxObj);

            String jsonPayload = root.toString();

            // Connect to Cloudflare Worker Relay
            String endpointUrl = BuildConfig.AI_RELAY_URL;
            if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
                endpointUrl = "https://perazim-ai-relay.perazim-mission.workers.dev/chat";
            }
            URL url = new URL(endpointUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);

            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", USER_AGENT);

            String appToken = BuildConfig.AI_APP_TOKEN;
            if (appToken != null && !appToken.trim().isEmpty()) {
                conn.setRequestProperty("X-App-Token", appToken.trim());
            }

            // Write body
            byte[] bytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int statusCode = conn.getResponseCode();
            Log.d(TAG, "Relay response HTTP code: " + statusCode);

            if (statusCode == HttpURLConnection.HTTP_OK) {
                String responseBody = readStream(conn.getInputStream());
                JSONObject respJson = new JSONObject(responseBody);
                String answer = respJson.optString("answer", "").trim();
                int remaining = respJson.optInt("remaining", 0);

                if (answer.isEmpty()) {
                    postError(callback, "The AI returned an empty answer. Please try again.", true);
                    return;
                }
                AiPreferences.recordQuestionUsed(appContext);
                postSuccess(callback, answer, remaining);
            } else if (statusCode == 429) {
                // Rate limit reached on server
                String responseBody = readStream(conn.getErrorStream());
                Log.w(TAG, "Relay 429 rate limit reached: " + responseBody);
                AiPreferences.recordQuestionUsed(appContext);
                postQuotaExceeded(callback, 0);
            } else {
                String errBody = readStream(conn.getErrorStream());
                Log.e(TAG, "Relay returned error " + statusCode + ": " + errBody);
                postError(callback, "Server returned " + statusCode, true);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Relay network exception: " + t.getMessage(), t);
            postError(callback, "Network or timeout error: " + t.getMessage(), true);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void postSuccess(RelayCallback callback, String answer, int remaining) {
        MAIN_HANDLER.post(() -> callback.onSuccess(answer, remaining));
    }

    private static void postQuotaExceeded(RelayCallback callback, int remaining) {
        MAIN_HANDLER.post(() -> callback.onQuotaExceeded(remaining));
    }

    private static void postError(RelayCallback callback, String msg, boolean fallback) {
        MAIN_HANDLER.post(() -> callback.onError(msg, fallback));
    }
}
