package com.example.app.sync;

import android.content.Context;
import android.util.Log;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.entity.SyncQueueEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Self-contained verification helper for Phase 1.6 Sync Queue Mechanics.
 * Validates:
 * 1. Enqueue new operation.
 * 2. Duplicate operation is rejected / deduplicated (idempotency key & entity match).
 * 3. Status transitions: PENDING -> SYNCING -> FAILED -> retry -> SYNCED.
 * 4. Failure preservation: lastError and retryCount correctly stored and retrieved.
 * 5. Conflict handling, cancellation, and purge mechanics.
 */
public class SyncQueueVerificationHelper {

    private static final String TAG = "PerazimSyncQueue";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onVerificationComplete(boolean success, String message);
    }

    public static void runVerification(Context context) {
        runVerification(context, null);
    }

    public static void runVerification(Context context, VerificationCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Log.d(TAG, "Starting Phase 1.6 Sync Queue Verification Gate...");
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                SyncQueueManager manager = new SyncQueueManager(db.syncQueueDao());

                verifyInternal(manager);

                Log.i(TAG, "[PERAZIM-SYNC-QUEUE-GATE-1.6: SUCCESS]");
                if (callback != null) {
                    callback.onVerificationComplete(true, "Phase 1.6 Sync Queue verified successfully.");
                }
            } catch (Exception e) {
                Log.e(TAG, "[PERAZIM-SYNC-QUEUE-GATE-1.6: FAILED] " + e.getMessage(), e);
                if (callback != null) {
                    callback.onVerificationComplete(false, e.getMessage());
                }
            }
        });
    }

    /**
     * Executes the verification suite synchronously against a {@link SyncQueueManager}.
     *
     * @param manager The SyncQueueManager under test.
     * @return true if all verification steps pass.
     * @throws IllegalStateException if any assertion fails.
     */
    public static boolean verifyInternal(SyncQueueManager manager) {
        long testTimestamp = System.currentTimeMillis();
        String testSuffix = "_" + testTimestamp;
        String entityId = "prayer_gate" + testSuffix;
        String idempKey = "idemp_gate" + testSuffix;
        String payload = "{\"prayerId\":\"" + entityId + "\",\"title\":\"Breakthrough Faith\"}";

        int initialPendingCount = manager.getPendingCount();

        // ---------------------------------------------------------------------
        // 1. Enqueue new operation
        // ---------------------------------------------------------------------
        String syncId = manager.enqueue(
                SyncOperationType.CREATE.name(),
                "PRAYER",
                entityId,
                payload,
                idempKey
        );

        if (syncId == null || syncId.isEmpty()) {
            throw new IllegalStateException("Enqueue failed: returned syncId is null or empty.");
        }

        SyncQueueEntity item = manager.getItem(syncId);
        if (item == null) {
            throw new IllegalStateException("Enqueue failed: could not retrieve item with ID " + syncId);
        }

        if (!SyncStatus.PENDING.name().equals(item.getStatus())) {
            throw new IllegalStateException("Enqueue failed: expected status PENDING, got " + item.getStatus());
        }

        if (item.getRetryCount() != 0) {
            throw new IllegalStateException("Enqueue failed: expected retryCount=0, got " + item.getRetryCount());
        }

        if (!"PRAYER".equals(item.getEntityType())) {
            throw new IllegalStateException("Enqueue failed: expected entityType 'PRAYER', got " + item.getEntityType());
        }

        if (!SyncOperationType.CREATE.name().equals(item.getOperationType())) {
            throw new IllegalStateException("Enqueue failed: expected operationType 'CREATE', got " + item.getOperationType());
        }

        if (manager.getPendingCount() != initialPendingCount + 1) {
            throw new IllegalStateException("Enqueue failed: pending count expected "
                    + (initialPendingCount + 1) + ", got " + manager.getPendingCount());
        }

        List<SyncQueueEntity> pendingList = manager.getPendingOperations(100);
        boolean foundInPending = false;
        for (SyncQueueEntity e : pendingList) {
            if (syncId.equals(e.getId())) {
                foundInPending = true;
                break;
            }
        }
        if (!foundInPending) {
            throw new IllegalStateException("Enqueue failed: item not found in getPendingOperations list.");
        }
        Log.d(TAG, "Step 1 passed: New operation enqueued successfully (id=" + syncId + ").");

        // ---------------------------------------------------------------------
        // 2. Duplicate operation is rejected / deduplicated
        // ---------------------------------------------------------------------
        // 2a: Duplicate with same idempotency key
        String dupId1 = manager.enqueue(
                SyncOperationType.CREATE.name(),
                "PRAYER",
                entityId,
                payload,
                idempKey
        );
        if (!syncId.equals(dupId1)) {
            throw new IllegalStateException("Deduplication failed: expected existing ID "
                    + syncId + ", got " + dupId1);
        }
        if (manager.getPendingCount() != initialPendingCount + 1) {
            throw new IllegalStateException("Deduplication failed: pending count incremented on duplicate idempotency key.");
        }

        // 2b: Duplicate with same (entityType, entityId, operationType) and different/null idempotency key
        String dupId2 = manager.enqueue(
                SyncOperationType.CREATE.name(),
                "PRAYER",
                entityId,
                payload,
                null
        );
        if (!syncId.equals(dupId2)) {
            throw new IllegalStateException("Deduplication failed: expected existing ID "
                    + syncId + ", got " + dupId2);
        }
        if (manager.getPendingCount() != initialPendingCount + 1) {
            throw new IllegalStateException("Deduplication failed: pending count incremented on duplicate entity triple.");
        }
        Log.d(TAG, "Step 2 passed: Duplicate operations rejected and deduplicated successfully.");

        // ---------------------------------------------------------------------
        // 3. Status transitions: PENDING -> SYNCING -> FAILED -> retry -> SYNCED
        //    Failure preservation: lastError and retryCount correctly stored and retrieved
        // ---------------------------------------------------------------------
        // 3a. PENDING -> SYNCING
        manager.markSyncing(syncId);
        item = manager.getItem(syncId);
        if (item == null || !SyncStatus.SYNCING.name().equals(item.getStatus())) {
            throw new IllegalStateException("Transition to SYNCING failed: expected SYNCING, got "
                    + (item == null ? "null" : item.getStatus()));
        }

        // 3b. SYNCING -> FAILED (with willRetry = false) & Failure preservation
        String firstError = "HTTP 503: Service Unavailable";
        manager.recordFailure(syncId, firstError, false);
        item = manager.getItem(syncId);
        if (item == null) {
            throw new IllegalStateException("Failure recording failed: item is null.");
        }
        if (!SyncStatus.FAILED.name().equals(item.getStatus())) {
            throw new IllegalStateException("Transition to FAILED failed: expected FAILED, got " + item.getStatus());
        }
        if (item.getRetryCount() != 1) {
            throw new IllegalStateException("Failure preservation failed: expected retryCount=1, got " + item.getRetryCount());
        }
        if (!firstError.equals(item.getLastError())) {
            throw new IllegalStateException("Failure preservation failed: expected lastError '"
                    + firstError + "', got '" + item.getLastError() + "'");
        }
        Log.d(TAG, "Step 3b passed: Transition to FAILED and failure preservation verified (retryCount=1).");

        // 3c. FAILED -> retry (PENDING)
        manager.retry(syncId);
        item = manager.getItem(syncId);
        if (item == null || !SyncStatus.PENDING.name().equals(item.getStatus())) {
            throw new IllegalStateException("Retry transition failed: expected PENDING, got "
                    + (item == null ? "null" : item.getStatus()));
        }

        // 3d. Retry with exponential backoff (willRetry = true)
        manager.markSyncing(syncId);
        String secondError = "Network connection timeout";
        manager.recordFailure(syncId, secondError, true);
        item = manager.getItem(syncId);
        if (item == null) {
            throw new IllegalStateException("Backoff retry recording failed: item is null.");
        }
        if (!SyncStatus.PENDING.name().equals(item.getStatus())) {
            throw new IllegalStateException("Expected backoff to return to PENDING, got " + item.getStatus());
        }
        if (item.getRetryCount() != 2) {
            throw new IllegalStateException("Failure preservation failed: expected retryCount=2, got " + item.getRetryCount());
        }
        if (!secondError.equals(item.getLastError())) {
            throw new IllegalStateException("Failure preservation failed: expected lastError '"
                    + secondError + "', got '" + item.getLastError() + "'");
        }

        // 3e. PENDING -> SYNCING -> SYNCED
        manager.markSyncing(syncId);
        item = manager.getItem(syncId);
        if (item == null || !SyncStatus.SYNCING.name().equals(item.getStatus())) {
            throw new IllegalStateException("Transition to SYNCING failed: expected SYNCING, got "
                    + (item == null ? "null" : item.getStatus()));
        }

        manager.markSynced(syncId);
        item = manager.getItem(syncId);
        if (item == null || !SyncStatus.SYNCED.name().equals(item.getStatus())) {
            throw new IllegalStateException("Transition to SYNCED failed: expected SYNCED, got "
                    + (item == null ? "null" : item.getStatus()));
        }
        Log.d(TAG, "Step 3 passed: Status transitions PENDING -> SYNCING -> FAILED -> retry -> SYNCED verified.");

        // ---------------------------------------------------------------------
        // 4. Conflict, Cancelled, and Purge mechanics
        // ---------------------------------------------------------------------
        // Conflict
        String conflictKey = "idemp_conflict" + testSuffix;
        String conflictId = manager.enqueue(
                SyncOperationType.UPDATE.name(),
                "USER",
                "user_gate_1",
                "{}",
                conflictKey
        );
        manager.markConflict(conflictId, "Version conflict 409: Remote edit detected");
        SyncQueueEntity conflictItem = manager.getItem(conflictId);
        if (conflictItem == null || !SyncStatus.CONFLICT.name().equals(conflictItem.getStatus())) {
            throw new IllegalStateException("markConflict failed: expected CONFLICT, got "
                    + (conflictItem == null ? "null" : conflictItem.getStatus()));
        }
        if (!"Version conflict 409: Remote edit detected".equals(conflictItem.getLastError())) {
            throw new IllegalStateException("markConflict details mismatch: got " + conflictItem.getLastError());
        }

        // Cancelled
        String cancelKey = "idemp_cancel" + testSuffix;
        String cancelId = manager.enqueue(
                SyncOperationType.DELETE.name(),
                "HYMN",
                "hymn_gate_1",
                "{}",
                cancelKey
        );
        manager.markCancelled(cancelId);
        SyncQueueEntity cancelItem = manager.getItem(cancelId);
        if (cancelItem == null || !SyncStatus.CANCELLED.name().equals(cancelItem.getStatus())) {
            throw new IllegalStateException("markCancelled failed: expected CANCELLED, got "
                    + (cancelItem == null ? "null" : cancelItem.getStatus()));
        }

        // Purge completed items (olderThanMillis = 0 removes all SYNCED and CANCELLED)
        manager.purgeCompleted(0);
        if (manager.getItem(syncId) != null) {
            throw new IllegalStateException("purgeCompleted failed: SYNCED item " + syncId + " was not purged.");
        }
        if (manager.getItem(cancelId) != null) {
            throw new IllegalStateException("purgeCompleted failed: CANCELLED item " + cancelId + " was not purged.");
        }
        if (manager.getItem(conflictId) == null) {
            throw new IllegalStateException("purgeCompleted error: CONFLICT item " + conflictId + " was unexpectedly purged.");
        }

        // Clean up conflict test item
        manager.delete(conflictId);
        if (manager.getItem(conflictId) != null) {
            throw new IllegalStateException("Cleanup failed: conflict test item remains.");
        }

        Log.d(TAG, "Step 4 passed: Conflict, cancellation, and purge completed successfully.");
        return true;
    }
}
