package org.perazimchurch.app.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/** Changes the member's PIN on the server. The server returns a new token for this device. */
public final class PinRemoteSource {
    public interface Callback {
        void onChanged(@NonNull String newToken);
        void onError(@NonNull String message);
    }

    private PinRemoteSource() {}

    public static void changePin(@NonNull Context context, @NonNull String authToken,
                                 @NonNull String currentPin, @NonNull String newPin, @NonNull Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("current_pin", currentPin);
            body.put("new_pin", newPin);
            ApiClient.post(context, "/api/auth/change-pin", body, authToken, new ApiClient.ApiCallback() {
                @Override public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    String token = json.optString("token", "");
                    if (token.isEmpty()) {
                        callback.onError("The PIN could not be changed. Please try again.");
                    } else {
                        org.perazimchurch.app.data.local.session.SessionManager.getInstance(context)
                                .setPendingRefreshToken(json.optString("refresh_token", ""));
                        callback.onChanged(token);
                    }
                }
                @Override public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(errorMessage != null && !errorMessage.isEmpty()
                            ? errorMessage : "The PIN could not be changed. Please try again.");
                }
            });
        } catch (Exception e) {
            callback.onError("The PIN could not be changed. Please try again.");
        }
    }
}
