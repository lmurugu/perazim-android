package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Fellowship Chat / Direct Message.
 * Compliant with Guidebook §30 (includes client_message_id for offline-first deduplication).
 */
public class Message implements Serializable {

    private String id;
    private String clientMessageId; // client_message_id for optimistic UI & sync deduplication
    private String conversationId;
    private String senderId;
    private String senderName;
    private String content;
    private String status; // PENDING, SENT, DELIVERED, READ, FAILED
    private long timestamp;

    public Message() {
    }

    public Message(String id, String clientMessageId, String conversationId,
                   String senderId, String senderName, String content,
                   String status, long timestamp) {
        this.id = id;
        this.clientMessageId = clientMessageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
