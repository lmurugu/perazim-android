package org.perazimchurch.app.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.perazimchurch.app.data.local.converter.Converters;
import org.perazimchurch.app.data.local.dao.AnnouncementDao;
import org.perazimchurch.app.data.local.dao.AppConfigDao;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.dao.CampusDao;
import org.perazimchurch.app.data.local.dao.ConnectionDao;
import org.perazimchurch.app.data.local.dao.ConversationDao;
import org.perazimchurch.app.data.local.dao.DailyVerseDao;
import org.perazimchurch.app.data.local.dao.DownloadedContentDao;
import org.perazimchurch.app.data.local.dao.EventDao;
import org.perazimchurch.app.data.local.dao.HymnDao;
import org.perazimchurch.app.data.local.dao.JokeDao;
import org.perazimchurch.app.data.local.dao.MessageDao;
import org.perazimchurch.app.data.local.dao.NotificationDao;
import org.perazimchurch.app.data.local.dao.PrayerDao;
import org.perazimchurch.app.data.local.dao.ReflectionDao;
import org.perazimchurch.app.data.local.dao.RiddleDao;
import org.perazimchurch.app.data.local.dao.SermonDao;
import org.perazimchurch.app.data.local.dao.SyncMetadataDao;
import org.perazimchurch.app.data.local.dao.SyncQueueDao;
import org.perazimchurch.app.data.local.dao.UserDao;
import org.perazimchurch.app.data.local.entity.AnnouncementEntity;
import org.perazimchurch.app.data.local.entity.AppConfigEntity;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleChapterEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.entity.CampusEntity;
import org.perazimchurch.app.data.local.entity.ConnectionEntity;
import org.perazimchurch.app.data.local.entity.ConversationEntity;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.data.local.entity.DownloadedContentEntity;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.local.entity.HymnEntity;
import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.MessageEntity;
import org.perazimchurch.app.data.local.entity.NotificationEntity;
import org.perazimchurch.app.data.local.entity.PrayerEntity;
import org.perazimchurch.app.data.local.entity.ReflectionEntity;
import org.perazimchurch.app.data.local.entity.RiddleEntity;
import org.perazimchurch.app.data.local.entity.SermonEntity;
import org.perazimchurch.app.data.local.entity.SyncMetadataEntity;
import org.perazimchurch.app.data.local.entity.SyncQueueEntity;
import org.perazimchurch.app.data.local.entity.UserEntity;

/**
 * Main Room Database for the Perazim Android application.
 * Persists all core entities: Users, Campuses, Sermons, Hymns, Riddles, Jokes, Events,
 * Announcements, Prayers, Reflections, Daily Verses, App Config, Bible (translations, books, chapters, verses),
 * Conversations, Messages, Connections, Notifications, Sync Queues, and Offline Downloads.
 */
@Database(
    entities = {
        UserEntity.class,
        CampusEntity.class,
        SermonEntity.class,
        HymnEntity.class,
        RiddleEntity.class,
        JokeEntity.class,
        EventEntity.class,
        AnnouncementEntity.class,
        PrayerEntity.class,
        ReflectionEntity.class,
        BibleTranslationEntity.class,
        BibleBookEntity.class,
        BibleChapterEntity.class,
        BibleVerseEntity.class,
        ConversationEntity.class,
        MessageEntity.class,
        ConnectionEntity.class,
        NotificationEntity.class,
        SyncQueueEntity.class,
        SyncMetadataEntity.class,
        DownloadedContentEntity.class,
        DailyVerseEntity.class,
        AppConfigEntity.class
    },
    version = 5,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class PerazimDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "perazim_database.db";
    private static volatile PerazimDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract CampusDao campusDao();
    public abstract SermonDao sermonDao();
    public abstract HymnDao hymnDao();
    public abstract RiddleDao riddleDao();
    public abstract JokeDao jokeDao();
    public abstract BibleDao bibleDao();
    public abstract EventDao eventDao();
    public abstract AnnouncementDao announcementDao();
    public abstract PrayerDao prayerDao();
    public abstract ReflectionDao reflectionDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();
    public abstract ConnectionDao connectionDao();
    public abstract NotificationDao notificationDao();
    public abstract DownloadedContentDao downloadedContentDao();
    public abstract SyncQueueDao syncQueueDao();
    public abstract SyncMetadataDao syncMetadataDao();
    public abstract DailyVerseDao dailyVerseDao();
    public abstract AppConfigDao appConfigDao();

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE announcements ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE events ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE hymns ADD COLUMN writer TEXT");
            db.execSQL("ALTER TABLE hymns ADD COLUMN year TEXT");
            db.execSQL("ALTER TABLE hymns ADD COLUMN bpm INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE hymns ADD COLUMN tags TEXT");
            db.execSQL("ALTER TABLE hymns ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE riddles ADD COLUMN category TEXT");
            db.execSQL("ALTER TABLE riddles ADD COLUMN xp INTEGER NOT NULL DEFAULT 20");
            db.execSQL("ALTER TABLE riddles ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE jokes ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE sermons ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_verses` (`date` TEXT NOT NULL, `reference` TEXT, `text` TEXT, `active` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`date`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`key` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`key`))");
        }
    };

    public static PerazimDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PerazimDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            PerazimDatabase.class,
                            DATABASE_NAME
                    )
                    .createFromAsset("database/perazim_database.db")
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
