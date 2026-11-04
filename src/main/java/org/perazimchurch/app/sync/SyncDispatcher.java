package org.perazimchurch.app.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.PrayerDao;
import org.perazimchurch.app.data.local.entity.PrayerEntity;
import org.perazimchurch.app.data.local.entity.SyncQueueEntity;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.PrayerRemoteSource;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline-first sync dispatcher that flushes queued local mutations to the church server
 * and fetches newly published remote prayers into Room SQLite database.
 */
public final class SyncDispatcher {

    private static final String TAG = "SyncDispatcher";
    private static final String PREF_NAME = "perazim_sync_prefs";
    private static final String KEY_LAST_SYNC_TS = "last_sync_timestamp";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean IS_SYNCING = new AtomicBoolean(false);

    public interface SyncResultCallback {
        void onSyncComplete(boolean success, int syncedMutationsCount, int newRemotePrayersCount);
    }

    private SyncDispatcher() {}

    /**
     * Dispatches pending offline mutations and pulls remote updates.
     */
    public static void dispatchSync(@NonNull Context context, @Nullable SyncResultCallback callback) {
        final Context appContext = context.getApplicationContext();
        if (!IS_SYNCING.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress. Skipping redundant dispatch.");
            if (callback != null) {
                MAIN_HANDLER.post(() -> callback.onSyncComplete(true, 0, 0));
            }
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                SyncQueueManager queueManager = SyncQueueManager.getInstance(appContext);
                List<SyncQueueEntity> pending = queueManager.getPendingOperations(25);

                JSONArray mutationsArray = new JSONArray();
                if (pending != null) {
                    for (SyncQueueEntity item : pending) {
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put("id", item.getId());
                            obj.put("operationType", item.getOperationType());
                            obj.put("entityType", item.getEntityType());
                            obj.put("entityId", item.getEntityId());
                            obj.put("payloadJson", item.getPayloadJson() != null ? item.getPayloadJson() : "{}");
                            obj.put("timestamp", item.getTimestampMillis());
                            mutationsArray.put(obj);
                            queueManager.markSyncing(item.getId());
                        } catch (Exception e) {
                            Log.w(TAG, "Error serializing sync item: " + item.getId(), e);
                        }
                    }
                }

                SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                long lastSyncTs = prefs.getLong(KEY_LAST_SYNC_TS, 0);
                String authToken = SessionManager.getInstance(appContext).getAuthToken();

                PrayerRemoteSource.batchSync(appContext, mutationsArray, lastSyncTs, authToken, new PrayerRemoteSource.SyncCallback() {
                    @Override
                    public void onSuccess(@NonNull JSONArray syncedIds, @NonNull JSONArray updatedPrayers, long serverTime) {
                        EXECUTOR.execute(() -> {
                            int syncedCount = 0;
                            try {
                                // Mark synced mutations
                                for (int i = 0; i < syncedIds.length(); i++) {
                                    String id = syncedIds.optString(i, null);
                                    if (id != null) {
                                        queueManager.markSynced(id);
                                        syncedCount++;
                                    }
                                }

                                // Ingest updated prayers into Room
                                PerazimDatabase db = PerazimDatabase.getInstance(appContext);
                                PrayerDao prayerDao = db.prayerDao();
                                int newPrayersCount = 0;

                                for (int i = 0; i < updatedPrayers.length(); i++) {
                                    JSONObject pJson = updatedPrayers.optJSONObject(i);
                                    if (pJson == null) continue;

                                    String pId = pJson.optString("id", "");
                                    if (pId.isEmpty()) continue;

                                    String title = pJson.optString("title", "Prayer Request");
                                    String content = pJson.optString("content", "");
                                    String authorId = pJson.optString("author_id", "guest");
                                    String authorName = pJson.optString("author_name", "Fellow Saint");
                                    boolean isAnonymous = pJson.optBoolean("is_anonymous", false);
                                    int count = pJson.optInt("prayer_count", 0);
                                    boolean isAnswered = pJson.optBoolean("is_answered", false);
                                    long createdAt = pJson.optLong("created_at", System.currentTimeMillis());

                                    PrayerEntity existing = prayerDao.getPrayerById(pId);
                                    if (existing == null) {
                                        PrayerEntity newEntity = new PrayerEntity(
                                                pId,
                                                authorId,
                                                authorName,
                                                title,
                                                content,
                                                isAnonymous,
                                                isAnswered,
                                                count,
                                                createdAt
                                        );
                                        prayerDao.insert(newEntity);
                                        newPrayersCount++;
                                    } else {
                                        // Update counters & status while preserving local state
                                        existing.setPrayerCount(Math.max(existing.getPrayerCount(), count));
                                        if (isAnswered) {
                                            existing.setAnswered(true);
                                        }
                                        prayerDao.update(existing);
                                    }
                                }

                                prefs.edit().putLong(KEY_LAST_SYNC_TS, serverTime).apply();
                                Log.i(TAG, "Sync complete. Synced " + syncedCount + " mutations, ingested " + newPrayersCount + " prayers.");

                                final int finalSynced = syncedCount;
                                final int finalNew = newPrayersCount;
                                IS_SYNCING.set(false);
                                if (callback != null) {
                                    MAIN_HANDLER.post(() -> callback.onSyncComplete(true, finalSynced, finalNew));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error ingesting sync batch into Room", e);
                                IS_SYNCING.set(false);
                                if (callback != null) {
                                    MAIN_HANDLER.post(() -> callback.onSyncComplete(false, 0, 0));
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        EXECUTOR.execute(() -> {
                            Log.w(TAG, "Batch sync failed (HTTP " + statusCode + "): " + errorMessage);
                            // Mark pending operations as RETRY with backoff
                            if (pending != null) {
                                for (SyncQueueEntity item : pending) {
                                    queueManager.recordFailure(item.getId(), errorMessage, true);
                                }
                            }
                            IS_SYNCING.set(false);
                            if (callback != null) {
                                MAIN_HANDLER.post(() -> callback.onSyncComplete(false, 0, 0));
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Fatal error executing sync", e);
                IS_SYNCING.set(false);
                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onSyncComplete(false, 0, 0));
                }
            }
        });
    }
}
