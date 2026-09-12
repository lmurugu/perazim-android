package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Fellowship Connection / Friendship.
 * Compliant with Guidebook §31 (includes status enum: PENDING, ACCEPTED, BLOCKED, REMOVED).
 */
public class Connection implements Serializable {

    public enum Status {
        PENDING,
        ACCEPTED,
        BLOCKED,
        REMOVED
    }

    private String id;
    private String requesterId;
    private String recipientId;
    private Status status;
    private long requestedAt;
    private long respondedAt;

    public Connection() {
        this.status = Status.PENDING;
    }

    public Connection(String id, String requesterId, String recipientId,
                      Status status, long requestedAt, long respondedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.status = status != null ? status : Status.PENDING;
        this.requestedAt = requestedAt;
        this.respondedAt = respondedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

    public long getRespondedAt() { return respondedAt; }
    public void setRespondedAt(long respondedAt) { this.respondedAt = respondedAt; }
}
