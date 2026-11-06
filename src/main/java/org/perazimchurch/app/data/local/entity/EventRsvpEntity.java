package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Room Entity for table "event_rsvps".
 */
@Entity(tableName = "event_rsvps", primaryKeys = {"postId", "userId"})
public class EventRsvpEntity {

    @NonNull
    private String postId;
    @NonNull
    private String userId;
    private String status; // ATTENDING, DECLINED
    private long updatedMillis;

    public EventRsvpEntity(@NonNull String postId, @NonNull String userId,
                           String status, long updatedMillis) {
        this.postId = postId;
        this.userId = userId;
        this.status = status != null ? status : "ATTENDING";
        this.updatedMillis = updatedMillis;
    }

    @NonNull
    public String getPostId() { return postId; }
    public void setPostId(@NonNull String postId) { this.postId = postId; }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getUpdatedMillis() { return updatedMillis; }
    public void setUpdatedMillis(long updatedMillis) { this.updatedMillis = updatedMillis; }
}
