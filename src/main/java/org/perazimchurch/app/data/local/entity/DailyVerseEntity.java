package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "daily_verses".
 * Holds curated daily scripture references and text mapped to specific dates (YYYY-MM-DD).
 */
@Entity(tableName = "daily_verses")
public class DailyVerseEntity {

    @PrimaryKey
    @NonNull
    private String date; // ISO YYYY-MM-DD
    private String reference;
    private String text;
    private boolean active;

    public DailyVerseEntity(@NonNull String date, String reference, String text, boolean active) {
        this.date = date;
        this.reference = reference;
        this.text = text;
        this.active = active;
    }

    @Ignore
    public DailyVerseEntity(@NonNull String date, String reference, String text) {
        this(date, reference, text, true);
    }

    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
