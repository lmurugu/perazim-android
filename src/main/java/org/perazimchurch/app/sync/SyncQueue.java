package org.perazimchurch.app.sync;

import java.io.Serializable;

/**
 * Sync queue model for offline-first mutations.
 * Enqueues local mutations (prayers, giving submissions, user progress, messages)
 * when offline and dispatches them when network connectivity is restored.
 * Compliant with Guidebook §42.
 */
public class SyncQueue implements Serializable {

    public enum SyncStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        RETRY
    }

    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE
    }

    private String id;
    private String entityType; // PRAYER, GIVING, MESSAGE, USER_PROGRESS, REFLECTION
    private String entityId;
    private ActionType action;
    private String payloadJson;
    private SyncStatus status;
    private int retryCount;
    private long createdAt;
    private long lastAttemptTimestamp;
    private String errorMessage;

    public SyncQueue() {
        this.status = SyncStatus.PENDING;
        this.action = ActionType.CREATE;
    }

    public SyncQueue(String id, String entityType, String entityId,
                     ActionType action, String payloadJson, SyncStatus status,
                     int retryCount, long createdAt, long lastAttemptTimestamp,
                     String errorMessage) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action != null ? action : ActionType.CREATE;
        this.payloadJson = payloadJson;
        this.status = status != null ? status : SyncStatus.PENDING;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.lastAttemptTimestamp = lastAttemptTimestamp;
        this.errorMessage = errorMessage;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public ActionType getAction() { return action; }
    public void setAction(ActionType action) { this.action = action; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastAttemptTimestamp() { return lastAttemptTimestamp; }
    public void setLastAttemptTimestamp(long lastAttemptTimestamp) { this.lastAttemptTimestamp = lastAttemptTimestamp; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
