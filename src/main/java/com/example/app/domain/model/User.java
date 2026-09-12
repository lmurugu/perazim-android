package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Perazim Community Member / User.
 * Compliant with Guidebook §17.
 */
public class User implements Serializable {

    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String campusId;
    private String role;
    private int streakCount;
    private int spiritualXp;
    private int gracePoints;
    private boolean streakFrozen;
    private long createdAt;
    private long updatedAt;

    public User() {
    }

    public User(String id, String fullName, String email, String phoneNumber, String campusId,
                String role, int streakCount, int spiritualXp, int gracePoints,
                boolean streakFrozen, long createdAt, long updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.campusId = campusId;
        this.role = role;
        this.streakCount = streakCount;
        this.spiritualXp = spiritualXp;
        this.gracePoints = gracePoints;
        this.streakFrozen = streakFrozen;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getStreakCount() { return streakCount; }
    public void setStreakCount(int streakCount) { this.streakCount = streakCount; }

    public int getSpiritualXp() { return spiritualXp; }
    public void setSpiritualXp(int spiritualXp) { this.spiritualXp = spiritualXp; }

    public int getGracePoints() { return gracePoints; }
    public void setGracePoints(int gracePoints) { this.gracePoints = gracePoints; }

    public boolean isStreakFrozen() { return streakFrozen; }
    public void setStreakFrozen(boolean streakFrozen) { this.streakFrozen = streakFrozen; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
