package org.perazimchurch.app.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public final class PrivacyNoticeRemoteSource {
    public interface Callback {
        void onLoaded(@NonNull JSONObject notice);
        void onError(@NonNull String message);
    }

    public interface PresentationCallback {
        void onReady(@NonNull JSONObject presentation);
        void onError(int statusCode, @NonNull String message);
    }

    private PrivacyNoticeRemoteSource() {}

    public static void fetchCurrent(@NonNull Context context, @NonNull Callback callback) {
        ApiClient.get(context, "/api/privacy/notice/current", null, new ApiClient.ApiCallback() {
            @Override public void onSuccess(int statusCode, @NonNull JSONObject json) {
                if (json.optString("version", "").isEmpty() || json.optString("sha256", "").isEmpty() || json.optString("content", "").isEmpty()) {
                    callback.onError("The current privacy notice is incomplete. Please try again later.");
                    return;
                }
                callback.onLoaded(json);
            }

            @Override public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError("The current privacy notice could not be loaded. Registration is unavailable until it can be reviewed.");
            }
        });
    }

    public static void fetchPresentation(@NonNull Context context, @NonNull String authToken, @NonNull PresentationCallback callback) {
        ApiClient.post(context, "/api/privacy/presentation", new JSONObject(), authToken, new ApiClient.ApiCallback() {
            @Override public void onSuccess(int statusCode, @NonNull JSONObject json) { callback.onReady(json); }
            @Override public void onError(int statusCode, @NonNull String errorMessage) { callback.onError(statusCode, errorMessage); }
        });
    }

    public static void acknowledge(@NonNull Context context, @NonNull String authToken,
                                   @NonNull String receipt, @NonNull PresentationCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("receipt", receipt);
            ApiClient.post(context, "/api/privacy/acknowledge", body, authToken, new ApiClient.ApiCallback() {
                @Override public void onSuccess(int statusCode, @NonNull JSONObject json) { callback.onReady(json); }
                @Override public void onError(int statusCode, @NonNull String errorMessage) { callback.onError(statusCode, errorMessage); }
            });
        } catch (Exception e) {
            callback.onError(-1, "The privacy acknowledgement could not be prepared.");
        }
    }
}
