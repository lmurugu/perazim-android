package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "users".
 * Persists member identity, account details, and gamification state (streak, XP, grace points).
 */
@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String campusId;
    private String avatarUrl;
    private long createdAt;
    private long updatedAt;
    private boolean isActive;

    private int streakCount;
    private int spiritualXp;
    private int gracePoints;
    private boolean streakFrozen;

    public UserEntity(@NonNull String id, String name, String email, String phone, String role,
                      String campusId, String avatarUrl, long createdAt, long updatedAt, boolean isActive,
                      int streakCount, int spiritualXp, int gracePoints, boolean streakFrozen) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.campusId = campusId;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.streakCount = streakCount;
        this.spiritualXp = spiritualXp;
        this.gracePoints = gracePoints;
        this.streakFrozen = streakFrozen;
    }

    @Ignore
    public UserEntity(@NonNull String id, String name, String email, String phone, String role,
                      String campusId, String avatarUrl, long createdAt, long updatedAt, boolean isActive) {
        this(id, name, email, phone, role, campusId, avatarUrl, createdAt, updatedAt, isActive, 7, 100, 50, false);
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getStreakCount() { return streakCount; }
    public void setStreakCount(int streakCount) { this.streakCount = streakCount; }

    public int getSpiritualXp() { return spiritualXp; }
    public void setSpiritualXp(int spiritualXp) { this.spiritualXp = spiritualXp; }

    public int getGracePoints() { return gracePoints; }
    public void setGracePoints(int gracePoints) { this.gracePoints = gracePoints; }

    public boolean isStreakFrozen() { return streakFrozen; }
    public void setStreakFrozen(boolean streakFrozen) { this.streakFrozen = streakFrozen; }
}
