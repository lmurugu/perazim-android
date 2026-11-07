package org.perazimchurch.app.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/** Network-only Phase B membership client. Admin calls never enter the offline queue. */
public final class MembershipRemoteSource {
    private MembershipRemoteSource() {}

    public interface Callback {
        void onSuccess(@NonNull JSONObject response);
        void onError(int statusCode, @NonNull String message);
    }

    public static void status(Context c, String token, Callback cb) { ordinary(c, "GET", "/api/membership/status", null, token, cb); }
    public static void permissions(Context c, String token, Callback cb) { ordinary(c, "GET", "/api/access/permissions", null, token, cb); }
    public static void submitClaim(Context c, JSONObject body, String token, Callback cb) { ordinary(c, "POST", "/api/membership/claims", body, token, cb); }
    public static void stepUp(Context c, JSONObject body, String token, Callback cb) { ordinary(c, "POST", "/api/admin/security/step-up", body, token, cb); }

    public static void admin(Context c, String method, String path, @Nullable JSONObject body,
                             String token, String privilegedToken, Callback cb) {
        ApiClient.privileged(c, method, path, body, token, privilegedToken, callback(cb));
    }

    private static void ordinary(Context c, String method, String path, @Nullable JSONObject body, String token, Callback cb) {
        ApiClient.ApiCallback callback = callback(cb);
        if ("GET".equals(method)) ApiClient.get(c, path, token, callback);
        else ApiClient.post(c, path, body, token, callback);
    }

    private static ApiClient.ApiCallback callback(Callback callback) {
        return new ApiClient.ApiCallback() {
            @Override public void onSuccess(int statusCode, @NonNull JSONObject json) { callback.onSuccess(json); }
            @Override public void onError(int statusCode, @NonNull String errorMessage) { callback.onError(statusCode, errorMessage); }
        };
    }
}
