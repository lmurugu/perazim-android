package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Room Entity for table "group_members".
 */
@Entity(tableName = "group_members", primaryKeys = {"groupId", "userId"})
public class GroupMemberEntity {

    @NonNull
    private String groupId;
    @NonNull
    private String userId;
    private String role; // LEADER, MEMBER
    private String name;
    private String avatarUrl;
    private String campusId;
    private long joinedMillis;

    public GroupMemberEntity(@NonNull String groupId, @NonNull String userId, String role,
                             String name, String avatarUrl, String campusId, long joinedMillis) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role != null ? role : "MEMBER";
        this.name = name != null ? name : "";
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
        this.campusId = campusId != null ? campusId : "";
        this.joinedMillis = joinedMillis;
    }

    @NonNull
    public String getGroupId() { return groupId; }
    public void setGroupId(@NonNull String groupId) { this.groupId = groupId; }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public long getJoinedMillis() { return joinedMillis; }
    public void setJoinedMillis(long joinedMillis) { this.joinedMillis = joinedMillis; }
}
