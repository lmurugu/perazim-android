package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.ConnectionEntity;

import java.util.List;

@Dao
public interface ConnectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConnectionEntity connection);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ConnectionEntity> connections);

    @Update
    void update(ConnectionEntity connection);

    @Delete
    void delete(ConnectionEntity connection);

    @Query("SELECT * FROM connections WHERE id = :id LIMIT 1")
    ConnectionEntity getConnectionById(String id);

    @Query("SELECT * FROM connections WHERE userId = :userId ORDER BY createdMillis DESC")
    List<ConnectionEntity> getConnectionsForUser(String userId);

    @Query("SELECT * FROM connections WHERE (userId = :userId OR peerId = :userId) AND (status = 'ACCEPTED' OR status IS NULL) ORDER BY createdMillis DESC")
    List<ConnectionEntity> getAcceptedConnections(String userId);

    @Query("SELECT * FROM connections WHERE (peerId = :userId OR userId = :userId) AND status = 'PENDING' ORDER BY createdMillis DESC")
    List<ConnectionEntity> getPendingRequests(String userId);

    @Query("SELECT * FROM connections WHERE (userId = :userId AND peerId = :peerId) OR (userId = :peerId AND peerId = :userId) LIMIT 1")
    ConnectionEntity getConnectionBetween(String userId, String peerId);

    @Query("SELECT * FROM connections WHERE ((userId = :userId AND peerId = :peerId) OR (userId = :peerId AND peerId = :userId)) AND status = 'BLOCKED' LIMIT 1")
    ConnectionEntity getBlockedConnection(String userId, String peerId);

    @Query("DELETE FROM connections WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM connections")
    void deleteAll();
}
