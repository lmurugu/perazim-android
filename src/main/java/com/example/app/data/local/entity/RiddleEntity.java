package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "riddles".
 */
@Entity(tableName = "riddles")
public class RiddleEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String question;
    private String answer;
    private String explanation;
    private String scriptureRef;
    private String difficulty;

    public RiddleEntity(@NonNull String id, String question, String answer,
                       String explanation, String scriptureRef, String difficulty) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.explanation = explanation;
        this.scriptureRef = scriptureRef;
        this.difficulty = difficulty;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getScriptureRef() { return scriptureRef; }
    public void setScriptureRef(String scriptureRef) { this.scriptureRef = scriptureRef; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
