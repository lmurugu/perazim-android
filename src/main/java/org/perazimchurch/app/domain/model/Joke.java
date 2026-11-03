package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing Biblical Humor / Clean Christian Joke.
 * Compliant with Guidebook §23.
 */
public class Joke implements Serializable {

    private String id;
    private String setup;
    private String punchline;
    private int xpReward;
    private String topic;

    public Joke() {
    }

    public Joke(String id, String setup, String punchline, int xpReward, String topic) {
        this.id = id;
        this.setup = setup;
        this.punchline = punchline;
        this.xpReward = xpReward;
        this.topic = topic;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSetup() { return setup; }
    public void setSetup(String setup) { this.setup = setup; }

    public String getPunchline() { return punchline; }
    public void setPunchline(String punchline) { this.punchline = punchline; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
