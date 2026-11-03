package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing an offline downloaded media or document asset.
 * Compliant with Guidebook §20.
 */
public class DownloadedContent implements Serializable {

    private String id;
    private String contentId;
    private String contentType; // SERMON, HYMN, DEVOTIONAL, MEDIA
    private String title;
    private String localFilePath;
    private long fileSizeBytes;
    private long downloadedAt;
    private boolean isCompleted;
    private int downloadProgressPercent;

    public DownloadedContent() {
    }

    public DownloadedContent(String id, String contentId, String contentType, String title,
                             String localFilePath, long fileSizeBytes, long downloadedAt,
                             boolean isCompleted, int downloadProgressPercent) {
        this.id = id;
        this.contentId = contentId;
        this.contentType = contentType;
        this.title = title;
        this.localFilePath = localFilePath;
        this.fileSizeBytes = fileSizeBytes;
        this.downloadedAt = downloadedAt;
        this.isCompleted = isCompleted;
        this.downloadProgressPercent = downloadProgressPercent;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public long getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(long downloadedAt) { this.downloadedAt = downloadedAt; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getDownloadProgressPercent() { return downloadProgressPercent; }
    public void setDownloadProgressPercent(int downloadProgressPercent) { this.downloadProgressPercent = downloadProgressPercent; }
}
