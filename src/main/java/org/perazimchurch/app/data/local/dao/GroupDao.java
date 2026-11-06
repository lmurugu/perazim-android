package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.perazimchurch.app.data.local.entity.GroupEntity;
import org.perazimchurch.app.data.local.entity.GroupMemberEntity;

import java.util.List;

@Dao
public interface GroupDao {

    @Query("SELECT * FROM community_groups WHERE isActive = 1 ORDER BY createdMillis ASC")
    List<GroupEntity> getAllGroups();

    @Query("SELECT * FROM community_groups WHERE id = :id LIMIT 1")
    GroupEntity getGroupById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroup(GroupEntity group);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllGroups(List<GroupEntity> groups);

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedMillis ASC")
    List<GroupMemberEntity> getGroupMembers(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMember(GroupMemberEntity member);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllMembers(List<GroupMemberEntity> members);

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    void removeMember(String groupId, String userId);

    @Query("DELETE FROM community_groups WHERE id = :id")
    void deleteGroup(String id);
}
