package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "announcements".
 */
@Entity(tableName = "announcements")
public class AnnouncementEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String body;
    private String priority;
    private long publishedMillis;
    private long expiresMillis;
    private String campusId;

    public AnnouncementEntity(@NonNull String id, String title, String body, String priority,
                              long publishedMillis, long expiresMillis, String campusId) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.priority = priority;
        this.publishedMillis = publishedMillis;
        this.expiresMillis = expiresMillis;
        this.campusId = campusId;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public long getPublishedMillis() { return publishedMillis; }
    public void setPublishedMillis(long publishedMillis) { this.publishedMillis = publishedMillis; }

    public long getExpiresMillis() { return expiresMillis; }
    public void setExpiresMillis(long expiresMillis) { this.expiresMillis = expiresMillis; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }
}
