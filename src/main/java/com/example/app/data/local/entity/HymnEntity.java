package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
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

    public HymnEntity(@NonNull String id, int number, String title, String lyrics,
                      String chords, String keySignature, String timeSignature,
                      String category, boolean isFavorite) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.lyrics = lyrics;
        this.chords = chords;
        this.keySignature = keySignature;
        this.timeSignature = timeSignature;
        this.category = category;
        this.isFavorite = isFavorite;
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
}
