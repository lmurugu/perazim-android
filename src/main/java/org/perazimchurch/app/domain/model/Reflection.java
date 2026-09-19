package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Daily Devotional Reflection (Glorify / Baal-Perazim pattern).
 * Compliant with Guidebook §27.
 */
public class Reflection implements Serializable {

    private String id;
    private String title;
    private String scriptureReference;
    private String passageText;
    private String commentary;
    private String prayerPrompt;
    private String author;
    private String date;
    private boolean isCompleted;

    public Reflection() {
    }

    public Reflection(String id, String title, String scriptureReference,
                      String passageText, String commentary, String prayerPrompt,
                      String author, String date, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.scriptureReference = scriptureReference;
        this.passageText = passageText;
        this.commentary = commentary;
        this.prayerPrompt = prayerPrompt;
        this.author = author;
        this.date = date;
        this.isCompleted = isCompleted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getScriptureReference() { return scriptureReference; }
    public void setScriptureReference(String scriptureReference) { this.scriptureReference = scriptureReference; }

    public String getPassageText() { return passageText; }
    public void setPassageText(String passageText) { this.passageText = passageText; }

    public String getCommentary() { return commentary; }
    public void setCommentary(String commentary) { this.commentary = commentary; }

    public String getPrayerPrompt() { return prayerPrompt; }
    public void setPrayerPrompt(String prayerPrompt) { this.prayerPrompt = prayerPrompt; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
