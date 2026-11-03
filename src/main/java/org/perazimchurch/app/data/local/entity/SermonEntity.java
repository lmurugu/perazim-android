package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "sermons".
 */
@Entity(tableName = "sermons")
public class SermonEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String preacher;
    private String series;
    private String passage;
    private String audioUrl;
    private String videoUrl;
    private long dateMillis;
    private int durationSeconds;
    private String summary;
    private String notes;
    private boolean isDownloaded;
    private String localAudioPath;

    public SermonEntity(@NonNull String id, String title, String preacher, String series,
                        String passage, String audioUrl, String videoUrl, long dateMillis,
                        int durationSeconds, String summary, String notes,
                        boolean isDownloaded, String localAudioPath) {
        this.id = id;
        this.title = title;
        this.preacher = preacher;
        this.series = series;
        this.passage = passage;
        this.audioUrl = audioUrl;
        this.videoUrl = videoUrl;
        this.dateMillis = dateMillis;
        this.durationSeconds = durationSeconds;
        this.summary = summary;
        this.notes = notes;
        this.isDownloaded = isDownloaded;
        this.localAudioPath = localAudioPath;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPreacher() { return preacher; }
    public void setPreacher(String preacher) { this.preacher = preacher; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public String getPassage() { return passage; }
    public void setPassage(String passage) { this.passage = passage; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    public String getLocalAudioPath() { return localAudioPath; }
    public void setLocalAudioPath(String localAudioPath) { this.localAudioPath = localAudioPath; }
}
