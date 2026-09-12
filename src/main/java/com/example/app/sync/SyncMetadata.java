package com.example.app.sync;

import java.io.Serializable;

/**
 * Sync metadata tracking entity synchronization states, cursors, and timestamps.
 * Compliant with Guidebook §43.
 */
public class SyncMetadata implements Serializable {

    private String entityType;
    private long lastSyncTimestamp;
    private String lastSyncCursor;
    private int pendingChangesCount;
    private boolean isSyncing;

    public SyncMetadata() {
    }

    public SyncMetadata(String entityType, long lastSyncTimestamp, String lastSyncCursor,
                        int pendingChangesCount, boolean isSyncing) {
        this.entityType = entityType;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.lastSyncCursor = lastSyncCursor;
        this.pendingChangesCount = pendingChangesCount;
        this.isSyncing = isSyncing;
    }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(long lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }

    public String getLastSyncCursor() { return lastSyncCursor; }
    public void setLastSyncCursor(String lastSyncCursor) { this.lastSyncCursor = lastSyncCursor; }

    public int getPendingChangesCount() { return pendingChangesCount; }
    public void setPendingChangesCount(int pendingChangesCount) { this.pendingChangesCount = pendingChangesCount; }

    public boolean isSyncing() { return isSyncing; }
    public void setSyncing(boolean syncing) { isSyncing = syncing; }
}
