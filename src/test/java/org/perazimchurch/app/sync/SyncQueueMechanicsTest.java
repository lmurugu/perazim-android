package org.perazimchurch.app.sync;

import org.junit.Before;
import org.junit.Test;
import org.perazimchurch.app.data.local.dao.SyncQueueDao;
import org.perazimchurch.app.data.local.entity.SyncQueueEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying REQ-08 & REQ-09 Sync Queue Mechanics:
 * - Recovery of orphaned 'SYNCING' operations back to 'PENDING'
 * - Permanent failure detection (FORBIDDEN, UNSUPPORTED_OPERATION, AUTH_REQUIRED, etc.)
 * - Preservation of user drafts/payloads on permanent failure
 * - Transient failure retry with exponential backoff
 */
public class SyncQueueMechanicsTest {

    private InMemorySyncQueueDao dummyDao;
    private SyncQueueManager queueManager;

    @Before
    public void setUp() {
        dummyDao = new InMemorySyncQueueDao();
        queueManager = new SyncQueueManager(dummyDao);
    }

    @Test
    public void testRecoverOrphanedSyncingOperations() {
        // Enqueue three items: one PENDING, two stuck in SYNCING from a crash
        SyncQueueEntity item1 = new SyncQueueEntity("op_1", "CREATE", "PRAYER", "prayer_1", "{\"title\":\"P1\"}", 1000L, "SYNCING", 0, null);
        SyncQueueEntity item2 = new SyncQueueEntity("op_2", "UPDATE", "PRAYER_AMEN", "prayer_2", "{}", 1001L, "SYNCING", 0, null);
        SyncQueueEntity item3 = new SyncQueueEntity("op_3", "CREATE", "PRAYER", "prayer_3", "{\"title\":\"P3\"}", 1002L, "PENDING", 0, null);
        SyncQueueEntity item4 = new SyncQueueEntity("op_4", "CREATE", "PRAYER", "prayer_4", "{\"title\":\"P4\"}", 1003L, "SYNCED", 0, null);

        dummyDao.insert(item1);
        dummyDao.insert(item2);
        dummyDao.insert(item3);
        dummyDao.insert(item4);

        assertEquals(2, dummyDao.getSyncingItems().size());

        int recovered = queueManager.recoverOrphanedSyncing();
        assertEquals(2, recovered);

        // Verify that op_1 and op_2 are now PENDING
        assertEquals("PENDING", dummyDao.getItemById("op_1").getStatus());
        assertEquals("PENDING", dummyDao.getItemById("op_2").getStatus());
        assertEquals("PENDING", dummyDao.getItemById("op_3").getStatus());
        assertEquals("SYNCED", dummyDao.getItemById("op_4").getStatus());
        assertEquals(0, dummyDao.getSyncingItems().size());
    }

    @Test
    public void testPermanentFailureClassification() {
        // HTTP status codes
        assertTrue(SyncQueueManager.isPermanentFailure(null, 400));
        assertTrue(SyncQueueManager.isPermanentFailure(null, 401));
        assertTrue(SyncQueueManager.isPermanentFailure(null, 403));
        assertTrue(SyncQueueManager.isPermanentFailure(null, 404));
        assertTrue(SyncQueueManager.isPermanentFailure(null, 422));

        assertFalse(SyncQueueManager.isPermanentFailure(null, 500));
        assertFalse(SyncQueueManager.isPermanentFailure(null, 502));
        assertFalse(SyncQueueManager.isPermanentFailure(null, 503));
        assertFalse(SyncQueueManager.isPermanentFailure(null, -1));

        // Machine-readable reason codes
        assertTrue(SyncQueueManager.isPermanentFailure("FORBIDDEN", -1));
        assertTrue(SyncQueueManager.isPermanentFailure("CONFLICT_MISMATCH", -1));
        assertTrue(SyncQueueManager.isPermanentFailure("UNSUPPORTED_OPERATION", -1));
        assertTrue(SyncQueueManager.isPermanentFailure("AUTH_REQUIRED", -1));
        assertTrue(SyncQueueManager.isPermanentFailure("NOT_FOUND", -1));

        assertFalse(SyncQueueManager.isPermanentFailure("TIMEOUT", -1));
        assertFalse(SyncQueueManager.isPermanentFailure("NETWORK_ERROR", -1));
        assertFalse(SyncQueueManager.isPermanentFailure("SERVER_ERROR", -1));
    }

    @Test
    public void testRecordPermanentFailurePreservesUserPayload() {
        String draftPayload = "{\"title\":\"Healing Petition\",\"content\":\"Please pray for strength\",\"isAnonymous\":false}";
        String id = queueManager.enqueue("CREATE", "PRAYER", "prayer_local_123", draftPayload, "idempotency_123");

        queueManager.recordPermanentFailure(id, "FORBIDDEN", "Only registered members may broadcast globally");

        SyncQueueEntity failed = dummyDao.getItemById(id);
        assertNotNull(failed);
        assertEquals("FAILED", failed.getStatus());
        assertTrue(failed.getLastError().contains("FORBIDDEN"));
        assertTrue(failed.getLastError().contains("Only registered members"));

        // Critical: User draft/petition payload MUST NOT be discarded
        assertEquals(draftPayload, failed.getPayloadJson());
        assertEquals("prayer_local_123", failed.getEntityId());
        assertEquals("PRAYER", failed.getEntityType());
    }

    @Test
    public void testTransientFailureWithExponentialBackoff() {
        String id = queueManager.enqueue("CREATE", "PRAYER", "prayer_t_1", "{}", null);
        long initialTs = dummyDao.getItemById(id).getTimestampMillis();

        // 1st transient failure
        queueManager.recordFailure(id, "HTTP 503: Service Unavailable", true);
        SyncQueueEntity after1 = dummyDao.getItemById(id);
        assertEquals("PENDING", after1.getStatus());
        assertEquals(1, after1.getRetryCount());
        assertTrue("Backoff delay should advance timestamp", after1.getTimestampMillis() > initialTs);

        // 2nd transient failure
        queueManager.recordFailure(id, "HTTP 503: Service Unavailable", true);
        SyncQueueEntity after2 = dummyDao.getItemById(id);
        assertEquals("PENDING", after2.getStatus());
        assertEquals(2, after2.getRetryCount());

        // 3rd transient failure (reaching MAX_RETRIES = 3)
        queueManager.recordFailure(id, "HTTP 503: Service Unavailable", true);
        SyncQueueEntity after3 = dummyDao.getItemById(id);
        assertEquals("FAILED", after3.getStatus());
        assertEquals(3, after3.getRetryCount());
    }

    /**
     * In-memory implementation of SyncQueueDao for unit testing.
     */
    private static class InMemorySyncQueueDao implements SyncQueueDao {
        private final Map<String, SyncQueueEntity> storage = new HashMap<>();

        @Override
        public void insert(SyncQueueEntity item) {
            storage.put(item.getId(), copy(item));
        }

        @Override
        public void insertAll(List<SyncQueueEntity> items) {
            for (SyncQueueEntity item : items) {
                insert(item);
            }
        }

        @Override
        public void update(SyncQueueEntity item) {
            storage.put(item.getId(), copy(item));
        }

        @Override
        public void delete(SyncQueueEntity item) {
            storage.remove(item.getId());
        }

        @Override
        public SyncQueueEntity getItemById(String id) {
            SyncQueueEntity item = storage.get(id);
            return item != null ? copy(item) : null;
        }

        @Override
        public List<SyncQueueEntity> getPendingItems() {
            List<SyncQueueEntity> result = new ArrayList<>();
            for (SyncQueueEntity item : storage.values()) {
                if ("PENDING".equals(item.getStatus())) {
                    result.add(copy(item));
                }
            }
            return result;
        }

        @Override
        public List<SyncQueueEntity> getFailedItems() {
            List<SyncQueueEntity> result = new ArrayList<>();
            for (SyncQueueEntity item : storage.values()) {
                if ("FAILED".equals(item.getStatus()) || "REJECTED".equals(item.getStatus())) {
                    result.add(copy(item));
                }
            }
            return result;
        }

        @Override
        public List<SyncQueueEntity> getSyncingItems() {
            List<SyncQueueEntity> result = new ArrayList<>();
            for (SyncQueueEntity item : storage.values()) {
                if ("SYNCING".equals(item.getStatus())) {
                    result.add(copy(item));
                }
            }
            return result;
        }

        @Override
        public int recoverOrphanedSyncing() {
            int count = 0;
            for (SyncQueueEntity item : storage.values()) {
                if ("SYNCING".equals(item.getStatus())) {
                    item.setStatus("PENDING");
                    count++;
                }
            }
            return count;
        }

        @Override
        public int getPendingCount() {
            return getPendingItems().size();
        }

        @Override
        public List<SyncQueueEntity> getPendingOperations(int limit) {
            List<SyncQueueEntity> pending = getPendingItems();
            return pending.size() > limit ? pending.subList(0, limit) : pending;
        }

        @Override
        public SyncQueueEntity findPendingOrSyncing(String entityType, String entityId, String operationType) {
            for (SyncQueueEntity item : storage.values()) {
                if (entityType.equals(item.getEntityType())
                        && entityId.equals(item.getEntityId())
                        && operationType.equals(item.getOperationType())
                        && ("PENDING".equals(item.getStatus()) || "SYNCING".equals(item.getStatus()))) {
                    return copy(item);
                }
            }
            return null;
        }

        @Override
        public void purgeCompleted(long olderThanMillis) {
            List<String> toRemove = new ArrayList<>();
            for (SyncQueueEntity item : storage.values()) {
                if (("SYNCED".equals(item.getStatus()) || "CANCELLED".equals(item.getStatus()))
                        && item.getTimestampMillis() <= olderThanMillis) {
                    toRemove.add(item.getId());
                }
            }
            for (String id : toRemove) storage.remove(id);
        }

        @Override
        public void purgeAllCompleted() {
            List<String> toRemove = new ArrayList<>();
            for (SyncQueueEntity item : storage.values()) {
                if ("SYNCED".equals(item.getStatus()) || "CANCELLED".equals(item.getStatus())) {
                    toRemove.add(item.getId());
                }
            }
            for (String id : toRemove) storage.remove(id);
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }

        private SyncQueueEntity copy(SyncQueueEntity original) {
            return new SyncQueueEntity(
                    original.getId(),
                    original.getOperationType(),
                    original.getEntityType(),
                    original.getEntityId(),
                    original.getPayloadJson(),
                    original.getTimestampMillis(),
                    original.getStatus(),
                    original.getRetryCount(),
                    original.getLastError()
            );
        }
    }
}
