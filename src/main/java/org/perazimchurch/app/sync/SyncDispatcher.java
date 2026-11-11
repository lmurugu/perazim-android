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
     * Recovers orphaned SYNCING items, handles permanent vs retryable rejections,
     * paginates through all remote pages, and propagates prayer counts, testimony, and answered status into Room.
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

                // REQ-08: Recover any stuck operations in status 'SYNCING' (e.g. from app crash or killed process)
                int recoveredCount = queueManager.recoverOrphanedSyncing();
                if (recoveredCount > 0) {
                    Log.i(TAG, "Recovered " + recoveredCount + " orphaned SYNCING operations back to PENDING.");
                }

                // Pull pending operations
                SessionManager session = SessionManager.getInstance(appContext);
                final String syncUserId;
                final String authToken;
                final boolean guestSession;
                synchronized (session) {
                    syncUserId = session.getActiveUserId();
                    authToken = session.getAuthToken();
                    guestSession = session.isGuest();
                }
                List<SyncQueueEntity> pending = new java.util.ArrayList<>();
                if (authToken != null && !guestSession) {
                    for (SyncQueueEntity item : queueManager.getPendingOperations(0)) {
                        // Legacy unowned drafts are retained for manual recovery, never attributed to another member.
                        if (syncUserId.equals(SyncQueueManager.ownerOf(item)) && pending.size() < 25) pending.add(item);
                    }
                }
                JSONArray mutationsArray = new JSONArray();
                if (pending != null && !pending.isEmpty()) {
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

                SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME + "_" + syncUserId, Context.MODE_PRIVATE);
                long lastSyncTs = prefs.getLong(KEY_LAST_SYNC_TS, 0);

                // Kick off multi-page sync starting at page 1 with cursor = 0
                executeSyncPage(
                        appContext,
                        queueManager,
                        pending,
                        mutationsArray,
                        lastSyncTs,
                        null,
                        1,
                        0,
                        0,
                        0L,
                        authToken,
                        prefs,
                        callback
                );
            } catch (Exception e) {
                Log.e(TAG, "Fatal error executing sync", e);
                IS_SYNCING.set(false);
                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onSyncComplete(false, 0, 0));
                }
            }
        });
    }

    /**
     * Executes a single sync page request, processes mutations & updated prayers,
     * and loops to subsequent pages before updating KEY_LAST_SYNC_TS.
     */
    private static void executeSyncPage(
            @NonNull Context appContext,
            @NonNull SyncQueueManager queueManager,
            @Nullable List<SyncQueueEntity> pendingMutations,
            @NonNull JSONArray mutationsToSend,
            long lastSyncTs,
            @Nullable String cursor,
            int pageNumber,
            int accumulatedSyncedCount,
            int accumulatedIngestedCount,
            long maxServerTimeSeen,
            @Nullable String authToken,
            @NonNull SharedPreferences prefs,
            @Nullable SyncResultCallback callback) {

        PrayerRemoteSource.batchSync(appContext, mutationsToSend, lastSyncTs, cursor, authToken, new PrayerRemoteSource.SyncCallback() {
            @Override
            public void onSuccess(@NonNull JSONArray syncedIds,
                                  @NonNull JSONArray rejectedMutations,
                                  @NonNull JSONArray updatedPrayers,
                                  long serverTime,
                                  boolean hasMore,
                                  @Nullable String nextCursor) {
                EXECUTOR.execute(() -> {
                    try {
                        int pageSyncedCount = 0;

                        // On page 1, process mutations acknowledgment & rejections
                        if (pageNumber == 1 && pendingMutations != null && !pendingMutations.isEmpty()) {
                            java.util.Set<String> processedIds = new java.util.HashSet<>();

                            // 1. Process successfully synced mutations
                            for (int i = 0; i < syncedIds.length(); i++) {
                                String id = syncedIds.optString(i, null);
                                if (id != null && !id.isEmpty()) {
                                    queueManager.markSynced(id);
                                    processedIds.add(id);
                                    pageSyncedCount++;
                                }
                            }

                            // 2. Handle rejected mutations (REQ-08)
                            for (int i = 0; i < rejectedMutations.length(); i++) {
                                JSONObject rej = rejectedMutations.optJSONObject(i);
                                if (rej == null) continue;
                                String rejId = rej.optString("id", null);
                                if (rejId == null || rejId.isEmpty()) continue;
                                processedIds.add(rejId);

                                String reason = rej.optString("reason", "UNKNOWN");
                                String message = rej.optString("message", "Mutation rejected by server");

                                if (SyncQueueManager.isPermanentFailure(reason, -1)) {
                                    // Permanent rejection: preserve user content/draft in queue, mark FAILED
                                    queueManager.recordPermanentFailure(rejId, reason, message);
                                    Log.w(TAG, "Permanent sync mutation rejected; details retained for the member's local retry state.");
                                } else {
                                    // Transient failure: retry with exponential backoff
                                    queueManager.recordFailure(rejId, "[" + reason + "] " + message, true);
                                }
                            }

                            // 3. Mark unacknowledged items for retry (network dropped / partial server batch)
                            for (SyncQueueEntity item : pendingMutations) {
                                if (!processedIds.contains(item.getId())) {
                                    Log.w(TAG, "Mutation " + item.getId() + " unacknowledged by server; scheduling retry.");
                                    queueManager.recordTransportFailure(item.getId(), "Unacknowledged in batch response");
                                }
                            }
                        }

                        // REQ-09: Ingest updated prayers into Room PrayerDao
                        PerazimDatabase db = PerazimDatabase.getInstance(appContext);
                        PrayerDao prayerDao = db.prayerDao();
                        int pageIngestedCount = 0;

                        for (int i = 0; i < updatedPrayers.length(); i++) {
                            JSONObject pJson = updatedPrayers.optJSONObject(i);
                            if (pJson == null) continue;

                            String pId = pJson.optString("id", "");
                            if (pId.isEmpty()) continue;

                            String title = pJson.optString("title", "Prayer Request");
                            String content = pJson.optString("content", "");
                            String authorId = pJson.optString("author_id", pJson.optString("userId", "guest"));
                            String authorName = pJson.optString("author_name", pJson.optString("authorName", "Fellow Saint"));
                            boolean isAnonymous = pJson.optBoolean("is_anonymous", pJson.optBoolean("isAnonymous", false));
                            int count = pJson.optInt("prayer_count", pJson.optInt("prayerCount", 0));
                            boolean isAnswered = pJson.optBoolean("is_answered", pJson.optBoolean("isAnswered", false));
                            String testimony = pJson.optString("testimony", "").trim();
                            long createdAt = pJson.optLong("created_at", pJson.optLong("createdMillis", System.currentTimeMillis()));

                            // Format content with testimony note if testimony exists
                            String formattedContent = content;
                            if (!testimony.isEmpty()) {
                                if (formattedContent.isEmpty()) {
                                    formattedContent = "[Testimony]: " + testimony;
                                } else if (!formattedContent.contains("[Testimony]") && !formattedContent.contains(testimony)) {
                                    formattedContent = formattedContent + "\n\n[Testimony]: " + testimony;
                                }
                            }

                            PrayerEntity existing = prayerDao.getPrayerById(pId);
                            if (existing == null) {
                                PrayerEntity newEntity = new PrayerEntity(
                                        pId,
                                        authorId,
                                        authorName,
                                        title,
                                        formattedContent,
                                        isAnonymous,
                                        isAnswered,
                                        count,
                                        createdAt
                                );
                                prayerDao.insert(newEntity);
                                pageIngestedCount++;
                            } else {
                                boolean changed = false;

                                // Propagate prayer counts (take maximum to preserve offline increment)
                                int maxCount = Math.max(existing.getPrayerCount(), count);
                                if (existing.getPrayerCount() != maxCount) {
                                    existing.setPrayerCount(maxCount);
                                    changed = true;
                                }

                                // Propagate isAnswered status
                                if (isAnswered && !existing.isAnswered()) {
                                    existing.setAnswered(true);
                                    changed = true;
                                }

                                // Propagate testimony into Room
                                if (!testimony.isEmpty()) {
                                    String exContent = existing.getContent();
                                    if (exContent == null || (!exContent.contains("[Testimony]") && !exContent.contains(testimony))) {
                                        String updated = (exContent != null && !exContent.isEmpty())
                                                ? exContent + "\n\n[Testimony]: " + testimony
                                                : "[Testimony]: " + testimony;
                                        existing.setContent(updated);
                                        changed = true;
                                    }
                                }

                                // Update title if remote changed
                                if (title != null && !title.isEmpty() && !title.equals(existing.getTitle())) {
                                    existing.setTitle(title);
                                    changed = true;
                                }

                                if (changed) {
                                    prayerDao.update(existing);
                                }
                                pageIngestedCount++;
                            }
                        }

                        int totalSynced = accumulatedSyncedCount + pageSyncedCount;
                        int totalIngested = accumulatedIngestedCount + pageIngestedCount;
                        long highestServerTime = Math.max(maxServerTimeSeen, serverTime);

                        // Multi-page pagination evaluation (REQ-08)
                        String effectiveNextCursor = nextCursor;
                        if ((effectiveNextCursor == null || effectiveNextCursor.isEmpty()) && hasMore && updatedPrayers.length() > 0) {
                            JSONObject lastPrayer = updatedPrayers.optJSONObject(updatedPrayers.length() - 1);
                            if (lastPrayer != null) {
                                effectiveNextCursor = lastPrayer.optLong("updated_at", 0) + ":" + lastPrayer.optString("id", "");
                            }
                        }
                        boolean morePages = hasMore && effectiveNextCursor != null && !effectiveNextCursor.isEmpty()
                                && !effectiveNextCursor.equals(cursor) && updatedPrayers.length() > 0;

                        if (morePages) {
                            Log.d(TAG, "Fetching next sync page " + (pageNumber + 1) + " (cursor=" + effectiveNextCursor + ")...");
                            executeSyncPage(
                                    appContext,
                                    queueManager,
                                    pendingMutations,
                                    new JSONArray(), // Do not re-send mutations on subsequent pages
                                    lastSyncTs,
                                    effectiveNextCursor,
                                    pageNumber + 1,
                                    totalSynced,
                                    totalIngested,
                                    highestServerTime,
                                    authToken,
                                    prefs,
                                    callback
                            );
                        } else {
                            // All pages consumed! Only advance completed checkpoint timestamp after consuming every page
                            long checkpointToSave = highestServerTime > 0 ? highestServerTime : System.currentTimeMillis();
                            prefs.edit().putLong(KEY_LAST_SYNC_TS, checkpointToSave).apply();
                            Log.i(TAG, "Sync complete across " + pageNumber + " page(s). Synced "
                                    + totalSynced + " mutations, ingested " + totalIngested
                                    + " prayers. Checkpoint advanced to " + checkpointToSave);

                            IS_SYNCING.set(false);
                            if (callback != null) {
                                MAIN_HANDLER.post(() -> callback.onSyncComplete(true, totalSynced, totalIngested));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error ingesting sync batch into Room", e);
                        IS_SYNCING.set(false);
                        if (callback != null) {
                            MAIN_HANDLER.post(() -> callback.onSyncComplete(false, accumulatedSyncedCount, accumulatedIngestedCount));
                        }
                    }
                });
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                EXECUTOR.execute(() -> {
                    Log.w(TAG, "Batch sync failed on page " + pageNumber + " (HTTP " + statusCode + ").");

                    // Mark pending operations on failure
                    if (pageNumber == 1 && pendingMutations != null) {
                        boolean isPermanent = statusCode >= 400 && statusCode < 500 && statusCode != 408;
                        for (SyncQueueEntity item : pendingMutations) {
                            if (isPermanent) {
                                queueManager.recordPermanentFailure(item.getId(), "HTTP_" + statusCode, errorMessage);
                            } else {
                                queueManager.recordTransportFailure(item.getId(), "HTTP " + statusCode + ": " + errorMessage);
                            }
                        }
                    }

                    // On error, do not advance KEY_LAST_SYNC_TS checkpoint
                    IS_SYNCING.set(false);
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onSyncComplete(false, accumulatedSyncedCount, accumulatedIngestedCount));
                    }
                });
            }
        });
    }
}
