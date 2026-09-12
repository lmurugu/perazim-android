package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "sync_metadata".
 */
@Entity(tableName = "sync_metadata")
public class SyncMetadataEntity {

    @PrimaryKey
    @NonNull
    private String entityType;
    private long lastSyncTimestamp;
    private String syncToken;
    private String status;

    public SyncMetadataEntity(@NonNull String entityType, long lastSyncTimestamp,
                              String syncToken, String status) {
        this.entityType = entityType;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.syncToken = syncToken;
        this.status = status;
    }

    @NonNull
    public String getEntityType() { return entityType; }
    public void setEntityType(@NonNull String entityType) { this.entityType = entityType; }

    public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(long lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }

    public String getSyncToken() { return syncToken; }
    public void setSyncToken(String syncToken) { this.syncToken = syncToken; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
