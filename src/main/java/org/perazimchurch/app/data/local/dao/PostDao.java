package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.perazimchurch.app.data.local.entity.PostEntity;

import java.util.List;

@Dao
public interface PostDao {

    @Query("SELECT * FROM community_posts WHERE groupId IS NULL OR groupId = '' ORDER BY isPinned DESC, createdMillis DESC")
    List<PostEntity> getAllPublicPosts();

    @Query("SELECT * FROM community_posts WHERE (groupId IS NULL OR groupId = '') AND type = :type ORDER BY isPinned DESC, createdMillis DESC")
    List<PostEntity> getPostsByType(String type);

    @Query("SELECT * FROM community_posts WHERE groupId = :groupId ORDER BY isPinned DESC, createdMillis DESC")
    List<PostEntity> getPostsByGroup(String groupId);

    @Query("SELECT * FROM community_posts WHERE authorId = :authorId ORDER BY createdMillis DESC")
    List<PostEntity> getPostsByAuthor(String authorId);

    @Query("SELECT * FROM community_posts WHERE id = :id LIMIT 1")
    PostEntity getPostById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PostEntity post);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PostEntity> posts);

    @Update
    void update(PostEntity post);

    @Query("UPDATE community_posts SET amenCount = amenCount + 1, userHasAmened = 1 WHERE id = :postId")
    void incrementAmen(String postId);

    @Query("UPDATE community_posts SET commentCount = commentCount + 1 WHERE id = :postId")
    void incrementCommentCount(String postId);

    @Query("UPDATE community_posts SET userRsvpStatus = :status WHERE id = :postId")
    void updateRsvpStatus(String postId, String status);

    @Query("DELETE FROM community_posts WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM community_posts")
    void deleteAll();
}
