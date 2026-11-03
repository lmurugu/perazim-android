package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "hymns".
 */
@Entity(tableName = "hymns")
public class HymnEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private int number;
    private String title;
    private String lyrics;
    private String chords;
    private String keySignature;
    private String timeSignature;
    private String category;
    private boolean isFavorite;
    private String writer;
    private String year;
    private int bpm;
    private String tags;
    private boolean active;

    public HymnEntity(@NonNull String id, int number, String title, String lyrics,
                      String chords, String keySignature, String timeSignature,
                      String category, boolean isFavorite, String writer, String year,
                      int bpm, String tags, boolean active) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.lyrics = lyrics;
        this.chords = chords;
        this.keySignature = keySignature;
        this.timeSignature = timeSignature;
        this.category = category;
        this.isFavorite = isFavorite;
        this.writer = writer;
        this.year = year;
        this.bpm = bpm;
        this.tags = tags;
        this.active = active;
    }

    @Ignore
    public HymnEntity(@NonNull String id, int number, String title, String lyrics,
                      String chords, String keySignature, String timeSignature,
                      String category, boolean isFavorite) {
        this(id, number, title, lyrics, chords, keySignature, timeSignature, category, isFavorite, "Traditional", "", 80, category, true);
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }

    public String getChords() { return chords; }
    public void setChords(String chords) { this.chords = chords; }

    public String getKeySignature() { return keySignature; }
    public void setKeySignature(String keySignature) { this.keySignature = keySignature; }

    public String getTimeSignature() { return timeSignature; }
    public void setTimeSignature(String timeSignature) { this.timeSignature = timeSignature; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getWriter() { return writer; }
    public void setWriter(String writer) { this.writer = writer; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
