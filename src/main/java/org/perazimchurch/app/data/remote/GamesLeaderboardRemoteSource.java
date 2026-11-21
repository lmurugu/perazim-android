package org.perazimchurch.app.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Church-wide Bible games leaderboard (server routes under /api/games).
 * Members appear only after opting in, and only by first name.
 */
public final class GamesLeaderboardRemoteSource {
    private GamesLeaderboardRemoteSource() { }

    public interface Callback {
        void onSuccess(@NonNull JSONObject json);
        void onError(int statusCode, @NonNull String message);
    }

    /** Uploads a batch of stars. The same runId is resent on retry, so the server never counts it twice. */
    public static void uploadStars(@NonNull Context context, @Nullable String token, @NonNull String runId,
                                   int points, @NonNull Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("runId", runId);
            body.put("game", "bible_games");
            body.put("points", points);
        } catch (JSONException impossible) {
            callback.onError(0, "Could not prepare the upload");
            return;
        }
        ApiClient.post(context, "/api/games/scores", body, token, wrap(callback));
    }

    public static void leaderboard(@NonNull Context context, @Nullable String token, boolean allTime,
                                   @NonNull Callback callback) {
        ApiClient.get(context, "/api/games/leaderboard?period=" + (allTime ? "all" : "week"), token, wrap(callback));
    }

    /** Downloads the public game settings. Needs no sign-in. */
    public static void settings(@NonNull Context context, @NonNull Callback callback) {
        ApiClient.get(context, "/api/games/settings", null, wrap(callback));
    }

    public static void profile(@NonNull Context context, @NonNull String token, @NonNull Callback callback) {
        ApiClient.get(context, "/api/games/leaderboard/me", token, wrap(callback));
    }

    public static void setOptIn(@NonNull Context context, @NonNull String token, boolean optIn,
                                @NonNull Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("optIn", optIn);
        } catch (JSONException impossible) {
            callback.onError(0, "Could not prepare the request");
            return;
        }
        ApiClient.put(context, "/api/games/leaderboard/me", body, token, wrap(callback));
    }

    private static ApiClient.ApiCallback wrap(@NonNull Callback callback) {
        return new ApiClient.ApiCallback() {
            @Override public void onSuccess(int statusCode, @NonNull JSONObject json) { callback.onSuccess(json); }
            @Override public void onError(int statusCode, @NonNull String errorMessage) { callback.onError(statusCode, errorMessage); }
        };
    }
}
