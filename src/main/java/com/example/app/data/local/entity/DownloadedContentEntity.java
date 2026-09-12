package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "downloaded_content".
 */
@Entity(tableName = "downloaded_content")
public class DownloadedContentEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String contentType;
    private String contentId;
    private String localUri;
    private long fileSizeBytes;
    private long downloadMillis;
    private String status;

    public DownloadedContentEntity(@NonNull String id, String contentType, String contentId,
                                   String localUri, long fileSizeBytes, long downloadMillis,
                                   String status) {
        this.id = id;
        this.contentType = contentType;
        this.contentId = contentId;
        this.localUri = localUri;
        this.fileSizeBytes = fileSizeBytes;
        this.downloadMillis = downloadMillis;
        this.status = status;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getLocalUri() { return localUri; }
    public void setLocalUri(String localUri) { this.localUri = localUri; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public long getDownloadMillis() { return downloadMillis; }
    public void setDownloadMillis(long downloadMillis) { this.downloadMillis = downloadMillis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
