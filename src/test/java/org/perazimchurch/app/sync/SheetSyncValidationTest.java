package org.perazimchurch.app.sync;

import org.junit.Test;
import org.perazimchurch.app.data.local.dao.AnnouncementDao;
import org.perazimchurch.app.data.local.dao.AppConfigDao;
import org.perazimchurch.app.data.local.dao.DailyVerseDao;
import org.perazimchurch.app.data.local.dao.EventDao;
import org.perazimchurch.app.data.local.dao.HymnDao;
import org.perazimchurch.app.data.local.dao.JokeDao;
import org.perazimchurch.app.data.local.dao.RiddleDao;
import org.perazimchurch.app.data.local.dao.SermonDao;
import org.perazimchurch.app.data.local.entity.AnnouncementEntity;
import org.perazimchurch.app.data.local.entity.AppConfigEntity;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.local.entity.HymnEntity;
import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.RiddleEntity;
import org.perazimchurch.app.data.local.entity.SermonEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Google Sheet CSV synchronization validation:
 * - Header requirements & graceful fallback when columns are missing
 * - Skipping malformed rows (empty IDs, missing required fields)
 * - Safe type casting (booleans, integers, dates) with defaults
 * - Multi-tab entity mapping for Announcements, Hymns, Events, Verses, Config
 */
public class SheetSyncValidationTest {

    @Test
    public void testHeaderValidationMissingRequiredColumns() throws IOException {
        // Missing 'body' in announcements
        String csvMissingBody = "id,title,date\nann_1,Thanksgiving,2026-11-03";
        List<List<String>> rows = CsvParser.parse(csvMissingBody);

        List<AnnouncementEntity> inserted = new ArrayList<>();
        AnnouncementDao mockDao = new DummyAnnouncementDao(inserted);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processAnnouncementsRows(rows, mockDao);
        assertEquals(0, stats.synced);
        assertEquals(0, stats.skipped);
        assertEquals(0, inserted.size());
    }

    @Test
    public void testMalformedRowsSkipping() throws IOException {
        String csv = "id,title,body,priority,active\n" +
                "ann_1,Sunday Service,Join us at 9:00 AM,HIGH,true\n" +
                ",Missing ID Row,Body without ID,NORMAL,true\n" +
                "ann_2,,Empty Title Row,NORMAL,true\n" +
                "ann_3,Valid Second Row,Another good announcement,NORMAL,true";
        List<List<String>> rows = CsvParser.parse(csv);

        List<AnnouncementEntity> inserted = new ArrayList<>();
        AnnouncementDao mockDao = new DummyAnnouncementDao(inserted);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processAnnouncementsRows(rows, mockDao);
        assertEquals(2, stats.synced);
        assertEquals(2, stats.skipped);
        assertEquals(2, inserted.size());
        assertEquals("ann_1", inserted.get(0).getId());
        assertEquals("ann_3", inserted.get(1).getId());
    }

    @Test
    public void testFailSoftOnCorruptOrEmptyInput() {
        List<AnnouncementEntity> inserted = new ArrayList<>();
        AnnouncementDao mockDao = new DummyAnnouncementDao(inserted);

        SheetSyncManager.SyncStats statsEmpty = SheetSyncManager.processAnnouncementsRows(new ArrayList<>(), mockDao);
        assertEquals(0, statsEmpty.synced);
        assertEquals(0, statsEmpty.skipped);

        SheetSyncManager.SyncStats statsNull = SheetSyncManager.processAnnouncementsRows(null, mockDao);
        assertEquals(0, statsNull.synced);
        assertEquals(0, statsNull.skipped);
    }

    @Test
    public void testTypeParsingAndFallbackSafety() {
        // Booleans
        assertTrue(SheetSyncManager.parseBool("true"));
        assertTrue(SheetSyncManager.parseBool("TRUE"));
        assertTrue(SheetSyncManager.parseBool("1"));
        assertTrue(SheetSyncManager.parseBool("yes"));
        assertTrue(SheetSyncManager.parseBool(null)); // default true

        assertFalse(SheetSyncManager.parseBool("false"));
        assertFalse(SheetSyncManager.parseBool("FALSE"));
        assertFalse(SheetSyncManager.parseBool("0"));
        assertFalse(SheetSyncManager.parseBool("no"));

        // Integers
        assertEquals(80, SheetSyncManager.parseInt("80", 100));
        assertEquals(100, SheetSyncManager.parseInt("invalid_number", 100));
        assertEquals(100, SheetSyncManager.parseInt("", 100));

        // Dates
        long fallback = 1700000000000L;
        assertTrue(SheetSyncManager.parseDate("2026-11-03", fallback) > 0);
        assertEquals(fallback, SheetSyncManager.parseDate("not-a-date", fallback));
        assertEquals(fallback, SheetSyncManager.parseDate("", fallback));
    }

    @Test
    public void testHymnProcessingWithChordsAndMultiline() throws IOException {
        String csv = "id,title,writer,year,key,bpm,tags,lyrics,chords,active\n" +
                "hymn_101,\"Amazing Grace\",\"John Newton\",\"1779\",\"G Major\",\"72\",\"Grace,Faith\",\"Amazing grace! how sweet the sound\nThat saved a wretch like me!\",\"G - C - G - D\",true";
        List<List<String>> rows = CsvParser.parse(csv);

        List<HymnEntity> inserted = new ArrayList<>();
        HymnDao mockDao = new DummyHymnDao(inserted);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processHymnsRows(rows, mockDao);
        assertEquals(1, stats.synced);
        assertEquals(0, stats.skipped);
        assertEquals(1, inserted.size());

        HymnEntity h = inserted.get(0);
        assertEquals("hymn_101", h.getId());
        assertEquals(101, h.getNumber());
        assertEquals("Amazing Grace", h.getTitle());
        assertTrue(h.getLyrics().contains("\nThat saved a wretch like me!"));
        assertEquals("G - C - G - D", h.getChords());
        assertTrue(h.isActive());
    }

    @Test
    public void testDailyVerseProcessing() throws IOException {
        String csv = "date,reference,text,theme\n" +
                "2026-11-03,\"2 Samuel 5:20\",\"The LORD hath broken forth upon mine enemies before me, as the breach of waters.\",\"Breakthrough\"\n" +
                "2026-11-04,\"Philippians 4:13\",\"I can do all things through Christ which strengtheneth me.\",\"Strength\"";
        List<List<String>> rows = CsvParser.parse(csv);

        List<DailyVerseEntity> inserted = new ArrayList<>();
        DailyVerseDao mockDao = new DummyDailyVerseDao(inserted);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processVersesRows(rows, mockDao);
        assertEquals(2, stats.synced);
        assertEquals(0, stats.skipped);
        assertEquals(2, inserted.size());
        assertEquals("2026-11-03", inserted.get(0).getDate());
        assertEquals("2 Samuel 5:20", inserted.get(0).getReference());
        assertEquals("2026-11-04", inserted.get(1).getDate());
    }

    @Test
    public void testAppConfigProcessing() throws IOException {
        String csv = "key,value,description\n" +
                "church_motto,\"The Place of Great Breakthrough\",Institutional motto\n" +
                "giving_paybill,4069983,Safaricom Paybill number\n" +
                ",ignored_value,Empty key should skip";
        List<List<String>> rows = CsvParser.parse(csv);

        List<AppConfigEntity> inserted = new ArrayList<>();
        AppConfigDao mockDao = new DummyAppConfigDao(inserted);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processConfigRows(rows, mockDao);
        assertEquals(2, stats.synced);
        assertEquals(1, stats.skipped);
        assertEquals(2, inserted.size());
        assertEquals("church_motto", inserted.get(0).getKey());
        assertEquals("giving_paybill", inserted.get(1).getKey());
    }

    // -------------------------------------------------------------------------
    // Test Dummy DAOs (recording in-memory without database overhead)
    // -------------------------------------------------------------------------

    private static class DummyAnnouncementDao implements AnnouncementDao {
        private final List<AnnouncementEntity> list;
        DummyAnnouncementDao(List<AnnouncementEntity> list) { this.list = list; }
        @Override public void insert(AnnouncementEntity announcement) { list.add(announcement); }
        @Override public void insertAll(List<AnnouncementEntity> announcements) { list.addAll(announcements); }
        @Override public void update(AnnouncementEntity announcement) {}
        @Override public void delete(AnnouncementEntity announcement) {}
        @Override public AnnouncementEntity getAnnouncementById(String id) { return null; }
        @Override public List<AnnouncementEntity> getActiveAnnouncements(long currentTimeMillis) { return list; }
        @Override public List<AnnouncementEntity> getAllAnnouncements() { return list; }
        @Override public List<AnnouncementEntity> getActiveAnnouncements() { return list; }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummyHymnDao implements HymnDao {
        private final List<HymnEntity> list;
        DummyHymnDao(List<HymnEntity> list) { this.list = list; }
        @Override public void insert(HymnEntity hymn) { list.add(hymn); }
        @Override public void insertAll(List<HymnEntity> hymns) { list.addAll(hymns); }
        @Override public void update(HymnEntity hymn) {}
        @Override public void delete(HymnEntity hymn) {}
        @Override public HymnEntity getHymnById(String id) { return null; }
        @Override public HymnEntity getHymnByNumber(int number) { return null; }
        @Override public List<HymnEntity> getAllHymns() { return list; }
        @Override public List<HymnEntity> getActiveHymns() { return list; }
        @Override public List<HymnEntity> getFavoriteHymns() { return list; }
        @Override public List<HymnEntity> getHymnsByCategory(String category) { return list; }
        @Override public List<HymnEntity> searchHymns(String query) { return list; }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummyDailyVerseDao implements DailyVerseDao {
        private final List<DailyVerseEntity> list;
        DummyDailyVerseDao(List<DailyVerseEntity> list) { this.list = list; }
        @Override public void insert(DailyVerseEntity verse) { list.add(verse); }
        @Override public void insertAll(List<DailyVerseEntity> verses) { list.addAll(verses); }
        @Override public void update(DailyVerseEntity verse) {}
        @Override public void delete(DailyVerseEntity verse) {}
        @Override public DailyVerseEntity getVerseForDate(String date) { return null; }
        @Override public List<DailyVerseEntity> getAllActiveVerses() { return list; }
        @Override public void deleteByDate(String date) {}
        @Override public void deleteAll() {}
    }

    private static class DummyAppConfigDao implements AppConfigDao {
        private final List<AppConfigEntity> list;
        DummyAppConfigDao(List<AppConfigEntity> list) { this.list = list; }
        @Override public void insert(AppConfigEntity config) { list.add(config); }
        @Override public void insertAll(List<AppConfigEntity> configs) { list.addAll(configs); }
        @Override public String getConfigValue(String key) { return null; }
        @Override public List<AppConfigEntity> getAllConfig() { return list; }
        @Override public void deleteByKey(String key) {}
        @Override public void deleteAll() {}
    }
}
