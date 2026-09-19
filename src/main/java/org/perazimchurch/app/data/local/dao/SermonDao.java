package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.perazimchurch.app.data.local.entity.SermonEntity;

import java.util.List;

@Dao
public interface SermonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SermonEntity sermon);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SermonEntity> sermons);

    @Update
    void update(SermonEntity sermon);

    @Delete
    void delete(SermonEntity sermon);

    @Query("SELECT * FROM sermons WHERE id = :id LIMIT 1")
    SermonEntity getSermonById(String id);

    @Query("SELECT * FROM sermons ORDER BY dateMillis DESC")
    List<SermonEntity> getAllSermons();

    @Query("SELECT * FROM sermons WHERE isDownloaded = 1 ORDER BY dateMillis DESC")
    List<SermonEntity> getDownloadedSermons();

    @Query("SELECT * FROM sermons WHERE series = :series ORDER BY dateMillis DESC")
    List<SermonEntity> getSermonsBySeries(String series);

    @Query("DELETE FROM sermons WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM sermons")
    void deleteAll();
}
