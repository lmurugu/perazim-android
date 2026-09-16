package com.example.app.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.app.data.local.PerazimDatabase;
import com.example.app.domain.repository.AnnouncementRepository;
import com.example.app.domain.repository.BibleRepository;
import com.example.app.domain.repository.ConnectionRepository;
import com.example.app.domain.repository.DownloadedContentRepository;
import com.example.app.domain.repository.EventRepository;
import com.example.app.domain.repository.HymnRepository;
import com.example.app.domain.repository.MessageRepository;
import com.example.app.domain.repository.NotificationRepository;
import com.example.app.domain.repository.PrayerRepository;
import com.example.app.domain.repository.ReflectionRepository;
import com.example.app.domain.repository.SermonRepository;
import com.example.app.domain.repository.UserRepository;

/**
 * Thread-safe singleton service locator that instantiates and provides
 * all domain repositories backed by {@link PerazimDatabase}.
 */
public class RepositoryProvider {
    private static volatile RepositoryProvider instance;
    private final UserRepository userRepository;
    private final PrayerRepository prayerRepository;
    private final MessageRepository messageRepository;
    private final SermonRepository sermonRepository;
    private final HymnRepository hymnRepository;
    private final EventRepository eventRepository;
    private final AnnouncementRepository announcementRepository;
    private final ReflectionRepository reflectionRepository;
    private final BibleRepository bibleRepository;
    private final DownloadedContentRepository downloadedContentRepository;
    private final ConnectionRepository connectionRepository;
    private final NotificationRepository notificationRepository;

    private RepositoryProvider(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        PerazimDatabase db = PerazimDatabase.getInstance(appContext);
        this.userRepository = new RoomUserRepository(db.userDao(), db.campusDao());
        this.prayerRepository = new RoomPrayerRepository(db.prayerDao());
        this.messageRepository = new RoomMessageRepository(db.messageDao(), db.conversationDao());
        this.sermonRepository = new RoomSermonRepository(db.sermonDao());
        this.hymnRepository = new RoomHymnRepository(db.hymnDao());
        this.eventRepository = new RoomEventRepository(db.eventDao());
        this.announcementRepository = new RoomAnnouncementRepository(db.announcementDao());
        this.reflectionRepository = new RoomReflectionRepository(db.reflectionDao());
        this.bibleRepository = new RoomBibleRepository(db.bibleDao());
        this.downloadedContentRepository = new RoomDownloadedContentRepository(db.downloadedContentDao());
        this.connectionRepository = new RoomConnectionRepository(db.connectionDao());
        this.notificationRepository = new RoomNotificationRepository(db.notificationDao());
    }

    public static RepositoryProvider getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (RepositoryProvider.class) {
                if (instance == null) {
                    instance = new RepositoryProvider(context);
                }
            }
        }
        return instance;
    }

    public UserRepository getUserRepository() { return userRepository; }
    public PrayerRepository getPrayerRepository() { return prayerRepository; }
    public MessageRepository getMessageRepository() { return messageRepository; }
    public SermonRepository getSermonRepository() { return sermonRepository; }
    public HymnRepository getHymnRepository() { return hymnRepository; }
    public EventRepository getEventRepository() { return eventRepository; }
    public AnnouncementRepository getAnnouncementRepository() { return announcementRepository; }
    public ReflectionRepository getReflectionRepository() { return reflectionRepository; }
    public BibleRepository getBibleRepository() { return bibleRepository; }
    public DownloadedContentRepository getDownloadedContentRepository() { return downloadedContentRepository; }
    public ConnectionRepository getConnectionRepository() { return connectionRepository; }
    public NotificationRepository getNotificationRepository() { return notificationRepository; }
}
