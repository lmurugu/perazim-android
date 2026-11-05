package org.perazimchurch.app.data.remote;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Remote data source for user registration, authentication, and profile fetching.
 */
public final class AuthRemoteSource {

    public interface AuthCallback {
        void onSuccess(@NonNull String token, @NonNull JSONObject userJson);
        void onError(int statusCode, @NonNull String errorMessage);
    }

    public static void register(@NonNull Context context,
                                @NonNull String phone,
                                @NonNull String pin,
                                @NonNull String name,
                                @Nullable String campusId,
                                @NonNull AuthCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            body.put("pin", pin);
            body.put("name", name);
            body.put("campus_id", (campusId != null && !campusId.isEmpty()) ? campusId : "campus_embu");

            ApiClient.post(context, "/api/auth/register", body, null, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    String token = json.optString("token", "");
                    JSONObject user = json.optJSONObject("user");
                    if (user == null) user = new JSONObject();
                    callback.onSuccess(token, user);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage() != null ? e.getMessage() : "Error building registration request");
        }
    }

    public static void login(@NonNull Context context,
                             @NonNull String phone,
                             @NonNull String pin,
                             @NonNull AuthCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            body.put("pin", pin);

            ApiClient.post(context, "/api/auth/login", body, null, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    String token = json.optString("token", "");
                    JSONObject user = json.optJSONObject("user");
                    if (user == null) user = new JSONObject();
                    callback.onSuccess(token, user);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage() != null ? e.getMessage() : "Error building login request");
        }
    }

    public static void fetchProfile(@NonNull Context context,
                                    @NonNull String authToken,
                                    @NonNull AuthCallback callback) {
        ApiClient.get(context, "/api/auth/me", authToken, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                JSONObject user = json.optJSONObject("user");
                if (user == null) user = new JSONObject();
                callback.onSuccess(authToken, user);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public static void logout(@NonNull Context context, @Nullable String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) return;
        ApiClient.post(context, "/api/auth/logout", new JSONObject(), authToken, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {}

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {}
        });
    }
}
