package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "post_comments".
 */
@Entity(tableName = "post_comments")
public class CommentEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String postId;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private long createdMillis;

    public CommentEntity(@NonNull String id, String postId, String authorId,
                         String authorName, String authorAvatar, String content,
                         long createdMillis) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar != null ? authorAvatar : "";
        this.content = content;
        this.createdMillis = createdMillis;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }
}
