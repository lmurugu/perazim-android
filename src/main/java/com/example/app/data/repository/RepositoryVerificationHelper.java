package com.example.app.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.dao.AnnouncementDao;
import com.example.app.data.local.dao.BibleDao;
import com.example.app.data.local.dao.ConversationDao;
import com.example.app.data.local.dao.EventDao;
import com.example.app.data.local.dao.HymnDao;
import com.example.app.data.local.dao.MessageDao;
import com.example.app.data.local.dao.PrayerDao;
import com.example.app.data.local.dao.ReflectionDao;
import com.example.app.data.local.dao.SermonDao;
import com.example.app.data.local.dao.UserDao;
import com.example.app.data.local.entity.AnnouncementEntity;
import com.example.app.data.local.entity.BibleVerseEntity;
import com.example.app.data.local.entity.EventEntity;
import com.example.app.data.local.entity.HymnEntity;
import com.example.app.data.local.entity.ReflectionEntity;
import com.example.app.data.local.entity.SermonEntity;
import com.example.app.domain.model.Announcement;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.Conversation;
import com.example.app.domain.model.Event;
import com.example.app.domain.model.Hymn;
import com.example.app.domain.model.Message;
import com.example.app.domain.model.Prayer;
import com.example.app.domain.model.Reflection;
import com.example.app.domain.model.Sermon;
import com.example.app.domain.model.User;
import com.example.app.domain.repository.AnnouncementRepository;
import com.example.app.domain.repository.BibleRepository;
import com.example.app.domain.repository.EventRepository;
import com.example.app.domain.repository.HymnRepository;
import com.example.app.domain.repository.MessageRepository;
import com.example.app.domain.repository.PrayerRepository;
import com.example.app.domain.repository.ReflectionRepository;
import com.example.app.domain.repository.SermonRepository;
import com.example.app.domain.repository.UserRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous verification helper for Phase 1.2: Repository Layer Integration.
 * Validates all 9 domain repositories through {@link RepositoryProvider} and ensures
 * correct mapping, domain model integrity, and transactional persistence.
 */
public class RepositoryVerificationHelper {

    private static final String TAG = "PerazimRepository";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onVerificationComplete(boolean success, String message);
    }

    public static void runVerification(Context context) {
        runVerification(context, null);
    }

    public static void runVerification(Context context, VerificationCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Log.d(TAG, "Starting Phase 1.2 Repository Verification Gate...");
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                RepositoryProvider provider = RepositoryProvider.getInstance(context);

                UserDao userDao = db.userDao();
                SermonDao sermonDao = db.sermonDao();
                HymnDao hymnDao = db.hymnDao();
                BibleDao bibleDao = db.bibleDao();
                EventDao eventDao = db.eventDao();
                AnnouncementDao announcementDao = db.announcementDao();
                PrayerDao prayerDao = db.prayerDao();
                ReflectionDao reflectionDao = db.reflectionDao();
                MessageDao messageDao = db.messageDao();
                ConversationDao conversationDao = db.conversationDao();

                long timestamp = System.currentTimeMillis();

                // 1. UserRepository: call getCurrentUser(), updateStreak(5, false), addSpiritualXp(20), verify values
                UserRepository userRepo = provider.getUserRepository();
                User currentUser = userRepo.getCurrentUser();
                if (currentUser == null) {
                    throw new IllegalStateException("UserRepository.getCurrentUser() returned null");
                }
                int initialXp = currentUser.getSpiritualXp();
                userRepo.updateStreak(5, false);
                userRepo.addSpiritualXp(20);
                User updatedUser = userRepo.getCurrentUser();
                if (updatedUser == null) {
                    throw new IllegalStateException("UserRepository.getCurrentUser() returned null after update");
                }
                if (updatedUser.getStreakCount() != 5) {
                    throw new IllegalStateException("UserRepository streak verification failed: expected 5, got " + updatedUser.getStreakCount());
                }
                if (updatedUser.isStreakFrozen()) {
                    throw new IllegalStateException("UserRepository streakFrozen verification failed: expected false, got true");
                }
                if (updatedUser.getSpiritualXp() != initialXp + 20) {
                    throw new IllegalStateException("UserRepository spiritualXp verification failed: expected " + (initialXp + 20) + ", got " + updatedUser.getSpiritualXp());
                }
                Log.d(TAG, "UserRepository verified successfully.");

                // 2. SermonRepository: insert test SermonEntity via sermonDao(), call getRecentSermons(), searchSermons(), markAsDownloaded(), verify
                String sermonId = "test_sermon_1";
                SermonEntity testSermon = new SermonEntity(
                        sermonId,
                        "Walking in Supernatural Breakthrough",
                        "Bishop Dr. David Mutweri",
                        "Breakthrough Series",
                        "2 Samuel 5:20",
                        "https://audio.perazim.org/sermon1.mp3",
                        "https://video.perazim.org/sermon1.mp4",
                        timestamp,
                        2400,
                        "Faith and perseverance for breakthrough",
                        "Key sermon notes",
                        false,
                        ""
                );
                sermonDao.insert(testSermon);

                SermonRepository sermonRepo = provider.getSermonRepository();
                List<Sermon> recentSermons = sermonRepo.getRecentSermons();
                boolean foundRecent = false;
                for (Sermon s : recentSermons) {
                    if (sermonId.equals(s.getId())) {
                        foundRecent = true;
                        break;
                    }
                }
                if (!foundRecent) {
                    throw new IllegalStateException("SermonRepository.getRecentSermons() failed to return test sermon");
                }

                List<Sermon> searchResults = sermonRepo.searchSermons("Supernatural");
                boolean foundSearch = false;
                for (Sermon s : searchResults) {
                    if (sermonId.equals(s.getId())) {
                        foundSearch = true;
                        break;
                    }
                }
                if (!foundSearch) {
                    throw new IllegalStateException("SermonRepository.searchSermons() failed to find sermon by keyword");
                }

                sermonRepo.markAsDownloaded(sermonId, "/local/path/audio.mp3");
                Sermon downloadedSermon = sermonRepo.getSermonById(sermonId);
                if (downloadedSermon == null || !downloadedSermon.isDownloaded()) {
                    throw new IllegalStateException("SermonRepository.markAsDownloaded() failed to set isDownloaded flag");
                }
                Log.d(TAG, "SermonRepository verified successfully.");

                // 3. HymnRepository: insert test HymnEntity via hymnDao(), call getAllHymns(), getHymnByNumber(999), toggleFavorite(), verify
                String hymnId = "test_hymn_1";
                HymnEntity testHymn = new HymnEntity(
                        hymnId,
                        999,
                        "Holy, Holy, Holy! Lord God Almighty",
                        "Holy, Holy, Holy! Lord God Almighty! Early in the morning our song shall rise to Thee...",
                        "D - Bm - G - A",
                        "D Major",
                        "4/4",
                        "Adoration",
                        false
                );
                hymnDao.insert(testHymn);

                HymnRepository hymnRepo = provider.getHymnRepository();
                List<Hymn> allHymns = hymnRepo.getAllHymns();
                boolean foundHymn = false;
                for (Hymn h : allHymns) {
                    if (hymnId.equals(h.getId())) {
                        foundHymn = true;
                        break;
                    }
                }
                if (!foundHymn) {
                    throw new IllegalStateException("HymnRepository.getAllHymns() failed to return test hymn");
                }

                Hymn hymnByNum = hymnRepo.getHymnByNumber(999);
                if (hymnByNum == null || !hymnId.equals(hymnByNum.getId())) {
                    throw new IllegalStateException("HymnRepository.getHymnByNumber(999) failed: " + (hymnByNum == null ? "null" : hymnByNum.getId()));
                }

                hymnRepo.toggleFavorite(hymnId, true);
                Hymn favoriteHymn = hymnRepo.getHymnById(hymnId);
                if (favoriteHymn == null || !favoriteHymn.isFavorite()) {
                    throw new IllegalStateException("HymnRepository.toggleFavorite() failed: expected isFavorite=true");
                }
                Log.d(TAG, "HymnRepository verified successfully.");

                // 4. BibleRepository: insert test BibleVerseEntity via bibleDao(), call getVerse("KJV", "2SA", 5, 20), verify domain model text
                String verseId = "test_verse_repo_1_2";
                BibleVerseEntity testVerse = new BibleVerseEntity(
                        verseId,
                        "ch_2sam_5",
                        10, // 2 Samuel
                        5,
                        20,
                        "And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters."
                );
                bibleDao.insertVerse(testVerse);

                BibleRepository bibleRepo = provider.getBibleRepository();
                BibleVerse verse = bibleRepo.getVerse("KJV", "2SA", 5, 20);
                if (verse == null) {
                    throw new IllegalStateException("BibleRepository.getVerse('KJV', '2SA', 5, 20) returned null");
                }
                if (verse.getText() == null || !verse.getText().contains("Baal-perazim")) {
                    throw new IllegalStateException("BibleRepository verse text verification failed: got " + verse.getText());
                }
                Log.d(TAG, "BibleRepository verified successfully.");

                // 5. EventRepository: insert test EventEntity via eventDao(), call getUpcomingEvents(), verify domain model
                String eventId = "test_event_1";
                long now = System.currentTimeMillis();
                EventEntity testEvent = new EventEntity(
                        eventId,
                        "Breakthrough Night of Wonders",
                        "An evening of signs, wonders, and breakthrough prayers.",
                        now + 3600000L,
                        now + 7200000L,
                        "Main Sanctuary",
                        "campus_central",
                        "https://events.perazim.org/wonders.jpg",
                        "Worship"
                );
                eventDao.insert(testEvent);

                EventRepository eventRepo = provider.getEventRepository();
                List<Event> upcomingEvents = eventRepo.getUpcomingEvents();
                boolean foundEvent = false;
                for (Event e : upcomingEvents) {
                    if (eventId.equals(e.getId())) {
                        foundEvent = true;
                        if (!"Breakthrough Night of Wonders".equals(e.getTitle())) {
                            throw new IllegalStateException("EventRepository domain model title mismatch: " + e.getTitle());
                        }
                        break;
                    }
                }
                if (!foundEvent) {
                    throw new IllegalStateException("EventRepository.getUpcomingEvents() failed to return test event");
                }
                Log.d(TAG, "EventRepository verified successfully.");

                // 6. AnnouncementRepository: insert test AnnouncementEntity via announcementDao(), call getActiveAnnouncements(), verify domain model
                String announcementId = "test_announcement_1";
                AnnouncementEntity testAnnouncement = new AnnouncementEntity(
                        announcementId,
                        "Annual Breakthrough Conference Registration",
                        "Registration is now open for all delegates across all campuses.",
                        "HIGH",
                        now,
                        now + 86400000L,
                        "campus_central"
                );
                announcementDao.insert(testAnnouncement);

                AnnouncementRepository announcementRepo = provider.getAnnouncementRepository();
                List<Announcement> activeAnnouncements = announcementRepo.getActiveAnnouncements();
                boolean foundAnnouncement = false;
                for (Announcement a : activeAnnouncements) {
                    if (announcementId.equals(a.getId())) {
                        foundAnnouncement = true;
                        if (!"Annual Breakthrough Conference Registration".equals(a.getTitle())) {
                            throw new IllegalStateException("AnnouncementRepository domain title mismatch: " + a.getTitle());
                        }
                        break;
                    }
                }
                if (!foundAnnouncement) {
                    throw new IllegalStateException("AnnouncementRepository.getActiveAnnouncements() failed to return test announcement");
                }
                Log.d(TAG, "AnnouncementRepository verified successfully.");

                // 7. PrayerRepository: call submitPrayer(testPrayer), call getPublicPrayers(), call amenPrayer(), verify domain model
                String prayerId = "test_prayer_1";
                Prayer testPrayer = new Prayer(
                        prayerId,
                        "Healing & Divine Breakthrough",
                        "Praying for full recovery and breakthrough in family finances.",
                        "test_user",
                        "Elder Test",
                        Prayer.Visibility.PUBLIC,
                        0,
                        timestamp,
                        false,
                        false
                );
                PrayerRepository prayerRepo = provider.getPrayerRepository();
                prayerRepo.submitPrayer(testPrayer);

                List<Prayer> publicPrayers = prayerRepo.getPublicPrayers();
                boolean foundPrayer = false;
                for (Prayer p : publicPrayers) {
                    if (prayerId.equals(p.getId())) {
                        foundPrayer = true;
                        if (!"Healing & Divine Breakthrough".equals(p.getTitle())) {
                            throw new IllegalStateException("PrayerRepository domain title mismatch: " + p.getTitle());
                        }
                        break;
                    }
                }
                if (!foundPrayer) {
                    throw new IllegalStateException("PrayerRepository.getPublicPrayers() failed to return submitted prayer");
                }

                prayerRepo.amenPrayer(prayerId, "test_user");
                Prayer amenedPrayer = prayerRepo.getPrayerById(prayerId);
                if (amenedPrayer == null || amenedPrayer.getAmenCount() != 1) {
                    throw new IllegalStateException("PrayerRepository.amenPrayer() verification failed: amenCount expected 1, got " + (amenedPrayer == null ? "null" : amenedPrayer.getAmenCount()));
                }
                Log.d(TAG, "PrayerRepository verified successfully.");

                // 8. ReflectionRepository: insert test ReflectionEntity via reflectionDao(), call getTodayReflection(), call markReflectionCompleted()
                String reflectionId = "test_reflection_1";
                ReflectionEntity testReflection = new ReflectionEntity(
                        reflectionId,
                        "test_user",
                        "The Lord of the Breakthrough",
                        "God will break through every obstacle in your path.",
                        "2 Samuel 5:20",
                        timestamp,
                        timestamp,
                        false
                );
                reflectionDao.insert(testReflection);

                ReflectionRepository reflectionRepo = provider.getReflectionRepository();
                Reflection todayReflection = reflectionRepo.getTodayReflection();
                if (todayReflection == null) {
                    throw new IllegalStateException("ReflectionRepository.getTodayReflection() returned null");
                }
                reflectionRepo.markReflectionCompleted(todayReflection.getId());
                Reflection completedReflection = reflectionRepo.getReflectionById(todayReflection.getId());
                if (completedReflection == null || !completedReflection.isCompleted()) {
                    throw new IllegalStateException("ReflectionRepository.markReflectionCompleted() failed: isCompleted expected true");
                }
                Log.d(TAG, "ReflectionRepository verified successfully.");

                // 9. MessageRepository: call sendMessage(testMessage), call getConversations("test_user"), verify domain model
                String messageId = "test_msg_1";
                String conversationId = "test_conv_1";
                Message testMessage = new Message(
                        messageId,
                        "client_msg_1",
                        conversationId,
                        "test_user",
                        "Elder Test",
                        "Peace and blessings, fellowship saints!",
                        "SENT",
                        timestamp
                );
                MessageRepository messageRepo = provider.getMessageRepository();
                messageRepo.sendMessage(testMessage);

                List<Conversation> conversations = messageRepo.getConversations("test_user");
                boolean foundConv = false;
                for (Conversation c : conversations) {
                    if (conversationId.equals(c.getId())) {
                        foundConv = true;
                        if (!"Peace and blessings, fellowship saints!".equals(c.getLastMessageSnippet())) {
                            throw new IllegalStateException("MessageRepository conversation lastMessageSnippet mismatch: " + c.getLastMessageSnippet());
                        }
                        break;
                    }
                }
                if (!foundConv) {
                    throw new IllegalStateException("MessageRepository.getConversations() failed to return conversation for test message");
                }
                Log.d(TAG, "MessageRepository verified successfully.");

                // 10. Clean up test records from all DAOs
                if (currentUser != null && "guest_user".equals(currentUser.getId())) {
                    userDao.deleteById("guest_user");
                }
                sermonDao.deleteById(sermonId);
                hymnDao.deleteById(hymnId);
                bibleDao.deleteVerseById(verseId);
                eventDao.deleteById(eventId);
                announcementDao.deleteById(announcementId);
                prayerDao.deleteById(prayerId);
                reflectionDao.deleteById(reflectionId);
                messageDao.deleteById(messageId);
                conversationDao.deleteById(conversationId);

                if (sermonDao.getSermonById(sermonId) != null ||
                        hymnDao.getHymnById(hymnId) != null ||
                        bibleDao.getVerseById(verseId) != null ||
                        eventDao.getEventById(eventId) != null ||
                        announcementDao.getAnnouncementById(announcementId) != null ||
                        prayerDao.getPrayerById(prayerId) != null ||
                        reflectionDao.getReflectionById(reflectionId) != null ||
                        messageDao.getMessageById(messageId) != null ||
                        conversationDao.getConversationById(conversationId) != null) {
                    throw new IllegalStateException("Cleanup failed: test records remain in database");
                }
                Log.d(TAG, "All test records cleaned up successfully.");

                // 11. Log success marker
                Log.i(TAG, "[PERAZIM-REPOSITORY-GATE-1.2: SUCCESS]");

                if (callback != null) {
                    callback.onVerificationComplete(true, "All 9 repositories verified successfully.");
                }

            } catch (Exception error) {
                Log.e(TAG, "[PERAZIM-REPOSITORY-GATE-1.2: FAILED] " + error.getMessage(), error);
                if (callback != null) {
                    callback.onVerificationComplete(false, error.getMessage());
                }
            }
        });
    }
}
