package org.perazimchurch.app.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.UUID;

/** Sends data-subject requests (erasure, access, correction) to the server. */
public final class PrivacyRequestRemoteSource {
    public interface Callback {
        void onSubmitted(@NonNull JSONObject response);
        void onError(@NonNull String message);
    }

    private PrivacyRequestRemoteSource() {}

    /** Asks the server to erase this member's personal data and disable the account. */
    public static void requestErasure(@NonNull Context context, @NonNull String authToken, @NonNull Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("request_type", "ERASURE");
            body.put("request_id", UUID.randomUUID().toString());
            ApiClient.post(context, "/api/privacy/requests", body, authToken, new ApiClient.ApiCallback() {
                @Override public void onSuccess(int statusCode, @NonNull JSONObject json) { callback.onSubmitted(json); }
                @Override public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError("Your account could not be deleted right now. Please try again when you are online.");
                }
            });
        } catch (Exception e) {
            callback.onError("Your account could not be deleted right now. Please try again.");
        }
    }
}
