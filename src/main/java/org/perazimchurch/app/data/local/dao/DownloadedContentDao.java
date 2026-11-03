package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.perazimchurch.app.data.local.entity.DownloadedContentEntity;

import java.util.List;

@Dao
public interface DownloadedContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadedContentEntity content);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DownloadedContentEntity> contents);

    @Update
    void update(DownloadedContentEntity content);

    @Delete
    void delete(DownloadedContentEntity content);

    @Query("SELECT * FROM downloaded_content WHERE id = :id LIMIT 1")
    DownloadedContentEntity getContentById(String id);

    @Query("SELECT * FROM downloaded_content WHERE contentType = :contentType AND contentId = :contentId LIMIT 1")
    DownloadedContentEntity getContentByTypeAndId(String contentType, String contentId);

    @Query("SELECT * FROM downloaded_content ORDER BY downloadMillis DESC")
    List<DownloadedContentEntity> getAllDownloaded();

    @Query("DELETE FROM downloaded_content WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM downloaded_content")
    void deleteAll();
}
