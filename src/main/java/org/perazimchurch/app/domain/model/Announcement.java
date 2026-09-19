package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Church Bulletin / Announcement.
 * Compliant with Guidebook §25.
 */
public class Announcement implements Serializable {

    private String id;
    private String title;
    private String body;
    private String author;
    private long timestamp;
    private String priority; // LOW, NORMAL, URGENT
    private boolean isRead;

    public Announcement() {
    }

    public Announcement(String id, String title, String body, String author,
                        long timestamp, String priority, boolean isRead) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.author = author;
        this.timestamp = timestamp;
        this.priority = priority;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
