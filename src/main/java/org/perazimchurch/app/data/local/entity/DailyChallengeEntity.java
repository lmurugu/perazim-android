package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "daily_challenges".
 */
@Entity(tableName = "daily_challenges")
public class DailyChallengeEntity {

    @PrimaryKey
    @NonNull
    private String date; // YYYY-MM-DD
    private String riddleId;
    private String question;
    private String hint;
    private boolean isCompleted;

    public DailyChallengeEntity(@NonNull String date, String riddleId,
                                String question, String hint, boolean isCompleted) {
        this.date = date;
        this.riddleId = riddleId;
        this.question = question;
        this.hint = hint != null ? hint : "";
        this.isCompleted = isCompleted;
    }

    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }

    public String getRiddleId() { return riddleId; }
    public void setRiddleId(String riddleId) { this.riddleId = riddleId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
