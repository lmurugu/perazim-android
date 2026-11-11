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
import org.perazimchurch.app.data.local.dao.CommentDao;
import org.perazimchurch.app.data.local.dao.ConnectionDao;
import org.perazimchurch.app.data.local.dao.ConversationDao;
import org.perazimchurch.app.data.local.dao.DailyChallengeDao;
import org.perazimchurch.app.data.local.dao.DailyVerseDao;
import org.perazimchurch.app.data.local.dao.DownloadedContentDao;
import org.perazimchurch.app.data.local.dao.EventDao;
import org.perazimchurch.app.data.local.dao.EventRsvpDao;
import org.perazimchurch.app.data.local.dao.GroupDao;
import org.perazimchurch.app.data.local.dao.HymnDao;
import org.perazimchurch.app.data.local.dao.JokeDao;
import org.perazimchurch.app.data.local.dao.MessageDao;
import org.perazimchurch.app.data.local.dao.NotificationDao;
import org.perazimchurch.app.data.local.dao.PostDao;
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
import org.perazimchurch.app.data.local.entity.CommentEntity;
import org.perazimchurch.app.data.local.entity.ConnectionEntity;
import org.perazimchurch.app.data.local.entity.ConversationEntity;
import org.perazimchurch.app.data.local.entity.DailyChallengeEntity;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.data.local.entity.DownloadedContentEntity;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.local.entity.EventRsvpEntity;
import org.perazimchurch.app.data.local.entity.GroupEntity;
import org.perazimchurch.app.data.local.entity.GroupMemberEntity;
import org.perazimchurch.app.data.local.entity.HymnEntity;
import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.MessageEntity;
import org.perazimchurch.app.data.local.entity.NotificationEntity;
import org.perazimchurch.app.data.local.entity.PostEntity;
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
 * Conversations, Messages, Connections, Notifications, Sync Queues, Offline Downloads,
 * and Community (Posts, Comments, Groups, Group Members, Event RSVPs, Daily Challenges).
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
        AppConfigEntity.class,
        PostEntity.class,
        CommentEntity.class,
        GroupEntity.class,
        GroupMemberEntity.class,
        EventRsvpEntity.class,
        DailyChallengeEntity.class
    },
    version = 7,
    exportSchema = true
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
    public abstract PostDao postDao();
    public abstract CommentDao commentDao();
    public abstract GroupDao groupDao();
    public abstract EventRsvpDao eventRsvpDao();
    public abstract DailyChallengeDao dailyChallengeDao();

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

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN bio TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaUrl TEXT DEFAULT ''");
            db.execSQL("CREATE TABLE IF NOT EXISTS `community_posts` (" +
                    "`id` TEXT NOT NULL, `authorId` TEXT, `authorName` TEXT, `authorAvatar` TEXT, " +
                    "`type` TEXT, `title` TEXT, `content` TEXT, `mediaUrl` TEXT, `groupId` TEXT, " +
                    "`isPinned` INTEGER NOT NULL DEFAULT 0, `isAnswered` INTEGER NOT NULL DEFAULT 0, " +
                    "`eventDate` INTEGER, `eventLocation` TEXT, `amenCount` INTEGER NOT NULL DEFAULT 0, " +
                    "`commentCount` INTEGER NOT NULL DEFAULT 0, `userHasAmened` INTEGER NOT NULL DEFAULT 0, " +
                    "`userRsvpStatus` TEXT, `createdMillis` INTEGER NOT NULL DEFAULT 0, " +
                    "`updatedMillis` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `post_comments` (" +
                    "`id` TEXT NOT NULL, `postId` TEXT, `authorId` TEXT, `authorName` TEXT, " +
                    "`authorAvatar` TEXT, `content` TEXT, `createdMillis` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `community_groups` (" +
                    "`id` TEXT NOT NULL, `name` TEXT, `description` TEXT, `createdBy` TEXT, " +
                    "`isActive` INTEGER NOT NULL DEFAULT 1, `createdMillis` INTEGER NOT NULL DEFAULT 0, " +
                    "`memberCount` INTEGER NOT NULL DEFAULT 0, `isMember` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `group_members` (" +
                    "`groupId` TEXT NOT NULL, `userId` TEXT NOT NULL, `role` TEXT, `name` TEXT, " +
                    "`avatarUrl` TEXT, `campusId` TEXT, `joinedMillis` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`groupId`, `userId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `event_rsvps` (" +
                    "`postId` TEXT NOT NULL, `userId` TEXT NOT NULL, `status` TEXT, " +
                    "`updatedMillis` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`postId`, `userId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_challenges` (" +
                    "`date` TEXT NOT NULL, `riddleId` TEXT, `question` TEXT, `hint` TEXT, " +
                    "`isCompleted` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`date`))");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE `sync_queue_new` (" +
                    "`sequenceNumber` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`id` TEXT NOT NULL, `operationType` TEXT, `entityType` TEXT, `entityId` TEXT, " +
                    "`payloadJson` TEXT, `timestampMillis` INTEGER NOT NULL, " +
                    "`nextAttemptAt` INTEGER NOT NULL, `status` TEXT, " +
                    "`retryCount` INTEGER NOT NULL, `lastError` TEXT)");
            db.execSQL("INSERT INTO `sync_queue_new` (`id`, `operationType`, `entityType`, `entityId`, `payloadJson`, `timestampMillis`, `nextAttemptAt`, `status`, `retryCount`, `lastError`) " +
                    "SELECT `id`, `operationType`, `entityType`, `entityId`, `payloadJson`, `timestampMillis`, 0, `status`, `retryCount`, `lastError` FROM `sync_queue` ORDER BY `timestampMillis` ASC, `id` ASC");
            db.execSQL("DROP TABLE `sync_queue`");
            db.execSQL("ALTER TABLE `sync_queue_new` RENAME TO `sync_queue`");
            db.execSQL("CREATE UNIQUE INDEX `index_sync_queue_id` ON `sync_queue` (`id`)");
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
