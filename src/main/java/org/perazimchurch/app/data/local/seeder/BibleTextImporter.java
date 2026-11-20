package org.perazimchurch.app.data.local.seeder;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.text.BibleTextDao;
import org.perazimchurch.app.data.local.text.BibleTextDatabase;
import org.perazimchurch.app.data.local.text.BibleTextVerseEntity;
import org.perazimchurch.app.data.local.text.BibleVerseReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Imports the full public-domain Bible texts bundled in {@code assets/bible/text/}.
 *
 * <p>Each asset is a gzip-compressed TSV file ({@code bookNumber, chapter, verse, text}) built by
 * {@code scripts/bible/build_bible_text.py} from the eBible.org and Berean USFM releases.
 * Import is idempotent: a translation is imported only while its stored verse count is below the
 * expected count, and existing rows are never replaced, so bookmarks, notes and highlights survive.
 */
public final class BibleTextImporter {

    private static final String TAG = "BibleTextImporter";
    static final String ASSET_DIR = "bible/text/";
    static final int BATCH_SIZE = 1000;

    /** One bundled translation: its id, display metadata and the verse count its source publishes. */
    public static final class Spec {
        public final String id;
        public final String name;
        public final String language;
        public final String description;
        public final String licence;
        public final int expectedVerses;
        /** True for texts that are never bundled in the app and are read from a local file instead. */
        public final boolean local;

        Spec(String id, String name, String description, String licence, int expectedVerses) {
            this(id, name, description, licence, expectedVerses, false);
        }

        Spec(String id, String name, String description, String licence, int expectedVerses, boolean local) {
            this.id = id;
            this.name = name;
            this.language = "en";
            this.description = description;
            this.licence = licence;
            this.expectedVerses = expectedVerses;
            this.local = local;
        }
    }

    /**
     * Texts that are not in the repository. The owner copies the file to the app's external
     * files folder ({@code Android/data/org.perazimchurch.app/files/bible/<ID>.tsv.gz}) and the
     * importer reads it from there. Used for licensed texts such as the NASB.
     */
    public static final List<Spec> LOCAL = Collections.unmodifiableList(java.util.Arrays.asList(
            new Spec("NASB", "New American Standard Bible",
                    "New American Standard Bible. Scripture taken from the NEW AMERICAN STANDARD BIBLE, "
                            + "Copyright \u00a9 1960,1962,1963,1968,1971,1972,1973,1975,1977,1995 by The Lockman Foundation. Used by permission.",
                    "Licensed: The Lockman Foundation (local copy, not in the repository)", 31102, true)
    ));

    /** Bundled translations, in the order they appear in the reader's version picker. */
    public static final List<Spec> BUNDLED = Collections.unmodifiableList(java.util.Arrays.asList(
            new Spec("KJV", "King James Version", "Authorized King James Version (1611/1769)", "Public Domain", 31102),
            new Spec("BSB", "Berean Standard Bible", "Berean Standard Bible (2020, Public Domain dedication 2023)",
                    "Public Domain", 31086),
            new Spec("WEB", "World English Bible", "World English Bible (modern English, Public Domain)",
                    "Public Domain (eBible.org)", 31098),
            new Spec("ASV", "American Standard Version", "American Standard Version (1901)", "Public Domain", 31086)
    ));

    private BibleTextImporter() {
    }

    /**
     * Imports every bundled translation that is not yet in the separate text database. Safe to call repeatedly.
     *
     * @param context Application context used to open assets; no-op when null
     * @param dao     Bible DAO of the private database (translation and book names only)
     */
    public static synchronized void importBundledTranslations(@Nullable Context context, @Nullable BibleDao dao) {
        if (context == null || dao == null) {
            return;
        }
        BibleTextDao textDao = BibleTextDatabase.get(context).textDao();
        List<Spec> all = new ArrayList<>(BUNDLED);
        all.addAll(LOCAL);
        for (Spec spec : all) {
            try {
                importTranslation(context, dao, textDao, spec);
            } catch (Exception e) {
                // A broken or missing asset must not stop the reader from opening.
                Log.w(TAG, "Could not import bundled translation " + spec.id, e);
            }
        }
    }

    /** Returns true when the translation already has all verses its source publishes in the text database. */
    public static boolean isComplete(@NonNull BibleTextDao textDao, @NonNull Spec spec) {
        return textDao.count(spec.id) >= spec.expectedVerses;
    }

    private static void importTranslation(Context context, BibleDao dao, BibleTextDao textDao, Spec spec) throws IOException {
        if (BibleVerseReader.isReady(context, spec.id) && isComplete(textDao, spec)) {
            return;
        }
        if (isComplete(textDao, spec)) {
            ensureTranslationAndBooks(dao, spec);
            BibleVerseReader.markReady(context, spec.id);
            return;
        }
        InputStream source = openSource(context, spec);
        if (source == null) {
            return;
        }
        ensureTranslationAndBooks(dao, spec);
        textDao.deleteTranslation(spec.id);

        List<BibleTextVerseEntity> batch = new ArrayList<>(BATCH_SIZE);
        int imported = 0;
        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Row row = parseLine(line);
                if (row == null || row.bookNumber < 1 || row.bookNumber > BibleDataSeeder.CANONICAL_BOOKS.length) {
                    continue;
                }
                batch.add(new BibleTextVerseEntity(spec.id, row.bookNumber, row.chapter, row.verse, row.text));
                if (batch.size() >= BATCH_SIZE) {
                    textDao.insertAll(batch);
                    imported += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            textDao.insertAll(batch);
            imported += batch.size();
        }
        // Only trust the text database for this translation once every verse is in.
        if (isComplete(textDao, spec)) {
            BibleVerseReader.markReady(context, spec.id);
        }
        Log.i(TAG, "Imported " + imported + " verses for " + spec.id + " into the text database");
    }

    /** Opens the bundled asset, or the owner's local file for local texts. Returns null when absent. */
    @Nullable
    private static InputStream openSource(Context context, Spec spec) throws IOException {
        if (!spec.local) {
            return BibleAssets.openText(context, ASSET_DIR + spec.id + ".tsv.gz");
        }
        File dir = context.getExternalFilesDir("bible");
        File file = dir == null ? null : new File(dir, spec.id + ".tsv.gz");
        if (file == null || !file.isFile()) {
            return null;
        }
        return new FileInputStream(file);
    }

    private static void ensureTranslationAndBooks(BibleDao dao, Spec spec) {
        if (dao.getTranslationById(spec.id) == null) {
            dao.insertTranslation(new BibleTranslationEntity(
                    spec.id, spec.id, spec.name, spec.language, spec.description, false, spec.licence));
        }
        if (dao.getBooksByTranslation(spec.id).isEmpty()) {
            List<BibleBookEntity> books = new ArrayList<>(66);
            for (int i = 0; i < BibleDataSeeder.CANONICAL_BOOKS.length; i++) {
                Object[] meta = BibleDataSeeder.CANONICAL_BOOKS[i];
                books.add(new BibleBookEntity((String) meta[0], spec.id, i + 1,
                        (String) meta[1], (String) meta[2], (Integer) meta[3]));
            }
            dao.insertBooks(books);
        }
    }

    /**
     * Parses one TSV line. Returns null for blank or malformed lines so a damaged line
     * never aborts the whole import.
     */
    @Nullable
    public static Row parseLine(@Nullable String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] parts = line.split("\t", 4);
        if (parts.length != 4) {
            return null;
        }
        try {
            int book = Integer.parseInt(parts[0].trim());
            int chapter = Integer.parseInt(parts[1].trim());
            int verse = Integer.parseInt(parts[2].trim());
            String text = parts[3].trim();
            if (text.isEmpty()) {
                return null;
            }
            return new Row(book, chapter, verse, text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    public static BibleVerseEntity toEntity(Spec spec, Row row) {
        if (row.bookNumber < 1 || row.bookNumber > BibleDataSeeder.CANONICAL_BOOKS.length) {
            return null;
        }
        String bookId = (String) BibleDataSeeder.CANONICAL_BOOKS[row.bookNumber - 1][0];
        // The KJV keeps the id scheme of the starter passages ("GEN.1.1") so those rows are reused.
        String prefix = "KJV".equals(spec.id) ? "" : spec.id + ".";
        String id = prefix + bookId + "." + row.chapter + "." + row.verse;
        String chapterId = bookId + "." + row.chapter;
        return new BibleVerseEntity(id, spec.id, chapterId, row.bookNumber, row.chapter, row.verse,
                row.text, false, null);
    }

    /** One parsed TSV line. */
    public static final class Row {
        public final int bookNumber;
        public final int chapter;
        public final int verse;
        public final String text;

        public Row(int bookNumber, int chapter, int verse, String text) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
        }
    }
}
