package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.SyncMetadataEntity;

import java.util.List;

@Dao
public interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SyncMetadataEntity metadata);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SyncMetadataEntity> metadatas);

    @Update
    void update(SyncMetadataEntity metadata);

    @Delete
    void delete(SyncMetadataEntity metadata);

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType LIMIT 1")
    SyncMetadataEntity getMetadata(String entityType);

    @Query("SELECT * FROM sync_metadata")
    List<SyncMetadataEntity> getAllMetadata();

    @Query("DELETE FROM sync_metadata WHERE entityType = :entityType")
    void deleteByEntityType(String entityType);

    @Query("DELETE FROM sync_metadata")
    void deleteAll();
}
