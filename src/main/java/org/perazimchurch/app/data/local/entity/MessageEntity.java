package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "messages".
 */
@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String content;
    private long sentMillis;
    private String deliveryStatus;
    private String syncStatus;

    public MessageEntity(@NonNull String id, String conversationId, String senderId,
                         String senderName, String content, long sentMillis,
                         String deliveryStatus, String syncStatus) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.sentMillis = sentMillis;
        this.deliveryStatus = deliveryStatus;
        this.syncStatus = syncStatus;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getSentMillis() { return sentMillis; }
    public void setSentMillis(long sentMillis) { this.sentMillis = sentMillis; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
