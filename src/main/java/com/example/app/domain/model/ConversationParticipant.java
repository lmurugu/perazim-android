package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a participant in a Conversation.
 * Compliant with Guidebook §29.
 */
public class ConversationParticipant implements Serializable {

    private String id;
    private String conversationId;
    private String userId;
    private String role; // MEMBER, ADMIN, MODERATOR
    private long joinedAt;
    private long lastReadTimestamp;

    public ConversationParticipant() {
    }

    public ConversationParticipant(String id, String conversationId, String userId,
                                   String role, long joinedAt, long lastReadTimestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.lastReadTimestamp = lastReadTimestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public long getLastReadTimestamp() { return lastReadTimestamp; }
    public void setLastReadTimestamp(long lastReadTimestamp) { this.lastReadTimestamp = lastReadTimestamp; }
}
