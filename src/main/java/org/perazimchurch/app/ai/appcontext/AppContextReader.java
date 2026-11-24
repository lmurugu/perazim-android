package org.perazimchurch.app.ai.appcontext;

import android.content.Context;
import android.content.SharedPreferences;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleNoteEntity;
import org.perazimchurch.app.data.local.entity.BibleNoteLinkEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.local.entity.GroupEntity;
import org.perazimchurch.app.data.local.entity.PostEntity;
import org.perazimchurch.app.data.local.entity.SermonEntity;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.local.preference.QuestionHistoryStore;
import org.perazimchurch.app.data.local.preference.ReadingLog;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.games.ReviewScheduler;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.KeyValueStore;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.presentation.ui.games.GamesHubDialog;
import org.perazimchurch.app.presentation.ui.games.common.GameCatalog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Reads the few facts the guide needs from this phone's own stores. Read-only, offline, and every source is wrapped so a
 * failed read only leaves that part of the answer out.
 *
 * <p>Deliberately never touched: personal messages, private chats and member connections. Community reads go only
 * through the public-post query, joined groups and upcoming events.
 * Call {@link #read} on a worker thread, never the main thread.
 */
public final class AppContextReader {

    private static final String SETTINGS_PREFS = "perazim_guide_app_context";
    private static final String KEY_ENABLED = "enabled";
    private static final String BIBLE_READER_PREFS = "bible_reader";
    private static final String SCORES_PREFS = "perazim_game_scores_v1";
    private static final String ROUTINE_PREFS = "perazim_routine";
    private static final String DEFAULT_TRANSLATION = "KJV";
    private static final String TIMEZONE = "Africa/Nairobi";
    private static final int MAX_MISSED = 3;

    private AppContextReader() {}

    /** The member can switch the guide's access to app activity off. Defaults to on, and the switch is always visible. */
    public static boolean isEnabled(Context context) {
        try {
            return prefs(context).getBoolean(KEY_ENABLED, true);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        try {
            prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        } catch (RuntimeException ignored) {
            // Preference storage unavailable: the switch keeps its previous value.
        }
    }

    /** Reads only the sources the routed topics need. Never throws. */
    public static AppSnapshot read(Context context, Set<AppContextRouter.Topic> topics) {
        Context app = context.getApplicationContext();
        AppSnapshot snapshot = new AppSnapshot();
        String userId = userId(app);
        boolean all = topics.contains(AppContextRouter.Topic.SUMMARY);
        if (all || topics.contains(AppContextRouter.Topic.GAMES)) readGames(app, userId, snapshot);
        if (all || topics.contains(AppContextRouter.Topic.BIBLE)) readBible(app, snapshot);
        if (all || topics.contains(AppContextRouter.Topic.SERMONS)) readSermons(app, snapshot);
        if (all || topics.contains(AppContextRouter.Topic.COMMUNITY)) readCommunity(app, userId, snapshot);
        if (all || topics.contains(AppContextRouter.Topic.ROUTINE)) readRoutine(app, userId, snapshot);
        return snapshot;
    }

    private static void readGames(Context app, String userId, AppSnapshot snapshot) {
        try {
            SharedPreferences scores = app.getSharedPreferences(SCORES_PREFS, Context.MODE_PRIVATE);
            String owner = GamesHubDialog.ownerKey(userId);
            LevelProgression progress = new LevelProgression(new ReadOnlyStore(scores));
            for (GameDefinition game : GameCatalog.all()) {
                String stored = owner + game.id();
                int levels = Math.max(1, game.levelCount());
                int best = 0;
                for (int level = 1; level <= levels; level++) {
                    best = Math.max(best, progress.bestStars(stored, level));
                }
                snapshot.games.add(new AppSnapshot.Game(game.title(), best, progress.unlockedLevel(stored), levels));
            }
        } catch (RuntimeException ignored) {
            // Game progress unavailable: the answer leaves the games part out.
        }
        try {
            List<AppSnapshot.MissedQuestion> missed = new ArrayList<>();
            for (Map.Entry<String, Map<String, ReviewScheduler.Record>> game
                    : QuestionHistoryStore.loadAll(app).entrySet()) {
                for (Map.Entry<String, ReviewScheduler.Record> question : game.getValue().entrySet()) {
                    ReviewScheduler.Record record = question.getValue();
                    if (record.missed > 0 && record.missed >= record.correct) {
                        missed.add(new AppSnapshot.MissedQuestion(gameLabel(game.getKey()), question.getKey(),
                                record.missed, record.correct));
                    }
                }
            }
            Collections.sort(missed, new Comparator<AppSnapshot.MissedQuestion>() {
                @Override public int compare(AppSnapshot.MissedQuestion a, AppSnapshot.MissedQuestion b) {
                    return b.missed != a.missed ? b.missed - a.missed : a.correct - b.correct;
                }
            });
            for (int i = 0; i < missed.size() && i < MAX_MISSED; i++) snapshot.missedQuestions.add(missed.get(i));
        } catch (RuntimeException ignored) {
            // Question history unavailable: no missed-question coaching this time.
        }
    }

    private static void readBible(Context app, AppSnapshot snapshot) {
        PerazimDatabase db = PerazimDatabase.getInstance(app);
        Map<String, BibleBookEntity> booksById = new HashMap<>();
        Map<Integer, BibleBookEntity> booksByNumber = new HashMap<>();
        try {
            SharedPreferences reader = app.getSharedPreferences(BIBLE_READER_PREFS, Context.MODE_PRIVATE);
            String translation = reader.getString("translation", DEFAULT_TRANSLATION);
            for (BibleBookEntity book : db.bibleDao().getBooksByTranslation(translation)) {
                booksById.put(book.getId(), book);
                booksByNumber.put(book.getBookNumber(), book);
            }
            String bookId = reader.getString("book", null);
            if (bookId != null && booksById.containsKey(bookId)) {
                snapshot.lastReading = new AppSnapshot.Passage(bookId, booksById.get(bookId).getName(),
                        reader.getInt("chapter", 1), 0);
            }
        } catch (RuntimeException ignored) {
            // Reader position unavailable: the answer leaves it out.
        }
        try {
            snapshot.chaptersToday = ReadingLog.chaptersToday(app);
        } catch (RuntimeException ignored) {
            snapshot.chaptersToday = 0;
        }
        try {
            List<BibleNoteEntity> notes = db.bibleNoteDao().getAllNotes();
            snapshot.noteCount = notes.size();
            if (!notes.isEmpty()) {
                BibleNoteEntity latest = notes.get(0);
                snapshot.latestNotePreview = latest.getBody() == null ? "" : latest.getBody().trim();
                for (BibleNoteLinkEntity link : db.bibleNoteDao().getAllLinks()) {
                    if (link.getNoteId() == latest.getId()) {
                        snapshot.latestNotePassage = passage(booksByNumber, link.getBookNumber(),
                                link.getChapterNumber(), link.getVerseNumber());
                        break;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Notes unavailable: the answer leaves them out.
        }
        try {
            snapshot.highlightCount = db.bibleDao().getAllHighlights().size();
        } catch (RuntimeException ignored) {
            snapshot.highlightCount = 0;
        }
        try {
            List<BibleVerseEntity> saved = db.bibleDao().getBookmarkedVerses();
            snapshot.favouriteCount = saved.size();
            for (int i = 0; i < saved.size() && i < MAX_MISSED; i++) {
                BibleVerseEntity verse = saved.get(i);
                AppSnapshot.Passage p = passage(booksByNumber, verse.getBookNumber(),
                        verse.getChapterNumber(), verse.getVerseNumber());
                if (p != null) snapshot.favouriteVerses.add(p);
            }
        } catch (RuntimeException ignored) {
            snapshot.favouriteCount = 0;
        }
    }

    private static void readSermons(Context app, AppSnapshot snapshot) {
        try {
            List<SermonEntity> sermons = PerazimDatabase.getInstance(app).sermonDao().getActiveSermons();
            for (int i = 0; i < sermons.size() && i < MAX_MISSED; i++) {
                SermonEntity sermon = sermons.get(i);
                String title = sermon.getTitle() == null ? "" : sermon.getTitle().trim();
                if (!title.isEmpty()) snapshot.recentSermons.add(title);
            }
        } catch (RuntimeException ignored) {
            // Sermon list unavailable: the answer leaves it out.
        }
        try {
            snapshot.savedSermonCount = PerazimDatabase.getInstance(app).sermonDao().getDownloadedSermons().size();
        } catch (RuntimeException ignored) {
            snapshot.savedSermonCount = 0;
        }
    }

    /**
     * Public community content only: posts with no group, the groups this member has joined, and upcoming events.
     * Group posts, messages and conversations are not read.
     */
    private static void readCommunity(Context app, String userId, AppSnapshot snapshot) {
        PerazimDatabase db = PerazimDatabase.getInstance(app);
        try {
            List<PostEntity> posts = db.postDao().getAllPublicPosts();
            for (PostEntity post : posts) {
                if (!userId.isEmpty() && userId.equals(post.getAuthorId())) snapshot.myPostCount++;
            }
            for (int i = 0; i < posts.size() && snapshot.recentPublicPosts.size() < 2; i++) {
                String title = posts.get(i).getTitle();
                if (title != null && !title.trim().isEmpty()) snapshot.recentPublicPosts.add(title.trim());
            }
        } catch (RuntimeException ignored) {
            // Public posts unavailable: the answer leaves them out.
        }
        try {
            for (GroupEntity group : db.groupDao().getAllGroups()) {
                if (group.isMember() && group.getName() != null) snapshot.joinedGroups.add(group.getName().trim());
            }
        } catch (RuntimeException ignored) {
            // Groups unavailable: the answer leaves them out.
        }
        try {
            SimpleDateFormat day = new SimpleDateFormat("d MMM", Locale.ENGLISH);
            day.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
            List<EventEntity> events = db.eventDao().getUpcomingEvents(System.currentTimeMillis());
            for (int i = 0; i < events.size() && i < 2; i++) {
                EventEntity event = events.get(i);
                snapshot.upcomingEvents.add(event.getTitle() + " (" + day.format(event.getStartTimeMillis()) + ")");
            }
        } catch (RuntimeException ignored) {
            // Events unavailable: the answer leaves them out.
        }
    }

    private static void readRoutine(Context app, String userId, AppSnapshot snapshot) {
        try {
            snapshot.streak = GamificationStore.getStreak(app, userId, 0);
            snapshot.lifetimeRoutineDays = GamificationStore.getLifetimeRoutineDays(app, userId);
            snapshot.routineDoneToday = GamificationStore.isRoutineCompletedToday(app, userId);
        } catch (RuntimeException ignored) {
            // Routine unavailable: the answer leaves it out.
        }
    }

    private static AppSnapshot.Passage passage(Map<Integer, BibleBookEntity> booksByNumber, int bookNumber,
                                               int chapter, int verse) {
        BibleBookEntity book = booksByNumber.get(bookNumber);
        if (book == null) return null;
        return new AppSnapshot.Passage(book.getId(), book.getName(), chapter, verse);
    }

    private static String gameLabel(String storedGame) {
        return "ladder".equals(storedGame) ? "Ladder of Faith" : storedGame;
    }

    private static String userId(Context app) {
        try {
            String id = SessionManager.getInstance(app).getActiveUserId();
            return id == null ? "" : id;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
    }

    /** Reads stored level progress; nothing is written from the guide. */
    private static final class ReadOnlyStore implements KeyValueStore {
        private final SharedPreferences prefs;

        ReadOnlyStore(SharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override public int getInt(String key, int defaultValue) {
            return prefs.getInt(key, defaultValue);
        }

        @Override public void putInt(String key, int value) {
            // Read-only: the guide never changes game progress.
        }
    }
}
