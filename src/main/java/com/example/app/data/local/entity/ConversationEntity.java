package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "conversations".
 */
@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String type;
    private long createdAt;
    private long updatedAt;
    private String lastMessageText;
    private long lastMessageMillis;

    public ConversationEntity(@NonNull String id, String title, String type,
                              long createdAt, long updatedAt,
                              String lastMessageText, long lastMessageMillis) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastMessageText = lastMessageText;
        this.lastMessageMillis = lastMessageMillis;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public long getLastMessageMillis() { return lastMessageMillis; }
    public void setLastMessageMillis(long lastMessageMillis) { this.lastMessageMillis = lastMessageMillis; }
}
