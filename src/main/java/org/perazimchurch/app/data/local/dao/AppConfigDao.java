package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.perazimchurch.app.data.local.entity.AppConfigEntity;

import java.util.List;

@Dao
public interface AppConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppConfigEntity config);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppConfigEntity> configs);

    @Query("SELECT value FROM app_config WHERE `key` = :key LIMIT 1")
    String getConfigValue(String key);

    @Query("SELECT * FROM app_config")
    List<AppConfigEntity> getAllConfig();

    @Query("DELETE FROM app_config WHERE `key` = :key")
    void deleteByKey(String key);

    @Query("DELETE FROM app_config")
    void deleteAll();
}
