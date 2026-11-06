package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.perazimchurch.app.data.local.entity.DailyChallengeEntity;

@Dao
public interface DailyChallengeDao {

    @Query("SELECT * FROM daily_challenges WHERE date = :date LIMIT 1")
    DailyChallengeEntity getChallengeByDate(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyChallengeEntity challenge);

    @Query("UPDATE daily_challenges SET isCompleted = 1 WHERE date = :date")
    void markCompleted(String date);
}
