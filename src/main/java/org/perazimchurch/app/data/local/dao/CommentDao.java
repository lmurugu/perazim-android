package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.perazimchurch.app.data.local.entity.CommentEntity;

import java.util.List;

@Dao
public interface CommentDao {

    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY createdMillis ASC")
    List<CommentEntity> getCommentsForPost(String postId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CommentEntity comment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CommentEntity> comments);

    @Query("DELETE FROM post_comments WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM post_comments WHERE postId = :postId")
    void deleteByPostId(String postId);
}
