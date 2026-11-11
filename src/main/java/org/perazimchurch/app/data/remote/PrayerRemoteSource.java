package org.perazimchurch.app.data.remote;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Remote data source for prayer wall interactions and offline batch sync dispatching.
 */
public final class PrayerRemoteSource {

    public interface PrayersCallback {
        void onSuccess(@NonNull JSONArray prayers);
        void onError(int statusCode, @NonNull String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess(@NonNull JSONObject result);
        void onError(int statusCode, @NonNull String errorMessage);
    }

    public interface SyncCallback {
        void onSuccess(@NonNull JSONArray syncedIds,
                       @NonNull JSONArray rejectedMutations,
                       @NonNull JSONArray updatedPrayers,
                       long serverTime,
                       boolean hasMore,
                       @Nullable String nextCursor);
        void onError(int statusCode, @NonNull String errorMessage);
    }

    public static void fetchPrayers(@NonNull Context context,
                                    long sinceTimestamp,
                                    @NonNull PrayersCallback callback) {
        String endpoint = "/api/prayers" + (sinceTimestamp > 0 ? "?since=" + sinceTimestamp : "");
        ApiClient.get(context, endpoint, null, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                JSONArray prayers = json.optJSONArray("prayers");
                callback.onSuccess(prayers != null ? prayers : new JSONArray());
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public static void submitPrayer(@NonNull Context context,
                                    @Nullable String id,
                                    @NonNull String title,
                                    @NonNull String content,
                                    boolean isAnonymous,
                                    @Nullable String authorName,
                                    @Nullable String authToken,
                                    @NonNull ActionCallback callback) {
        try {
            JSONObject body = new JSONObject();
            if (id != null) body.put("id", id);
            body.put("title", title);
            body.put("content", content);
            body.put("isAnonymous", isAnonymous);
            if (authorName != null) body.put("authorName", authorName);

            ApiClient.post(context, "/api/prayers", body, authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage() != null ? e.getMessage() : "Error submitting prayer");
        }
    }

    public static void amenPrayer(@NonNull Context context,
                                  @NonNull String prayerId,
                                  @Nullable String userId,
                                  @Nullable String authToken,
                                  @NonNull ActionCallback callback) {
        try {
            JSONObject body = new JSONObject();
            if (userId != null) body.put("userId", userId);

            ApiClient.post(context, "/api/prayers/" + prayerId + "/amen", body, authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage() != null ? e.getMessage() : "Error amening prayer");
        }
    }

    public static void submitTestimony(@NonNull Context context,
                                       @NonNull String prayerId,
                                       @NonNull String testimony,
                                       @Nullable String authToken,
                                       @NonNull ActionCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("testimony", testimony);

            ApiClient.post(context, "/api/prayers/" + prayerId + "/testimony", body, authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage() != null ? e.getMessage() : "Error submitting testimony");
        }
    }

    public static void batchSync(@NonNull Context context,
                                 @NonNull JSONArray mutations,
                                 long lastSyncTimestamp,
                                 @Nullable String cursor,
                                 @Nullable String authToken,
                                 @NonNull SyncCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("mutations", mutations != null ? mutations : new JSONArray());
            body.put("lastSyncTimestamp", lastSyncTimestamp);
            if (cursor != null && !cursor.isEmpty()) {
                body.put("cursor", cursor);
            }

            ApiClient.post(context, "/api/sync", body, authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    JSONArray syncedIds = json.optJSONArray("syncedIds");
                    JSONArray rejectedMutations = json.optJSONArray("rejectedMutations");
                    JSONArray updatedPrayers = json.optJSONArray("updatedPrayers");
                    if (updatedPrayers == null) {
                        updatedPrayers = json.optJSONArray("prayers");
                    }
                    long serverTime = json.optLong("serverTime", System.currentTimeMillis());
                    boolean hasMore = json.optBoolean("hasMore", false);
                    String nextCursor = json.isNull("nextCursor") ? null : json.optString("nextCursor", null);

                    callback.onSuccess(
                            syncedIds != null ? syncedIds : new JSONArray(),
                            rejectedMutations != null ? rejectedMutations : new JSONArray(),
                            updatedPrayers != null ? updatedPrayers : new JSONArray(),
                            serverTime,
                            hasMore,
                            nextCursor
                    );
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage() != null ? e.getMessage() : "Error in batch sync");
        }
    }

    public static void batchSync(@NonNull Context context,
                                 @NonNull JSONArray mutations,
                                 long lastSyncTimestamp,
                                 @Nullable String authToken,
                                 @NonNull SyncCallback callback) {
        batchSync(context, mutations, lastSyncTimestamp, null, authToken, callback);
    }
}
