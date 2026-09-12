package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "reflections".
 */
@Entity(tableName = "reflections")
public class ReflectionEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String title;
    private String content;
    private String passageRef;
    private long createdMillis;
    private long updatedMillis;
    private boolean isPrivate;

    public ReflectionEntity(@NonNull String id, String userId, String title,
                            String content, String passageRef, long createdMillis,
                            long updatedMillis, boolean isPrivate) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.passageRef = passageRef;
        this.createdMillis = createdMillis;
        this.updatedMillis = updatedMillis;
        this.isPrivate = isPrivate;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPassageRef() { return passageRef; }
    public void setPassageRef(String passageRef) { this.passageRef = passageRef; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }

    public long getUpdatedAtMillis() { return updatedMillis; }
    public long getUpdatedMillis() { return updatedMillis; }
    public void setUpdatedMillis(long updatedMillis) { this.updatedMillis = updatedMillis; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
}
