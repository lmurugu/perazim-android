package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Church Event / Service.
 * Compliant with Guidebook §24.
 */
public class Event implements Serializable {

    private String id;
    private String title;
    private String description;
    private String dateText;
    private long timestamp;
    private String location;
    private String campusId;
    private String category;
    private String bannerUrl;

    public Event() {
    }

    public Event(String id, String title, String description, String dateText,
                 long timestamp, String location, String campusId,
                 String category, String bannerUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateText = dateText;
        this.timestamp = timestamp;
        this.location = location;
        this.campusId = campusId;
        this.category = category;
        this.bannerUrl = bannerUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateText() { return dateText; }
    public void setDateText(String dateText) { this.dateText = dateText; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
}
