package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Holy Scripture Translation.
 * Compliant with Guidebook §34.
 */
public class BibleTranslation implements Serializable {

    private String id;
    private String name;
    private String language;
    private boolean isDownloaded;

    public BibleTranslation() {
    }

    public BibleTranslation(String id, String name, String language, boolean isDownloaded) {
        this.id = id;
        this.name = name;
        this.language = language;
        this.isDownloaded = isDownloaded;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }
}
