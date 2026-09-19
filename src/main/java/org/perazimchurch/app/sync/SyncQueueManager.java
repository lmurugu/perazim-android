package org.perazimchurch.app.sync;

import android.content.Context;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.SyncQueueDao;
import org.perazimchurch.app.data.local.entity.SyncQueueEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manager coordinating offline mutation queuing, deduplication, retry policies with
 * exponential backoff, status transitions, and purging of completed records.
 * Compliant with Phase 1.6 Sync Queue Mechanics.
 */
public class SyncQueueManager {

    public static final int MAX_RETRIES = 3;
    public static final long BASE_BACKOFF_MILLIS = 1000L;

    private static volatile SyncQueueManager INSTANCE;
    private final SyncQueueDao syncQueueDao;

    public SyncQueueManager(SyncQueueDao syncQueueDao) {
        this.syncQueueDao = syncQueueDao;
    }

    public SyncQueueManager(Context context) {
        this(PerazimDatabase.getInstance(context).syncQueueDao());
    }

    public static SyncQueueManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SyncQueueManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SyncQueueManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public static synchronized void resetInstance() {
        INSTANCE = null;
    }

    public SyncQueueDao getSyncQueueDao() {
        return syncQueueDao;
    }

    /**
     * Enqueues an operation into the sync queue.
     * Checks for existing pending/syncing item with same idempotencyKey or
     * (entityType, entityId, operationType) to prevent duplicates.
     * Inserts SyncQueueEntity with status PENDING, retryCount=0, timestamp=System.currentTimeMillis().
     *
     * @param operationType  Type of operation (CREATE, UPDATE, DELETE).
     * @param entityType     Target entity type (e.g. PRAYER, USER, MESSAGE).
     * @param entityId       Identifier of the target entity.
     * @param payloadJson    Serialized payload for the sync operation.
     * @param idempotencyKey Optional idempotency key for deduplication.
     * @return Sync queue item ID (existing ID if deduplicated, or newly generated ID).
     */
    public synchronized String enqueue(String operationType, String entityType, String entityId,
                                       String payloadJson, String idempotencyKey) {
        // 1. Check for existing pending/syncing item with same idempotencyKey
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            SyncQueueEntity existingById = syncQueueDao.getItemById(idempotencyKey.trim());
            if (existingById != null) {
                String status = existingById.getStatus();
                if (SyncStatus.PENDING.name().equalsIgnoreCase(status)
                        || SyncStatus.SYNCING.name().equalsIgnoreCase(status)) {
                    return existingById.getId();
                }
            }
        }

        // 2. Check for existing pending/syncing item with same (entityType, entityId, operationType)
        if (entityType != null && entityId != null && operationType != null) {
            SyncQueueEntity existingByEntity = syncQueueDao.findPendingOrSyncing(entityType, entityId, operationType);
            if (existingByEntity != null) {
                return existingByEntity.getId();
            }
        }

        // 3. No existing pending/syncing item found; create new SyncQueueEntity
        String id = (idempotencyKey != null && !idempotencyKey.trim().isEmpty())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        long timestamp = System.currentTimeMillis();
        SyncQueueEntity newEntity = new SyncQueueEntity(
                id,
                operationType != null ? operationType : SyncOperationType.CREATE.name(),
                entityType,
                entityId,
                payloadJson,
                timestamp,
                SyncStatus.PENDING.name(),
                0,
                null
        );

        syncQueueDao.insert(newEntity);
        return id;
    }

    /**
     * Overload accepting typed {@link SyncOperationType}.
     */
    public String enqueue(SyncOperationType operationType, String entityType, String entityId,
                           String payloadJson, String idempotencyKey) {
        return enqueue(operationType != null ? operationType.name() : SyncOperationType.CREATE.name(),
                entityType, entityId, payloadJson, idempotencyKey);
    }

    /**
     * Fetches pending operations ordered by timestamp.
     *
     * @param limit Maximum number of records to return. If <= 0, returns all pending items.
     * @return List of pending SyncQueueEntity items.
     */
    public List<SyncQueueEntity> getPendingOperations(int limit) {
        if (limit <= 0) {
            List<SyncQueueEntity> items = syncQueueDao.getPendingItems();
            return items != null ? items : Collections.emptyList();
        }
        List<SyncQueueEntity> items = syncQueueDao.getPendingOperations(limit);
        return items != null ? items : Collections.emptyList();
    }

    /**
     * Transitions item status to SYNCING.
     *
     * @param id The sync queue item ID.
     */
    public synchronized void markSyncing(String id) {
        if (id == null) return;
        SyncQueueEntity item = syncQueueDao.getItemById(id);
        if (item != null) {
            item.setStatus(SyncStatus.SYNCING.name());
            syncQueueDao.update(item);
        }
    }

    /**
     * Transitions item status to SYNCED.
     *
     * @param id The sync queue item ID.
     */
    public synchronized void markSynced(String id) {
        if (id == null) return;
        SyncQueueEntity item = syncQueueDao.getItemById(id);
        if (item != null) {
            item.setStatus(SyncStatus.SYNCED.name());
            syncQueueDao.update(item);
        }
    }

    /**
     * Records failure for an operation.
     * Increments retryCount.
     * Sets lastError = errorMessage.
     * If retryCount >= MAX_RETRIES or !willRetry, transitions status to FAILED;
     * else returns to PENDING with exponential backoff delay.
     *
     * @param id           The sync queue item ID.
     * @param errorMessage Descriptive error message.
     * @param willRetry    Whether this operation should be retried.
     */
    public synchronized void recordFailure(String id, String errorMessage, boolean willRetry) {
        if (id == null) return;
        SyncQueueEntity item = syncQueueDao.getItemById(id);
        if (item == null) return;

        int newRetryCount = item.getRetryCount() + 1;
        item.setRetryCount(newRetryCount);
        item.setLastError(errorMessage);

        if (!willRetry || newRetryCount >= MAX_RETRIES) {
            item.setStatus(SyncStatus.FAILED.name());
        } else {
            item.setStatus(SyncStatus.PENDING.name());
            long backoffDelay = (long) Math.pow(2, newRetryCount) * BASE_BACKOFF_MILLIS;
            item.setTimestampMillis(System.currentTimeMillis() + backoffDelay);
        }
        syncQueueDao.update(item);
    }

    /**
     * Transitions item status to CONFLICT.
     *
     * @param id              The sync queue item ID.
     * @param conflictDetails Explanation of the detected conflict.
     */
    public synchronized void markConflict(String id, String conflictDetails) {
        if (id == null) return;
        SyncQueueEntity item = syncQueueDao.getItemById(id);
        if (item != null) {
            item.setStatus(SyncStatus.CONFLICT.name());
            if (conflictDetails != null) {
                item.setLastError(conflictDetails);
            }
            syncQueueDao.update(item);
        }
    }

    /**
     * Transitions item status to CANCELLED.
     *
     * @param id The sync queue item ID.
     */
    public synchronized void markCancelled(String id) {
        if (id == null) return;
        SyncQueueEntity item = syncQueueDao.getItemById(id);
        if (item != null) {
            item.setStatus(SyncStatus.CANCELLED.name());
            syncQueueDao.update(item);
        }
    }

    /**
     * Removes SYNCED or CANCELLED items older than the specified duration/timestamp.
     *
     * @param olderThanMillis If <= 0, purges all completed items.
     *                        If > 1,000,000,000,000, treated as absolute epoch timestamp.
     *                        Otherwise treated as relative duration in milliseconds.
     */
    public synchronized void purgeCompleted(long olderThanMillis) {
        if (olderThanMillis <= 0) {
            syncQueueDao.purgeAllCompleted();
        } else if (olderThanMillis > 1_000_000_000_000L) {
            syncQueueDao.purgeCompleted(olderThanMillis);
        } else {
            long cutoff = System.currentTimeMillis() - olderThanMillis;
            syncQueueDao.purgeCompleted(cutoff);
        }
    }

    /**
     * Returns the count of pending items in the queue.
     *
     * @return Pending operations count.
     */
    public int getPendingCount() {
        return syncQueueDao.getPendingCount();
    }

    /**
     * Retrieves an item by its ID.
     *
     * @param id The sync queue item ID.
     * @return The item entity, or null if not found.
     */
    public SyncQueueEntity getItem(String id) {
        if (id == null) return null;
        return syncQueueDao.getItemById(id);
    }

    /**
     * Resets a failed or conflicting item back to PENDING status for immediate retry.
     *
     * @param id The sync queue item ID.
     */
    public synchronized void retry(String id) {
        if (id == null) return;
        SyncQueueEntity item = syncQueueDao.getItemById(id);
        if (item != null) {
            item.setStatus(SyncStatus.PENDING.name());
            item.setTimestampMillis(System.currentTimeMillis());
            syncQueueDao.update(item);
        }
    }

    /**
     * Deletes an item by ID.
     *
     * @param id The sync queue item ID.
     */
    public synchronized void delete(String id) {
        if (id == null) return;
        syncQueueDao.deleteById(id);
    }

    /**
     * Clears all items in the sync queue.
     */
    public synchronized void clearAll() {
        syncQueueDao.deleteAll();
    }
}
