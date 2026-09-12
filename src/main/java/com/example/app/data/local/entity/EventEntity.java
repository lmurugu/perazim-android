package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "events".
 */
@Entity(tableName = "events")
public class EventEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String description;
    private long startTimeMillis;
    private long endTimeMillis;
    private String location;
    private String campusId;
    private String imageUrl;
    private String category;

    public EventEntity(@NonNull String id, String title, String description,
                       long startTimeMillis, long endTimeMillis, String location,
                       String campusId, String imageUrl, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.location = location;
        this.campusId = campusId;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long startTimeMillis) { this.startTimeMillis = startTimeMillis; }

    public long getEndTimeMillis() { return endTimeMillis; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
