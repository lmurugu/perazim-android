package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "sync_queue".
 */
@Entity(tableName = "sync_queue", indices = {@Index(value = {"id"}, unique = true)})
public class SyncQueueEntity {

    @PrimaryKey(autoGenerate = true)
    private long sequenceNumber;
    @NonNull
    private String id;
    private String operationType;
    private String entityType;
    private String entityId;
    private String payloadJson;
    private long timestampMillis;
    private long nextAttemptAt;
    private String status;
    private int retryCount;
    private String lastError;

    @Ignore
    public SyncQueueEntity(@NonNull String id, String operationType, String entityType,
                           String entityId, String payloadJson, long timestampMillis,
                           String status, int retryCount, String lastError) {
        this(0, id, operationType, entityType, entityId, payloadJson, timestampMillis, 0,
                status, retryCount, lastError);
    }

    public SyncQueueEntity(long sequenceNumber, @NonNull String id, String operationType, String entityType,
                           String entityId, String payloadJson, long timestampMillis, long nextAttemptAt,
                           String status, int retryCount, String lastError) {
        this.sequenceNumber = sequenceNumber;
        this.id = id;
        this.operationType = operationType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payloadJson = payloadJson;
        this.timestampMillis = timestampMillis;
        this.nextAttemptAt = nextAttemptAt;
        this.status = status;
        this.retryCount = retryCount;
        this.lastError = lastError;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public long getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(long nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
