package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.ConversationEntity;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConversationEntity conversation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ConversationEntity> conversations);

    @Update
    void update(ConversationEntity conversation);

    @Delete
    void delete(ConversationEntity conversation);

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    ConversationEntity getConversationById(String id);

    @Query("SELECT * FROM conversations ORDER BY lastMessageMillis DESC")
    List<ConversationEntity> getAllConversations();

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM conversations")
    void deleteAll();
}
