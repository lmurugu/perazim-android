package org.perazimchurch.app.data.local.text;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads verse text. Verses come from the separate text database once a translation has been moved there,
 * and from the private database before that, so reading never stops during the move.
 */
public final class BibleVerseReader {

    private static final String PREFS = "bible_text_db";

    private final BibleDao mainDao;
    @Nullable
    private final BibleTextDao textDao;
    @Nullable
    private final Context context;

    private BibleVerseReader(BibleDao mainDao, @Nullable BibleTextDao textDao, @Nullable Context context) {
        this.mainDao = mainDao;
        this.textDao = textDao;
        this.context = context;
    }

    /** Reader that uses the text database when a context is available. */
    public static BibleVerseReader create(@Nullable Context context, @NonNull BibleDao mainDao) {
        if (context == null) {
            return new BibleVerseReader(mainDao, null, null);
        }
        Context app = context.getApplicationContext();
        return new BibleVerseReader(mainDao, BibleTextDatabase.get(app).textDao(), app);
    }

    /** Reader over the private database only (used by tests). */
    public static BibleVerseReader mainOnly(@NonNull BibleDao mainDao) {
        return new BibleVerseReader(mainDao, null, null);
    }

    public static boolean isReady(@Nullable Context context, String translationId) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("ready_" + translationId, false);
    }

    public static void markReady(Context context, String translationId) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("ready_" + translationId, true).apply();
    }

    private boolean useText(String translationId) {
        return textDao != null && isReady(context, translationId);
    }

    public List<BibleVerseEntity> getVerses(String translationId, int bookNumber, int chapterNumber) {
        if (useText(translationId)) {
            return toEntities(textDao.getChapter(translationId, bookNumber, chapterNumber));
        }
        return mainDao.getVerses(translationId, bookNumber, chapterNumber);
    }

    @Nullable
    public BibleVerseEntity getVerse(String translationId, int bookNumber, int chapterNumber, int verseNumber) {
        if (useText(translationId)) {
            BibleTextVerseEntity row = textDao.getVerse(translationId, bookNumber, chapterNumber, verseNumber);
            return row == null ? null : toEntity(row);
        }
        return mainDao.getVerse(translationId, bookNumber, chapterNumber, verseNumber);
    }

    public List<BibleVerseEntity> search(String translationId, String query) {
        if (useText(translationId)) {
            return toEntities(textDao.search(translationId, query));
        }
        return mainDao.searchVerses(translationId, query);
    }

    public List<BibleVerseEntity> searchTerms(String translationId, String first, String second) {
        if (useText(translationId)) {
            return toEntities(textDao.searchTerms(translationId, first, second));
        }
        return mainDao.searchVersesByTerms(translationId, first, second);
    }

    /** Number of verses of a translation, whichever database holds them. */
    public int count(String translationId) {
        if (useText(translationId)) {
            return textDao.count(translationId);
        }
        return mainDao.countVerses(translationId);
    }

    /** Same shape the repository expects: an entity without personal data (not bookmarked, no note). */
    private static BibleVerseEntity toEntity(BibleTextVerseEntity row) {
        String bookId = org.perazimchurch.app.data.local.seeder.BibleDataSeeder.canonicalBookId(row.getBookNumber());
        String prefix = "KJV".equals(row.getTranslationId()) ? "" : row.getTranslationId() + ".";
        String id = prefix + bookId + "." + row.getChapterNumber() + "." + row.getVerseNumber();
        return new BibleVerseEntity(id, row.getTranslationId(), bookId + "." + row.getChapterNumber(),
                row.getBookNumber(), row.getChapterNumber(), row.getVerseNumber(), row.getText(), false, null);
    }

    private static List<BibleVerseEntity> toEntities(@Nullable List<BibleTextVerseEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<BibleVerseEntity> list = new ArrayList<>(rows.size());
        for (BibleTextVerseEntity row : rows) {
            list.add(toEntity(row));
        }
        return list;
    }
}
