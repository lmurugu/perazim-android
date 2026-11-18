package org.perazimchurch.app.ai.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.ApiClient;

/** Sends one consented question to the authenticated church API. */
public final class AiRelayClient {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_QUESTION_CHARS = 500;

    public interface RelayCallback {
        void onSuccess(@NonNull String answer, int remaining, boolean generated);
        void onError(@NonNull String message);
    }

    private AiRelayClient() {}

    public static void askQuestion(@NonNull Context context, @NonNull String question,
                                   @NonNull RelayCallback callback) {
        Context app = context.getApplicationContext();
        if (PerazimAiEngine.isCrisisQuery(question)) {
            MAIN.post(() -> callback.onError(PerazimAiEngine.crisisResponse().getMessageText()));
            return;
        }
        if (PerazimAiEngine.isSensitivePastoralDisclosure(question)) {
            MAIN.post(() -> callback.onError(PerazimAiEngine.sensitivePastoralResponse().getMessageText()));
            return;
        }
        if (!BuildConfig.ONLINE_AI_ENABLED || !AiPreferences.hasConsented(app)) {
            MAIN.post(() -> callback.onError("Online AI requires an enabled build and your consent."));
            return;
        }
        if (question.trim().isEmpty() || question.length() > MAX_QUESTION_CHARS) {
            MAIN.post(() -> callback.onError("Enter a question of 1 to 500 characters."));
            return;
        }
        SessionManager session = SessionManager.getInstance(app);
        String token = session.getAuthToken();
        if (token == null) {
            MAIN.post(() -> callback.onError("Sign in to use online AI."));
            return;
        }
        final JSONObject body = new JSONObject();
        try {
            body.put("question", question);
        } catch (JSONException e) {
            MAIN.post(() -> callback.onError("The question could not be prepared."));
            return;
        }
        ApiClient.post(app, "/api/ai/online", body, token, new ApiClient.ApiCallback() {
            @Override public void onSuccess(int status, @NonNull JSONObject json) {
                if (!token.equals(session.getAuthToken()) || !AiPreferences.hasConsented(app)) {
                    callback.onError("Your session or consent changed. Try again.");
                    return;
                }
                Object answer = json.opt("answer");
                if (!(answer instanceof String) || ((String) answer).trim().isEmpty()) {
                    callback.onError("The church service returned no answer.");
                    return;
                }
                StringBuilder text = new StringBuilder((String) answer);
                appendCitations(text, json.optJSONArray("sourceReferences"), 6, "title", "version", "location");
                appendCitations(text, json.optJSONArray("scriptureReferences"), 3, "reference", "translationId", "corpusVersion");
                boolean generated = "SUFFICIENT".equals(json.optString("sourceSufficiency"));
                int remaining = json.isNull("remaining") ? -1 : json.optInt("remaining", -1);
                callback.onSuccess(text.toString(), remaining, generated);
            }

            @Override public void onError(int status, @NonNull String ignored) {
                String message;
                switch (status) {
                    case 401: message = "Sign in again to use online AI."; break;
                    case 403: message = "Online AI access is unavailable for this account."; break;
                    case 422: message = "Remove personal or sensitive information before trying again."; break;
                    case 429: message = "The online service limit has been reached. Try again later."; break;
                    default: message = "The online church service is unavailable. You can switch to offline app help.";
                }
                callback.onError(message);
            }
        });
    }

    private static void appendCitations(StringBuilder text, JSONArray references, int limit,
                                        String titleKey, String versionKey, String locationKey) {
        if (references == null) return;
        for (int i = 0; i < Math.min(references.length(), limit); i++) {
            JSONObject citation = references.optJSONObject(i);
            if (citation == null) continue;
            String title = citation.optString(titleKey, "").trim();
            String version = citation.optString(versionKey, "").trim();
            String location = citation.optString(locationKey, "").trim();
            if (!title.isEmpty() && !version.isEmpty() && !location.isEmpty()) {
                text.append("\n\n").append(title).append(" · ").append(version).append(" · ").append(location);
            }
        }
    }
}
