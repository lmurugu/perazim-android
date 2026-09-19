package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "bible_translations".
 */
@Entity(tableName = "bible_translations")
public class BibleTranslationEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String shortName;
    private String fullName;
    private String language;
    private String versionInfo;
    private boolean isDefault;
    private String licenseInfo;

    public BibleTranslationEntity(@NonNull String id, String shortName, String fullName,
                                  String language, String versionInfo, boolean isDefault,
                                  String licenseInfo) {
        this.id = id;
        this.shortName = shortName;
        this.fullName = fullName;
        this.language = language;
        this.versionInfo = versionInfo;
        this.isDefault = isDefault;
        this.licenseInfo = licenseInfo;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getVersionInfo() { return versionInfo; }
    public void setVersionInfo(String versionInfo) { this.versionInfo = versionInfo; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public String getLicenseInfo() { return licenseInfo; }
    public void setLicenseInfo(String licenseInfo) { this.licenseInfo = licenseInfo; }
}
