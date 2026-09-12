package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "notifications".
 */
@Entity(tableName = "notifications")
public class NotificationEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String title;
    private String body;
    private String type;
    private String actionTarget;
    private boolean isRead;
    private long createdMillis;

    public NotificationEntity(@NonNull String id, String userId, String title,
                              String body, String type, String actionTarget,
                              boolean isRead, long createdMillis) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.actionTarget = actionTarget;
        this.isRead = isRead;
        this.createdMillis = createdMillis;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getActionTarget() { return actionTarget; }
    public void setActionTarget(String actionTarget) { this.actionTarget = actionTarget; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }
}
