package com.example.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.app.data.local.converter.Converters;
import com.example.app.data.local.dao.AnnouncementDao;
import com.example.app.data.local.dao.BibleDao;
import com.example.app.data.local.dao.CampusDao;
import com.example.app.data.local.dao.ConnectionDao;
import com.example.app.data.local.dao.ConversationDao;
import com.example.app.data.local.dao.DownloadedContentDao;
import com.example.app.data.local.dao.EventDao;
import com.example.app.data.local.dao.HymnDao;
import com.example.app.data.local.dao.JokeDao;
import com.example.app.data.local.dao.MessageDao;
import com.example.app.data.local.dao.NotificationDao;
import com.example.app.data.local.dao.PrayerDao;
import com.example.app.data.local.dao.ReflectionDao;
import com.example.app.data.local.dao.RiddleDao;
import com.example.app.data.local.dao.SermonDao;
import com.example.app.data.local.dao.SyncMetadataDao;
import com.example.app.data.local.dao.SyncQueueDao;
import com.example.app.data.local.dao.UserDao;
import com.example.app.data.local.entity.AnnouncementEntity;
import com.example.app.data.local.entity.BibleBookEntity;
import com.example.app.data.local.entity.BibleChapterEntity;
import com.example.app.data.local.entity.BibleTranslationEntity;
import com.example.app.data.local.entity.BibleVerseEntity;
import com.example.app.data.local.entity.CampusEntity;
import com.example.app.data.local.entity.ConnectionEntity;
import com.example.app.data.local.entity.ConversationEntity;
import com.example.app.data.local.entity.DownloadedContentEntity;
import com.example.app.data.local.entity.EventEntity;
import com.example.app.data.local.entity.HymnEntity;
import com.example.app.data.local.entity.JokeEntity;
import com.example.app.data.local.entity.MessageEntity;
import com.example.app.data.local.entity.NotificationEntity;
import com.example.app.data.local.entity.PrayerEntity;
import com.example.app.data.local.entity.ReflectionEntity;
import com.example.app.data.local.entity.RiddleEntity;
import com.example.app.data.local.entity.SermonEntity;
import com.example.app.data.local.entity.SyncMetadataEntity;
import com.example.app.data.local.entity.SyncQueueEntity;
import com.example.app.data.local.entity.UserEntity;

/**
 * Main Room Database for the Perazim Android application.
 * Persists all core entities: Users, Campuses, Sermons, Hymns, Riddles, Jokes, Events,
 * Announcements, Prayers, Reflections, Bible (translations, books, chapters, verses),
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
        DownloadedContentEntity.class
    },
    version = 3,
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

    public static PerazimDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PerazimDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            PerazimDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
