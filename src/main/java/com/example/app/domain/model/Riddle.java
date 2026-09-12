package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Biblical Riddle.
 * Compliant with Guidebook §22.
 */
public class Riddle implements Serializable {

    private String id;
    private String question;
    private String answer;
    private String scriptureReference;
    private int xpReward;
    private boolean isAnswered;
    private String explanation;

    public Riddle() {
    }

    public Riddle(String id, String question, String answer, String scriptureReference,
                  int xpReward, boolean isAnswered, String explanation) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.scriptureReference = scriptureReference;
        this.xpReward = xpReward;
        this.isAnswered = isAnswered;
        this.explanation = explanation;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getScriptureReference() { return scriptureReference; }
    public void setScriptureReference(String scriptureReference) { this.scriptureReference = scriptureReference; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public boolean isAnswered() { return isAnswered; }
    public void setAnswered(boolean answered) { isAnswered = answered; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
