package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "prayers".
 */
@Entity(tableName = "prayers")
public class PrayerEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String authorName;
    private String title;
    private String content;
    private boolean isAnonymous;
    private boolean isAnswered;
    private int prayerCount;
    private long createdMillis;

    public PrayerEntity(@NonNull String id, String userId, String authorName,
                        String title, String content, boolean isAnonymous,
                        boolean isAnswered, int prayerCount, long createdMillis) {
        this.id = id;
        this.userId = userId;
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.isAnswered = isAnswered;
        this.prayerCount = prayerCount;
        this.createdMillis = createdMillis;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public boolean isAnswered() { return isAnswered; }
    public void setAnswered(boolean answered) { isAnswered = answered; }

    public int getPrayerCount() { return prayerCount; }
    public void setPrayerCount(int prayerCount) { this.prayerCount = prayerCount; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }
}
