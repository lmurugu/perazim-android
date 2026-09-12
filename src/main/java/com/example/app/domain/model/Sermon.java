package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Sermon / Message.
 * Compliant with Guidebook §19.
 */
public class Sermon implements Serializable {

    private String id;
    private String title;
    private String preacher;
    private String scriptureReference;
    private String seriesName;
    private String audioUrl;
    private String videoUrl;
    private String thumbnailUrl;
    private long durationSeconds;
    private String datePreached;
    private String description;
    private boolean isDownloaded;
    private long playCount;

    public Sermon() {
    }

    public Sermon(String id, String title, String preacher, String scriptureReference,
                  String seriesName, String audioUrl, String videoUrl, String thumbnailUrl,
                  long durationSeconds, String datePreached, String description,
                  boolean isDownloaded, long playCount) {
        this.id = id;
        this.title = title;
        this.preacher = preacher;
        this.scriptureReference = scriptureReference;
        this.seriesName = seriesName;
        this.audioUrl = audioUrl;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.datePreached = datePreached;
        this.description = description;
        this.isDownloaded = isDownloaded;
        this.playCount = playCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPreacher() { return preacher; }
    public void setPreacher(String preacher) { this.preacher = preacher; }

    public String getScriptureReference() { return scriptureReference; }
    public void setScriptureReference(String scriptureReference) { this.scriptureReference = scriptureReference; }

    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getDatePreached() { return datePreached; }
    public void setDatePreached(String datePreached) { this.datePreached = datePreached; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    public long getPlayCount() { return playCount; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }
}
