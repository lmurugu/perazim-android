package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing an In-App or Push Notification.
 * Compliant with Guidebook §33.
 */
public class Notification implements Serializable {

    private String id;
    private String userId;
    private String title;
    private String message;
    private String type; // PRAYER, SERMON, EVENT, SYSTEM, ANNOUNCEMENT
    private String targetRoute;
    private boolean isRead;
    private long timestamp;

    public Notification() {
    }

    public Notification(String id, String userId, String title, String message,
                        String type, String targetRoute, boolean isRead, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.targetRoute = targetRoute;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
