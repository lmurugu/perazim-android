package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Hymn, including lyrics, author, and chord chart.
 * Compliant with Guidebook §21.
 */
public class Hymn implements Serializable {

    private String id;
    private int number;
    private String title;
    private String author;
    private String key;
    private int tempoBpm;
    private String chords;
    private String lyrics;
    private String category;
    private boolean isFavorite;

    public Hymn() {
    }

    public Hymn(String id, int number, String title, String author, String key,
                int tempoBpm, String chords, String lyrics, String category, boolean isFavorite) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.author = author;
        this.key = key;
        this.tempoBpm = tempoBpm;
        this.chords = chords;
        this.lyrics = lyrics;
        this.category = category;
        this.isFavorite = isFavorite;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public int getTempoBpm() { return tempoBpm; }
    public void setTempoBpm(int tempoBpm) { this.tempoBpm = tempoBpm; }

    public String getChords() { return chords; }
    public void setChords(String chords) { this.chords = chords; }

    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
