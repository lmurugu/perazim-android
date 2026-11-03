package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "app_config".
 * Stores key-value remote configuration settings from Google Sheets.
 */
@Entity(tableName = "app_config")
public class AppConfigEntity {

    @PrimaryKey
    @NonNull
    private String key;
    private String value;

    public AppConfigEntity(@NonNull String key, String value) {
        this.key = key;
        this.value = value;
    }

    @NonNull
    public String getKey() { return key; }
    public void setKey(@NonNull String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
