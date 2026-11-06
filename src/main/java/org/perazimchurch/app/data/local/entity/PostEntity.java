package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "community_posts".
 * Supports feed post types: GENERAL, PRAYER, PRAISE, EVENT.
 */
@Entity(tableName = "community_posts")
public class PostEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String type; // GENERAL, PRAYER, PRAISE, EVENT
    private String title;
    private String content;
    private String mediaUrl;
    private String groupId;
    private boolean isPinned;
    private boolean isAnswered;
    private Long eventDate;
    private String eventLocation;
    private int amenCount;
    private int commentCount;
    private boolean userHasAmened;
    private String userRsvpStatus;
    private long createdMillis;
    private long updatedMillis;

    public PostEntity(@NonNull String id, String authorId, String authorName, String authorAvatar,
                      String type, String title, String content, String mediaUrl, String groupId,
                      boolean isPinned, boolean isAnswered, Long eventDate, String eventLocation,
                      int amenCount, int commentCount, boolean userHasAmened, String userRsvpStatus,
                      long createdMillis, long updatedMillis) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar != null ? authorAvatar : "";
        this.type = type;
        this.title = title;
        this.content = content;
        this.mediaUrl = mediaUrl != null ? mediaUrl : "";
        this.groupId = groupId;
        this.isPinned = isPinned;
        this.isAnswered = isAnswered;
        this.eventDate = eventDate;
        this.eventLocation = eventLocation != null ? eventLocation : "";
        this.amenCount = amenCount;
        this.commentCount = commentCount;
        this.userHasAmened = userHasAmened;
        this.userRsvpStatus = userRsvpStatus;
        this.createdMillis = createdMillis;
        this.updatedMillis = updatedMillis;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isAnswered() { return isAnswered; }
    public void setAnswered(boolean answered) { isAnswered = answered; }

    public Long getEventDate() { return eventDate; }
    public void setEventDate(Long eventDate) { this.eventDate = eventDate; }

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    public int getAmenCount() { return amenCount; }
    public void setAmenCount(int amenCount) { this.amenCount = amenCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isUserHasAmened() { return userHasAmened; }
    public void setUserHasAmened(boolean userHasAmened) { this.userHasAmened = userHasAmened; }

    public String getUserRsvpStatus() { return userRsvpStatus; }
    public void setUserRsvpStatus(String userRsvpStatus) { this.userRsvpStatus = userRsvpStatus; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }

    public long getUpdatedMillis() { return updatedMillis; }
    public void setUpdatedMillis(long updatedMillis) { this.updatedMillis = updatedMillis; }
}
