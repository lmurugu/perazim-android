package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.perazimchurch.app.data.local.entity.SyncQueueEntity;

import java.util.List;

@Dao
public interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SyncQueueEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SyncQueueEntity> items);

    @Update
    void update(SyncQueueEntity item);

    @Delete
    void delete(SyncQueueEntity item);

    @Query("SELECT * FROM sync_queue WHERE id = :id LIMIT 1")
    SyncQueueEntity getItemById(String id);

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestampMillis ASC")
    List<SyncQueueEntity> getPendingItems();

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY timestampMillis ASC")
    List<SyncQueueEntity> getFailedItems();

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    int getPendingCount();

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestampMillis ASC LIMIT :limit")
    List<SyncQueueEntity> getPendingOperations(int limit);

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId AND operationType = :operationType AND status IN ('PENDING', 'SYNCING') LIMIT 1")
    SyncQueueEntity findPendingOrSyncing(String entityType, String entityId, String operationType);

    @Query("DELETE FROM sync_queue WHERE status IN ('SYNCED', 'CANCELLED') AND timestampMillis <= :olderThanMillis")
    void purgeCompleted(long olderThanMillis);

    @Query("DELETE FROM sync_queue WHERE status IN ('SYNCED', 'CANCELLED')")
    void purgeAllCompleted();

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM sync_queue")
    void deleteAll();
}
