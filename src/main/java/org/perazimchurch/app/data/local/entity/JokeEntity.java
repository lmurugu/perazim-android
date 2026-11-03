package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "jokes".
 */
@Entity(tableName = "jokes")
public class JokeEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String text;
    private String cleanRating;
    private String category;
    private boolean active;

    public JokeEntity(@NonNull String id, String text, String cleanRating, String category, boolean active) {
        this.id = id;
        this.text = text;
        this.cleanRating = cleanRating;
        this.category = category;
        this.active = active;
    }

    @Ignore
    public JokeEntity(@NonNull String id, String text, String cleanRating, String category) {
        this(id, text, cleanRating, category, true);
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCleanRating() { return cleanRating; }
    public void setCleanRating(String cleanRating) { this.cleanRating = cleanRating; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
