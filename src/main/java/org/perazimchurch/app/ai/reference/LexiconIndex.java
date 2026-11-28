package org.perazimchurch.app.ai.reference;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Read-only access to the bundled lexicon ({@code assets/reference/lexicon-core.db.gz}, built by
 * {@code scripts/hermeneutics/build_core.py}). Every query is limited to lexicon sources in {@link ReferenceApprovals},
 * so an unapproved source is never returned. The ESV name forms in the names table are never read here.
 */
public final class LexiconIndex {

    private static final String TAG = "LexiconIndex";
    static final String ASSET = "reference/lexicon-core.db.gz";
    /** The build can store the same file already decompressed under this name (as it does for reference.db). */
    static final String ASSET_PLAIN = "reference/lexicon-core.db";
    /** Bump when the bundled database changes, so the unpacked copy is rebuilt. */
    static final String UNPACKED_NAME = "lexicon-core-v2.db";
    static final String BDB = "bdb-openscriptures";
    static final String TBESG = "step-tbesg";
    static final String TBESH = "step-tbesh";
    /**
     * The only sources this index may return. Names, verse maps and token tables are not lexicons.
     * TBESH is left out of the release pack until Online Bible permission is confirmed (Q6).
     */
    static final List<String> SOURCES = Arrays.asList(TBESG, BDB);

    private final SQLiteDatabase db;

    private LexiconIndex(SQLiteDatabase db) {
        this.db = db;
    }

    /** Returns the index, or null when the bundled database is missing or cannot be opened. */
    public static LexiconIndex open(Context context) {
        if (context == null) return null;
        try {
            File file = unpack(context);
            if (file == null) return null;
            return new LexiconIndex(SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY));
        } catch (Exception e) {
            Log.w(TAG, "Lexicon index unavailable", e);
            return null;
        }
    }

    private static File unpack(Context context) throws Exception {
        File dir = new File(context.getFilesDir(), "lexicon");
        File target = new File(dir, UNPACKED_NAME);
        if (target.isFile() && target.length() > 0) return target;
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        try (InputStream in = openAsset(context)) {
            File temp = new File(dir, UNPACKED_NAME + ".tmp");
            try (OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }
            if (!temp.renameTo(target)) throw new IllegalStateException("Could not place the lexicon database");
        } catch (FileNotFoundException missingAsset) {
            return null;
        }
        return target;
    }

    /**
     * Opens the bundled database, gunzipping it when the gzipped asset is present. Otherwise reads the
     * same file stored without the .gz suffix. A missing asset throws FileNotFoundException.
     */
    private static InputStream openAsset(Context context) throws java.io.IOException {
        try {
            return new GZIPInputStream(new BufferedInputStream(context.getAssets().open(ASSET)));
        } catch (FileNotFoundException gzippedMissing) {
            return new BufferedInputStream(context.getAssets().open(ASSET_PLAIN));
        }
    }

    /** Lexicon sources that are both lexicons and approved. */
    static List<String> approvedSources(ReferenceApprovals approvals) {
        List<String> ids = new ArrayList<>();
        if (approvals == null) return ids;
        for (String id : SOURCES) if (approvals.isApproved(id)) ids.add(id);
        return ids;
    }

    /** Entries for one key (a Strong's code or an exact original-script lemma) from approved lexicons only. */
    public List<ReferenceHit> lookup(LexiconKeys.Key key, ReferenceApprovals approvals, int limit) {
        List<ReferenceHit> out = new ArrayList<>();
        if (db == null || key == null || approvals == null || limit <= 0) return out;
        List<String> ids = approvedSources(approvals);
        if (ids.isEmpty()) return out;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) in.append(i == 0 ? "?" : ",?");
        List<String> args = new ArrayList<>();
        String match;
        if (key.strongs) {
            match = "(e.entry_id = ? OR e.entry_id LIKE ?)";
            args.add(key.value);
            args.add(key.value + "%");
        } else {
            match = "e.lemma = ?";
            args.add(key.value);
        }
        args.addAll(ids);
        args.add(String.valueOf(limit));
        String sql = "SELECT e.source_id, e.entry_id, e.lemma, e.gloss, e.definition, s.licence "
                + "FROM lexicon_entries e JOIN sources s ON s.source_id = e.source_id "
                + "WHERE " + match + " AND e.source_id IN (" + in + ") "
                + "ORDER BY e.source_id, e.entry_id LIMIT ?";
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                String sourceId = c.getString(0);
                String entryId = c.getString(1);
                String lemma = c.getString(2);
                String gloss = c.getString(3);
                String definition = c.getString(4);
                // Show G26, not the padded G0026 used in the source. Only plain numbers are changed.
                String shownId = entryId.replaceFirst("^([GH])0+(\\d+)$", "$1$2");
                String headword = shownId + (lemma == null || lemma.isEmpty() ? "" : " " + lemma);
                String text = "Meaning: " + (gloss == null ? "" : gloss) + ". "
                        + (definition == null ? "" : definition);
                out.add(new ReferenceHit(ReferenceHit.KIND_LEXICON, sourceId, title(sourceId), author(sourceId),
                        c.getString(5), "", headword, text, 0));
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Lexicon lookup failed", e);
        }
        return out;
    }

    static String title(String sourceId) {
        if (TBESG.equals(sourceId)) return "Translators Brief Lexicon, Greek (STEP Bible)";
        if (TBESH.equals(sourceId)) return "Translators Brief Lexicon, Hebrew (STEP Bible)";
        if (BDB.equals(sourceId)) return "Brown-Driver-Briggs Hebrew Lexicon (OpenScriptures)";
        return "Lexicon";
    }

    static String author(String sourceId) {
        if (TBESG.equals(sourceId) || TBESH.equals(sourceId)) return "STEPBible.org, Tyndale House Cambridge";
        if (BDB.equals(sourceId)) return "OpenScriptures Hebrew Lexicon project";
        return "";
    }

    public void close() {
        if (db != null && db.isOpen()) db.close();
    }
}
