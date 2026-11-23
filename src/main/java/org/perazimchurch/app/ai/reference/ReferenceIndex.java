package org.perazimchurch.app.ai.reference;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Read-only access to the bundled reference database ({@code assets/reference/reference.db.gz}).
 *
 * The compressed asset is unpacked once into the app's files directory and opened read-only. Every query is
 * restricted to sources in {@link ReferenceApprovals}, so an unapproved source is never returned. When the asset
 * is absent or unreadable, {@link #open} returns null and the guide behaves exactly as before.
 */
public final class ReferenceIndex {

    private static final String TAG = "ReferenceIndex";
    static final String ASSET = "reference/reference.db.gz";
    /** Bump when the database format or contents change, so the unpacked copy is rebuilt. */
    static final String UNPACKED_NAME = "reference-v2.db";
    static final String CROSS_REFERENCE_SOURCE = "openbible-xrefs";
    /** Commentaries are in their own database so the factual index stays small. */
    static final String COMMENTARY_ASSET = "reference/reference-commentaries.db.gz";
    static final String COMMENTARY_UNPACKED = "reference-commentaries-v1.db";
    static final String HENRY_SOURCE = "matthew-henry-commentary";
    static final String WESLEY_SOURCE = "wesley-notes-nt";
    static final List<String> COMMENTARY_SOURCES = java.util.Arrays.asList(HENRY_SOURCE, WESLEY_SOURCE);

    private final SQLiteDatabase db;
    private final SQLiteDatabase commentaries;

    private ReferenceIndex(SQLiteDatabase db, SQLiteDatabase commentaries) {
        this.db = db;
        this.commentaries = commentaries;
    }

    /** Returns the index, or null when the bundled database is missing or cannot be opened. */
    public static ReferenceIndex open(Context context) {
        if (context == null) return null;
        try {
            File file = unpack(context, ASSET, UNPACKED_NAME);
            if (file == null) return null;
            SQLiteDatabase db = openReadOnly(file);
            SQLiteDatabase commentaryDb = null;
            try {
                File commentaryFile = unpack(context, COMMENTARY_ASSET, COMMENTARY_UNPACKED);
                if (commentaryFile != null) commentaryDb = openReadOnly(commentaryFile);
            } catch (Exception e) {
                Log.w(TAG, "Commentaries unavailable", e);
            }
            return new ReferenceIndex(db, commentaryDb);
        } catch (Exception e) {
            Log.w(TAG, "Reference index unavailable", e);
            return null;
        }
    }

    private static SQLiteDatabase openReadOnly(File file) {
        return SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private static File unpack(Context context, String asset, String unpackedName) throws Exception {
        File dir = new File(context.getFilesDir(), "reference");
        File target = new File(dir, unpackedName);
        if (target.isFile() && target.length() > 0) return target;
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        try (InputStream raw = context.getAssets().open(asset);
             InputStream in = new GZIPInputStream(new BufferedInputStream(raw))) {
            File temp = new File(dir, unpackedName + ".tmp");
            try (OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            }
            if (!temp.renameTo(target)) {
                throw new IllegalStateException("Could not place the reference database");
            }
        } catch (java.io.FileNotFoundException missingAsset) {
            return null;
        }
        return target;
    }

    /** Dictionary entries whose headword matches exactly (case-insensitive), from approved sources only. */
    public List<ReferenceHit> dictionary(String headword, ReferenceApprovals approvals, int limit) {
        return headwordEntries(headword, approvals, limit, "ORDER BY e.source_id");
    }

    /**
     * Every stored part of a multi-part text (such as a creed) under one headword, in stored order, from approved
     * sources only. Returns at most {@code limit} parts.
     */
    public List<ReferenceHit> parts(String headword, ReferenceApprovals approvals, int limit) {
        return headwordEntries(headword, approvals, limit, "ORDER BY e.source_id, e.id");
    }

    private List<ReferenceHit> headwordEntries(String headword, ReferenceApprovals approvals, int limit, String order) {
        List<ReferenceHit> out = new ArrayList<>();
        if (db == null || headword == null || approvals == null || approvals.isEmpty() || limit <= 0) return out;
        List<String> ids = new ArrayList<>(approvals.ids());
        ids.remove(CROSS_REFERENCE_SOURCE);
        if (ids.isEmpty()) return out;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) in.append(i == 0 ? "?" : ",?");
        List<String> args = new ArrayList<>();
        args.add(headword.trim());
        args.addAll(ids);
        args.add(String.valueOf(limit));
        String sql = "SELECT e.source_id, e.headword, e.body, s.title, s.author, s.licence, s.theology_tag "
                + "FROM entries e JOIN sources s ON s.source_id = e.source_id "
                + "WHERE e.headword = ? COLLATE NOCASE AND e.source_id IN (" + in + ") "
                + order + " LIMIT ?";
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                out.add(new ReferenceHit(ReferenceHit.KIND_DICTIONARY, c.getString(0), c.getString(3),
                        c.getString(4), c.getString(5), c.getString(6), c.getString(1), c.getString(2), 0));
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Dictionary lookup failed", e);
        }
        return out;
    }

    /** Cross-references for a verse key, strongest first. Empty unless the cross-reference source is approved. */
    public List<ReferenceHit> crossReferences(int fromKey, ReferenceApprovals approvals, int limit) {
        List<ReferenceHit> out = new ArrayList<>();
        if (db == null || approvals == null || !approvals.isApproved(CROSS_REFERENCE_SOURCE)
                || fromKey <= 0 || limit <= 0) {
            return out;
        }
        String from = ReferenceKeys.format(fromKey);
        String sql = "SELECT x.to_start, x.to_end, x.votes, s.title, s.author, s.licence, s.theology_tag "
                + "FROM xrefs x JOIN sources s ON s.source_id = ? WHERE x.from_key = ? "
                + "ORDER BY x.votes DESC, x.to_start LIMIT ?";
        try (Cursor c = db.rawQuery(sql, new String[]{CROSS_REFERENCE_SOURCE, String.valueOf(fromKey),
                String.valueOf(limit)})) {
            while (c.moveToNext()) {
                int start = c.getInt(0);
                int end = c.getInt(1);
                String target = ReferenceKeys.format(start);
                if (end != start) target = target + "-" + ReferenceKeys.format(end);
                out.add(new ReferenceHit(ReferenceHit.KIND_CROSS_REFERENCE, CROSS_REFERENCE_SOURCE, c.getString(3),
                        c.getString(4), c.getString(5), c.getString(6), from, target, c.getInt(2)));
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cross-reference lookup failed", e);
        }
        return out;
    }

    /**
     * Commentary excerpts for one chapter (for example "Genesis 1"), from approved commentaries only. Each hit is
     * labelled with its author and tradition by {@link ReferenceEvidence}.
     */
    public List<ReferenceHit> commentaries(String chapterHeadword, ReferenceApprovals approvals, int limit) {
        List<ReferenceHit> out = new ArrayList<>();
        if (commentaries == null || chapterHeadword == null || chapterHeadword.trim().isEmpty()
                || approvals == null || limit <= 0) {
            return out;
        }
        List<String> ids = new ArrayList<>();
        for (String id : COMMENTARY_SOURCES) if (approvals.isApproved(id)) ids.add(id);
        if (ids.isEmpty()) return out;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) in.append(i == 0 ? "?" : ",?");
        List<String> args = new ArrayList<>();
        args.add(chapterHeadword.trim());
        args.addAll(ids);
        args.add(String.valueOf(limit));
        String sql = "SELECT e.source_id, e.headword, e.body, s.title, s.author, s.licence, s.theology_tag "
                + "FROM entries e JOIN sources s ON s.source_id = e.source_id "
                + "WHERE e.headword = ? COLLATE NOCASE AND e.source_id IN (" + in + ") "
                + "ORDER BY e.source_id, e.id LIMIT ?";
        try (Cursor c = commentaries.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                out.add(new ReferenceHit(ReferenceHit.KIND_COMMENTARY, c.getString(0), c.getString(3),
                        c.getString(4), c.getString(5), c.getString(6), c.getString(1), c.getString(2), 0));
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Commentary lookup failed", e);
        }
        return out;
    }

    public void close() {
        if (db != null && db.isOpen()) db.close();
        if (commentaries != null && commentaries.isOpen()) commentaries.close();
    }
}
