package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "campuses".
 */
@Entity(tableName = "campuses")
public class CampusEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String code;
    private String location;
    private String leaderName;
    private String phone;
    private String email;
    private boolean isMainCampus;

    public CampusEntity(@NonNull String id, String name, String code, String location,
                        String leaderName, String phone, String email, boolean isMainCampus) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.location = location;
        this.leaderName = leaderName;
        this.phone = phone;
        this.email = email;
        this.isMainCampus = isMainCampus;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isMainCampus() { return isMainCampus; }
    public void setMainCampus(boolean mainCampus) { isMainCampus = mainCampus; }
}
