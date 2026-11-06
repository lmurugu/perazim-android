package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "community_groups".
 */
@Entity(tableName = "community_groups")
public class GroupEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private String createdBy;
    private boolean isActive;
    private long createdMillis;
    private int memberCount;
    private boolean isMember;

    public GroupEntity(@NonNull String id, String name, String description,
                       String createdBy, boolean isActive, long createdMillis,
                       int memberCount, boolean isMember) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.createdBy = createdBy;
        this.isActive = isActive;
        this.createdMillis = createdMillis;
        this.memberCount = memberCount;
        this.isMember = isMember;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public boolean isMember() { return isMember; }
    public void setMember(boolean member) { isMember = member; }
}
