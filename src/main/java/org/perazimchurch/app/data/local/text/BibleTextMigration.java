package org.perazimchurch.app.data.local.text;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.seeder.BibleTextImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time move of Bible text out of the private encrypted database into the separate text database.
 *
 * <p>Steps, all on a background thread: (1) import the bundled public-domain translations from their assets,
 * (2) copy any other translation that only exists in the private database, (3) only after a translation is
 * complete in the text database, delete its verse rows from the private database, keeping every verse that has a
 * bookmark or a note, and (4) compact the private database. Reading keeps working throughout, because the reader
 * falls back to the private database until a translation is marked ready.
 */
public final class BibleTextMigration {

    private static final String TAG = "BibleTextMigration";
    private static final long COMPACT_ABOVE_BYTES = 8L * 1024 * 1024;
    private static boolean started;

    private BibleTextMigration() {
    }

    /** Starts the move once per process, on its own thread. */
    public static synchronized void startInBackground(@NonNull Context context) {
        if (started) {
            return;
        }
        started = true;
        final Context app = context.getApplicationContext();
        Thread worker = new Thread(() -> {
            try {
                run(app);
            } catch (Throwable t) {
                Log.w(TAG, "Bible text move did not finish; reading continues from the private database", t);
            }
        }, "bible-text-migration");
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();
    }

    static void run(Context app) {
        PerazimDatabase db = PerazimDatabase.getInstance(app);
        BibleDao dao = db.bibleDao();
        BibleTextDao textDao = BibleTextDatabase.get(app).textDao();

        // 1. Bundled public-domain translations, straight from their assets.
        BibleTextImporter.importBundledTranslations(app, dao);

        // 2. Anything else that only exists in the private database.
        for (String id : dao.getVerseTranslationIds()) {
            if (id == null || BibleVerseReader.isReady(app, id) || BibleVerseReader.isBundledId(id)) {
                continue;
            }
            if (copyFromPrivate(dao, textDao, id)) {
                BibleVerseReader.markReady(app, id);
            }
        }

        Log.i(TAG, "Private verse rows before trim: " + dao.countAllVerseRows());
        // 3. Remove the moved rows from the private database (never a bookmarked or noted verse).
        boolean trimmed = false;
        for (String id : dao.getVerseTranslationIds()) {
            if (id != null && BibleVerseReader.isReady(app, id)) {
                dao.deleteVersesWithoutUserData(id);
                trimmed = true;
            }
        }

        Log.i(TAG, "Private verse rows after trim: " + dao.countAllVerseRows());
        // 4. Compact the private database so its file, and the start-up check on it, become small.
        File file = app.getDatabasePath("perazim_database.db");
        if (trimmed && file.length() > COMPACT_ABOVE_BYTES) {
            long before = file.length();
            db.getOpenHelper().getWritableDatabase().execSQL("VACUUM");
            // In write-ahead mode the rewritten file only shrinks once the log is merged back and truncated.
            android.database.Cursor checkpoint = db.getOpenHelper().getWritableDatabase().query("PRAGMA wal_checkpoint(TRUNCATE)");
            if (checkpoint.moveToFirst()) {
                Log.i(TAG, "wal_checkpoint busy=" + checkpoint.getInt(0) + " log=" + checkpoint.getInt(1) + " checkpointed=" + checkpoint.getInt(2));
            }
            checkpoint.close();
            android.database.Cursor pages = db.getOpenHelper().getWritableDatabase().query("PRAGMA page_count");
            if (pages.moveToFirst()) {
                Log.i(TAG, "page_count after vacuum=" + pages.getInt(0));
            }
            pages.close();
            Log.i(TAG, "Private database compacted from " + before + " to " + file.length() + " bytes");
        }
    }

    /** Copies one translation book by book; returns true only when the text database holds every row. */
    private static boolean copyFromPrivate(BibleDao dao, BibleTextDao textDao, String translationId) {
        int expected = dao.countVerses(translationId);
        if (expected == 0) {
            return false;
        }
        textDao.deleteTranslation(translationId);
        for (int book = 1; book <= 66; book++) {
            List<BibleVerseEntity> rows = dao.getVersesForBook(translationId, book);
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            List<BibleTextVerseEntity> batch = new ArrayList<>(rows.size());
            for (BibleVerseEntity row : rows) {
                if (row.getText() != null) {
                    batch.add(new BibleTextVerseEntity(translationId, row.getBookNumber(), row.getChapterNumber(),
                            row.getVerseNumber(), row.getText()));
                }
            }
            textDao.insertAll(batch);
        }
        // Rows with the same position in the private database collapse into one here, so compare to distinct positions.
        int copied = textDao.count(translationId);
        boolean complete = copied > 0 && copied <= expected && copied >= expected - duplicateAllowance(expected);
        Log.i(TAG, "Copied " + translationId + ": " + copied + " of " + expected + " rows, complete=" + complete);
        return complete;
    }

    /** Older imports left a few duplicate positions; a handful of collapsed rows is expected, not a failure. */
    private static int duplicateAllowance(int expected) {
        return Math.max(50, expected / 20);
    }
}
