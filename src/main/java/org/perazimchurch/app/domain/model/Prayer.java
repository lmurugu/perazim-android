package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Community Prayer Wall Petition or Personal Prayer.
 * Compliant with Guidebook §26.
 */
public class Prayer implements Serializable {

    public enum Visibility {
        PRIVATE,
        PUBLIC,
        PASTORAL
    }

    private String id;
    private String title;
    private String body;
    private String authorId;
    private String authorName;
    private Visibility visibility;
    private int amenCount;
    private long createdAt;
    private boolean isAnswered;
    private boolean userHasAmened;

    public Prayer() {
        this.visibility = Visibility.PUBLIC;
    }

    public Prayer(String id, String title, String body, String authorId, String authorName,
                  Visibility visibility, int amenCount, long createdAt,
                  boolean isAnswered, boolean userHasAmened) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.authorId = authorId;
        this.authorName = authorName;
        this.visibility = visibility != null ? visibility : Visibility.PUBLIC;
        this.amenCount = amenCount;
        this.createdAt = createdAt;
        this.isAnswered = isAnswered;
        this.userHasAmened = userHasAmened;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public int getAmenCount() { return amenCount; }
    public void setAmenCount(int amenCount) { this.amenCount = amenCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isAnswered() { return isAnswered; }
    public void setAnswered(boolean answered) { isAnswered = answered; }

    public boolean isUserHasAmened() { return userHasAmened; }
    public void setUserHasAmened(boolean userHasAmened) { this.userHasAmened = userHasAmened; }
}
