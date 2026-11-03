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

    @Test
    public void testHymnSyncPreservesTimeSignatureAndFavorite() throws IOException {
        List<HymnEntity> existingList = new ArrayList<>();
        // Seed hymn_101 as favorite with custom 6/8 meter and category
        HymnEntity existingFavorite = new HymnEntity(
                "hymn_101", 101, "Old Title", "Old lyrics", "C", "C Major", "6/8",
                "Faith", true, "Writer", "1900", 80, "Grace", true);
        existingList.add(existingFavorite);

        DummyHymnDao mockDao = new DummyHymnDao(existingList);

        // CSV updates hymn_101 without time_signature or category, adds hymn_102 (no time_sig), adds hymn_103 (explicit 3/4)
        String csv = "id,title,writer,year,key,bpm,tags,lyrics,chords,active\n" +
                "hymn_101,\"Amazing Grace (Updated)\",\"Newton\",\"1779\",\"G\",\"72\",\"Grace\",\"Updated lyrics\",\"G\",true\n" +
                "hymn_102,\"How Great Thou Art\",\"Boberg\",\"1885\",\"Bb\",\"68\",\"Praise\",\"O Lord my God\",\"Bb\",true\n" +
                "hymn_103,\"The Old Rugged Cross\",\"Bennard\",\"1913\",\"Bb\",\"68\",\"Cross\",\"On a hill far away\",\"Bb\",true\n";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processHymnsRows(rows, mockDao);
        assertEquals(3, stats.synced);

        // hymn_101 must preserve isFavorite = true, timeSignature = "6/8", and category = "Faith"
        HymnEntity updated101 = mockDao.getHymnById("hymn_101");
        assertEquals("Amazing Grace (Updated)", updated101.getTitle());
        assertTrue("Local favorite flag must be preserved after sync", updated101.isFavorite());
        assertEquals("Existing timeSignature must be preserved", "6/8", updated101.getTimeSignature());
        assertEquals("Existing category must be preserved", "Faith", updated101.getCategory());

        // hymn_102 should default to isFavorite = false, and timeSignature = "" (NEVER forced to 4/4)
        HymnEntity new102 = mockDao.getHymnById("hymn_102");
        assertEquals("How Great Thou Art", new102.getTitle());
        assertFalse("New hymn must default to isFavorite = false", new102.isFavorite());
        assertEquals("Time signature must not be forced to 4/4 on new rows", "", new102.getTimeSignature());
    }

    @Test
    public void testSermonSyncPreservesExistingDurationSummaryNotesAndDownloads() throws IOException {
        List<SermonEntity> existingList = new ArrayList<>();
        // Seed sermon_1 as downloaded with local file path, custom duration, summary, notes
        SermonEntity downloadedSermon = new SermonEntity(
                "sermon_1", "Old Title", "Bishop Mutweri", "Series", "Passage",
                "https://example.com/audio.mp3", "", 1700000000000L, 3450,
                "Deep breakthrough summary", "My personal study notes", true, "/local/path/sermon_1.mp3", true);
        existingList.add(downloadedSermon);

        DummySermonDao mockDao = new DummySermonDao(existingList);

        // CSV updates sermon_1 (omitting duration, summary, notes), adds sermon_2 (new row without duration/summary/notes)
        String csv = "id,title,speaker,date,series,scripture,youtube_url,audio_url,active\n" +
                "sermon_1,\"Breakthrough Authority (Updated)\",\"Bishop Dr. David Mutweri\",\"2026-11-01\",\"Power\",\"2 Sam 5:20\",\"\",\"https://example.com/new_audio.mp3\",true\n" +
                "sermon_2,\"Walking in Faith\",\"Bishop Dr. David Mutweri\",\"2026-11-08\",\"Faith\",\"Heb 11:1\",\"\",\"https://example.com/s2.mp3\",true";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processSermonsRows(rows, mockDao);
        assertEquals(2, stats.synced);

        // sermon_1 must preserve isDownloaded, localAudioPath, durationSeconds, summary, notes
        SermonEntity updated1 = mockDao.getSermonById("sermon_1");
        assertEquals("Breakthrough Authority (Updated)", updated1.getTitle());
        assertTrue("Local isDownloaded flag must be preserved after sync", updated1.isDownloaded());
        assertEquals("/local/path/sermon_1.mp3", updated1.getLocalAudioPath());
        assertEquals("Existing durationSeconds must be preserved, not overwritten with 2700", 3450, updated1.getDurationSeconds());
        assertEquals("Existing summary must be preserved, not overwritten with hardcoded text", "Deep breakthrough summary", updated1.getSummary());
        assertEquals("Existing notes must be preserved", "My personal study notes", updated1.getNotes());

        // sermon_2 should default to isDownloaded = false, empty localAudioPath, 0 duration, empty summary, empty notes
        SermonEntity new2 = mockDao.getSermonById("sermon_2");
        assertFalse("New sermon must default to isDownloaded = false", new2.isDownloaded());
        assertEquals("", new2.getLocalAudioPath());
        assertEquals("New sermon duration must default to 0 when unspecified", 0, new2.getDurationSeconds());
        assertEquals("New sermon summary must default to empty when unspecified", "", new2.getSummary());
        assertEquals("New sermon notes must default to empty when unspecified", "", new2.getNotes());
    }

    @Test
    public void testDateParsingUsesNairobiTimezone() {
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Africa/Nairobi"), java.util.Locale.ROOT);
        cal.clear();
        cal.set(2026, java.util.Calendar.NOVEMBER, 3, 10, 0, 0);
        long expectedMillis = cal.getTimeInMillis();

        long parsed = SheetSyncManager.parseDate("2026-11-03 10:00", 0L);
        assertEquals("Parsed date must match Africa/Nairobi timezone", expectedMillis, parsed);
    }

    @Test
    public void testSermonSyncPreservesMemberNotesAgainstRemoteNotesColumn() throws IOException {
        List<SermonEntity> existingList = new ArrayList<>();
        SermonEntity existingSermon = new SermonEntity(
                "sermon_1", "Original Title", "Speaker", "Series", "Scripture",
                "https://audio.mp3", "", 1700000000000L, 1800,
                "Summary", "Personal Member Study Notes", true, "/local/path/s1.mp3", true);
        existingList.add(existingSermon);

        DummySermonDao mockDao = new DummySermonDao(existingList);

        String csv = "id,title,speaker,date,series,scripture,youtube_url,audio_url,active,notes\n" +
                "sermon_1,\"Updated Title\",\"Speaker\",\"2026-11-01\",\"Series\",\"Scripture\",\"\",\"https://audio.mp3\",true,\"Remote Secretariat Note\"";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processSermonsRows(rows, mockDao);
        assertEquals(1, stats.synced);

        SermonEntity updated = mockDao.getSermonById("sermon_1");
        assertEquals("Personal Member Study Notes", updated.getNotes());
        assertTrue(updated.isDownloaded());
        assertEquals("/local/path/s1.mp3", updated.getLocalAudioPath());
    }

    @Test
    public void testAnnouncementProcessingMapsSheetContent() throws IOException {
        List<AnnouncementEntity> list = new ArrayList<>();
        DummyAnnouncementDao mockDao = new DummyAnnouncementDao(list);

        String csv = "id,title,body,priority,date,campus,active\n" +
                "ann_1,\"Sunday Celebration\",\"Service starts at 9:00 AM\",HIGH,2026-11-08,Nairobi,true\n" +
                "ann_2,\"Youth Fellowship\",\"Join us on Friday\",NORMAL,2026-11-06,all,true";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processAnnouncementsRows(rows, mockDao);
        assertEquals(2, stats.synced);

        AnnouncementEntity a1 = mockDao.getAnnouncementById("ann_1");
        assertEquals("Sunday Celebration", a1.getTitle());
        assertEquals("HIGH", a1.getPriority());
        assertEquals("Nairobi", a1.getCampusId());

        AnnouncementEntity a2 = mockDao.getAnnouncementById("ann_2");
        assertEquals("NORMAL", a2.getPriority());
        assertEquals("all", a2.getCampusId());
    }

    @Test
    public void testEventProcessingMapsSheetContent() throws IOException {
        List<EventEntity> list = new ArrayList<>();
        DummyEventDao mockDao = new DummyEventDao(list);

        String csv = "id,title,description,venue,campus,category,active\n" +
                "evt_1,\"Breakthrough Night 2026\",\"All-night prayer vigil\",Prayer Hall,Embu Headquarters,Vigil,true\n" +
                "evt_2,\"Sunday First Service\",\"Morning praise and word\",Sanctuary,all,Worship,true";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processEventsRows(rows, mockDao);
        assertEquals(2, stats.synced);

        EventEntity e1 = mockDao.getEventById("evt_1");
        assertEquals("Breakthrough Night 2026", e1.getTitle());
        assertEquals("Prayer Hall", e1.getLocation());
        assertEquals("Embu Headquarters", e1.getCampusId());
        assertEquals("Vigil", e1.getCategory());

        EventEntity e2 = mockDao.getEventById("evt_2");
        assertEquals("Sanctuary", e2.getLocation());
        assertEquals("Worship", e2.getCategory());
    }

    @Test
    public void testRiddleProcessingMapsSheetContent() throws IOException {
        List<RiddleEntity> list = new ArrayList<>();
        DummyRiddleDao mockDao = new DummyRiddleDao(list);

        String csv = "id,question,answer,explanation,scripture_ref,difficulty,xp,active\n" +
                "rid_1,\"Who defeated the Philistine giant?\",\"David\",\"David used 5 smooth stones\",\"1 Samuel 17:40\",HARD,50,true\n" +
                "rid_2,\"Who built the ark?\",\"Noah\",\"Built gopher wood ark\",\"Genesis 6:14\",MEDIUM,20,true";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processRiddlesRows(rows, mockDao);
        assertEquals(2, stats.synced);

        RiddleEntity r1 = mockDao.getRiddleById("rid_1");
        assertEquals("David", r1.getAnswer());
        assertEquals("David used 5 smooth stones", r1.getExplanation());
        assertEquals("1 Samuel 17:40", r1.getScriptureRef());
        assertEquals("HARD", r1.getDifficulty());
        assertEquals(50, r1.getXp());

        RiddleEntity r2 = mockDao.getRiddleById("rid_2");
        assertEquals("Noah", r2.getAnswer());
        assertEquals("MEDIUM", r2.getDifficulty());
        assertEquals(20, r2.getXp());
    }

    @Test
    public void testJokeProcessingMapsSheetContent() throws IOException {
        List<JokeEntity> list = new ArrayList<>();
        DummyJokeDao mockDao = new DummyJokeDao(list);

        String csv = "id,text,clean_rating,category,active\n" +
                "joke_1,\"Why did Noah have to fish with two lines? Because he had to feed two cats!\",FAMILY_SAFE,Fellowship,true\n" +
                "joke_2,\"Who was the greatest financier in the Bible? Noah, he was floating his stock while everyone else was in liquidation!\",FAMILY_SAFE,Fellowship,true";
        List<List<String>> rows = CsvParser.parse(csv);

        SheetSyncManager.SyncStats stats = SheetSyncManager.processJokesRows(rows, mockDao);
        assertEquals(2, stats.synced);

        JokeEntity j1 = mockDao.getJokeById("joke_1");
        assertTrue(j1.getText().contains("feed two cats"));
        assertEquals("FAMILY_SAFE", j1.getCleanRating());
        assertEquals("Fellowship", j1.getCategory());

        JokeEntity new2 = mockDao.getJokeById("joke_2");
        assertEquals("FAMILY_SAFE", new2.getCleanRating());
        assertEquals("Fellowship", new2.getCategory());
    }

    // -------------------------------------------------------------------------
    // Test Dummy DAOs (recording in-memory without database overhead)
    // -------------------------------------------------------------------------

    private static class DummyAnnouncementDao implements AnnouncementDao {
        private final List<AnnouncementEntity> list;
        DummyAnnouncementDao(List<AnnouncementEntity> list) { this.list = list; }
        @Override public void insert(AnnouncementEntity announcement) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(announcement.getId())) {
                    list.set(i, announcement);
                    return;
                }
            }
            list.add(announcement);
        }
        @Override public void insertAll(List<AnnouncementEntity> announcements) {
            for (AnnouncementEntity a : announcements) insert(a);
        }
        @Override public void update(AnnouncementEntity announcement) {}
        @Override public void delete(AnnouncementEntity announcement) {}
        @Override public AnnouncementEntity getAnnouncementById(String id) {
            for (AnnouncementEntity a : list) {
                if (a.getId().equals(id)) return a;
            }
            return null;
        }
        @Override public List<AnnouncementEntity> getActiveAnnouncements(long currentTimeMillis) { return list; }
        @Override public List<AnnouncementEntity> getAllAnnouncements() { return list; }
        @Override public List<AnnouncementEntity> getActiveAnnouncements() { return list; }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummyEventDao implements EventDao {
        private final List<EventEntity> list;
        DummyEventDao(List<EventEntity> list) { this.list = list; }
        @Override public void insert(EventEntity event) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(event.getId())) {
                    list.set(i, event);
                    return;
                }
            }
            list.add(event);
        }
        @Override public void insertAll(List<EventEntity> events) {
            for (EventEntity e : events) insert(e);
        }
        @Override public void update(EventEntity event) {}
        @Override public void delete(EventEntity event) {}
        @Override public EventEntity getEventById(String id) {
            for (EventEntity e : list) {
                if (e.getId().equals(id)) return e;
            }
            return null;
        }
        @Override public List<EventEntity> getAllEvents() { return list; }
        @Override public List<EventEntity> getActiveEvents() { return list; }
        @Override public List<EventEntity> getUpcomingEvents(long currentTimeMillis) { return list; }
        @Override public List<EventEntity> getEventsByCampus(String campusId) { return list; }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummyRiddleDao implements RiddleDao {
        private final List<RiddleEntity> list;
        DummyRiddleDao(List<RiddleEntity> list) { this.list = list; }
        @Override public void insert(RiddleEntity riddle) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(riddle.getId())) {
                    list.set(i, riddle);
                    return;
                }
            }
            list.add(riddle);
        }
        @Override public void insertAll(List<RiddleEntity> riddles) {
            for (RiddleEntity r : riddles) insert(r);
        }
        @Override public void update(RiddleEntity riddle) {}
        @Override public void delete(RiddleEntity riddle) {}
        @Override public RiddleEntity getRiddleById(String id) {
            for (RiddleEntity r : list) {
                if (r.getId().equals(id)) return r;
            }
            return null;
        }
        @Override public List<RiddleEntity> getAllRiddles() { return list; }
        @Override public List<RiddleEntity> getActiveRiddles() { return list; }
        @Override public List<RiddleEntity> getRiddlesByDifficulty(String difficulty) { return list; }
        @Override public RiddleEntity getRandomRiddle() { return list.isEmpty() ? null : list.get(0); }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummyJokeDao implements JokeDao {
        private final List<JokeEntity> list;
        DummyJokeDao(List<JokeEntity> list) { this.list = list; }
        @Override public void insert(JokeEntity joke) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(joke.getId())) {
                    list.set(i, joke);
                    return;
                }
            }
            list.add(joke);
        }
        @Override public void insertAll(List<JokeEntity> jokes) {
            for (JokeEntity j : jokes) insert(j);
        }
        @Override public void update(JokeEntity joke) {}
        @Override public void delete(JokeEntity joke) {}
        @Override public JokeEntity getJokeById(String id) {
            for (JokeEntity j : list) {
                if (j.getId().equals(id)) return j;
            }
            return null;
        }
        @Override public List<JokeEntity> getAllJokes() { return list; }
        @Override public List<JokeEntity> getActiveJokes() { return list; }
        @Override public List<JokeEntity> getJokesByCategory(String category) { return list; }
        @Override public JokeEntity getRandomJoke() { return list.isEmpty() ? null : list.get(0); }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummyHymnDao implements HymnDao {
        private final List<HymnEntity> list;
        DummyHymnDao(List<HymnEntity> list) { this.list = list; }
        @Override public void insert(HymnEntity hymn) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(hymn.getId())) {
                    list.set(i, hymn);
                    return;
                }
            }
            list.add(hymn);
        }
        @Override public void insertAll(List<HymnEntity> hymns) {
            for (HymnEntity h : hymns) insert(h);
        }
        @Override public void update(HymnEntity hymn) {}
        @Override public void delete(HymnEntity hymn) {}
        @Override public HymnEntity getHymnById(String id) {
            for (HymnEntity h : list) {
                if (h.getId().equals(id)) return h;
            }
            return null;
        }
        @Override public HymnEntity getHymnByNumber(int number) { return null; }
        @Override public List<HymnEntity> getAllHymns() { return list; }
        @Override public List<HymnEntity> getActiveHymns() { return list; }
        @Override public List<HymnEntity> getFavoriteHymns() { return list; }
        @Override public List<HymnEntity> getHymnsByCategory(String category) { return list; }
        @Override public List<HymnEntity> searchHymns(String query) { return list; }
        @Override public void deleteById(String id) {}
        @Override public void deleteAll() {}
    }

    private static class DummySermonDao implements SermonDao {
        private final List<SermonEntity> list;
        DummySermonDao(List<SermonEntity> list) { this.list = list; }
        @Override public void insert(SermonEntity sermon) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(sermon.getId())) {
                    list.set(i, sermon);
                    return;
                }
            }
            list.add(sermon);
        }
        @Override public void insertAll(List<SermonEntity> sermons) {
            for (SermonEntity s : sermons) insert(s);
        }
        @Override public void update(SermonEntity sermon) {}
        @Override public void delete(SermonEntity sermon) {}
        @Override public SermonEntity getSermonById(String id) {
            for (SermonEntity s : list) {
                if (s.getId().equals(id)) return s;
            }
            return null;
        }
        @Override public List<SermonEntity> getAllSermons() { return list; }
        @Override public List<SermonEntity> getActiveSermons() { return list; }
        @Override public List<SermonEntity> getDownloadedSermons() { return list; }
        @Override public List<SermonEntity> getSermonsBySeries(String series) { return list; }
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
