package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Direct or Group Fellowship Conversation.
 * Compliant with Guidebook §28.
 */
public class Conversation implements Serializable {

    private String id;
    private String title;
    private boolean isGroup;
    private String lastMessageSnippet;
    private long lastMessageTimestamp;
    private int unreadCount;
    private long createdAt;

    public Conversation() {
    }

    public Conversation(String id, String title, boolean isGroup, String lastMessageSnippet,
                        long lastMessageTimestamp, int unreadCount, long createdAt) {
        this.id = id;
        this.title = title;
        this.isGroup = isGroup;
        this.lastMessageSnippet = lastMessageSnippet;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    public String getLastMessageSnippet() { return lastMessageSnippet; }
    public void setLastMessageSnippet(String lastMessageSnippet) { this.lastMessageSnippet = lastMessageSnippet; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
