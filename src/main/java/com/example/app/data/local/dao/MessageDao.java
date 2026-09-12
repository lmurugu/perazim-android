package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MessageEntity> messages);

    @Update
    void update(MessageEntity message);

    @Delete
    void delete(MessageEntity message);

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    MessageEntity getMessageById(String id);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentMillis ASC")
    List<MessageEntity> getMessagesForConversation(String conversationId);

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteByConversationId(String conversationId);

    @Query("DELETE FROM messages")
    void deleteAll();
}
