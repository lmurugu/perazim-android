package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.perazimchurch.app.data.local.entity.DailyVerseEntity;

import java.util.List;

@Dao
public interface DailyVerseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyVerseEntity verse);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DailyVerseEntity> verses);

    @Update
    void update(DailyVerseEntity verse);

    @Delete
    void delete(DailyVerseEntity verse);

    @Query("SELECT * FROM daily_verses WHERE date = :date AND active = 1 LIMIT 1")
    DailyVerseEntity getVerseForDate(String date);

    @Query("SELECT * FROM daily_verses WHERE active = 1 ORDER BY date DESC")
    List<DailyVerseEntity> getAllActiveVerses();

    @Query("DELETE FROM daily_verses WHERE date = :date")
    void deleteByDate(String date);

    @Query("DELETE FROM daily_verses")
    void deleteAll();
}
