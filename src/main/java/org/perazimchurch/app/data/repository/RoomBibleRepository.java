package org.perazimchurch.app.data.repository;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseHighlightEntity;
import org.perazimchurch.app.data.local.entity.BibleTopicVerseEntity;
import org.perazimchurch.app.data.local.dao.BibleNoteDao;
import org.perazimchurch.app.data.local.entity.BibleNoteEntity;
import org.perazimchurch.app.data.local.entity.BibleNoteLinkEntity;
import org.perazimchurch.app.domain.model.BibleNote;
import org.perazimchurch.app.data.local.seeder.BibleDataSeeder;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.data.interlinear.GreekInterlinearStore;
import org.perazimchurch.app.data.interlinear.HebrewInterlinearStore;
import org.perazimchurch.app.data.interlinear.StrongsStore;
import org.perazimchurch.app.domain.model.StrongsEntry;
import org.perazimchurch.app.domain.model.VerseLocation;
import org.perazimchurch.app.domain.repository.BibleRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Room-backed repository implementation for Holy Scripture.
 * Manages translations, books, verses, bookmarks, and verse notes.
 * Compliant with Guidebook §34 and Phase 1.2 Architecture Target.
 */
public class RoomBibleRepository implements BibleRepository {

    private final BibleDao bibleDao;
    private BibleNoteDao noteDao;
    /** Verse text: the separate text database once a translation has moved there, the private one before. */
    private org.perazimchurch.app.data.local.text.BibleVerseReader reader;

    public RoomBibleRepository(BibleDao bibleDao) {
        this.bibleDao = bibleDao;
        this.reader = bibleDao != null ? org.perazimchurch.app.data.local.text.BibleVerseReader.mainOnly(bibleDao) : null;
        if (bibleDao != null) {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        BibleDataSeeder.seedIfNeeded(null, bibleDao);
                    } catch (Throwable ignored) {}
                });
            } else {
                BibleDataSeeder.seedIfNeeded(null, bibleDao);
            }
        }
    }

    public RoomBibleRepository(PerazimDatabase database) {
        this(database != null ? database.bibleDao() : null);
    }

    private android.content.Context assetContext;
    private volatile boolean assetsReady;

    public RoomBibleRepository(android.content.Context context, PerazimDatabase database) {
        this.bibleDao = database != null ? database.bibleDao() : null;
        this.noteDao = database != null ? database.bibleNoteDao() : null;
        this.assetContext = context != null ? context.getApplicationContext() : null;
        if (this.bibleDao != null) {
            this.reader = org.perazimchurch.app.data.local.text.BibleVerseReader.create(this.assetContext, this.bibleDao);
        }
        if (this.assetContext != null && this.bibleDao != null) {
            org.perazimchurch.app.data.local.text.BibleTextMigration.startInBackground(this.assetContext);
        }
    }

    // Run on the ViewModel executor before the first read, avoiding a race with seeding.
    private synchronized void ensureAssetsReady() {
        if (!assetsReady && assetContext != null) {
            BibleDataSeeder.seedIfNeeded(assetContext, bibleDao);
            assetsReady = true;
        }
    }

    @Override
    public List<BibleTranslation> getAvailableTranslations() {
        ensureAssetsReady();
        List<BibleTranslationEntity> entities = bibleDao.getTranslations();
        if (entities == null || entities.isEmpty()) {
            BibleDataSeeder.seedIfNeeded(null, bibleDao);
            entities = bibleDao.getTranslations();
        }
        return BibleMapper.toDomainTranslationList(entities);
    }

    @Override
    public List<BibleBook> getBooks() {
        ensureAssetsReady();
        String translationId = resolveDefaultTranslationId();
        List<BibleBookEntity> bookEntities = bibleDao.getBooksByTranslation(translationId);
        if ((bookEntities == null || bookEntities.isEmpty()) && !"KJV".equalsIgnoreCase(translationId)) {
            // Fallback to KJV if default translation returned no books
            bookEntities = bibleDao.getBooksByTranslation("KJV");
        }
        if (bookEntities == null || bookEntities.isEmpty()) {
            BibleDataSeeder.seedIfNeeded(null, bibleDao);
            bookEntities = bibleDao.getBooksByTranslation(translationId);
            if ((bookEntities == null || bookEntities.isEmpty()) && !"KJV".equalsIgnoreCase(translationId)) {
                bookEntities = bibleDao.getBooksByTranslation("KJV");
            }
        }
        return BibleMapper.toDomainBookList(bookEntities);
    }

    @Override
    public List<BibleVerse> getVersesForChapter(String translationId, String bookId, int chapterNumber) {
        ensureAssetsReady();
        String resolvedTranslation = (translationId != null && !translationId.isEmpty())
                ? translationId
                : resolveDefaultTranslationId();
        int bookNumber = resolveBookNumber(resolvedTranslation, bookId);
        List<BibleVerse> verses;
        if (GREEK_INTERLINEAR.equals(resolvedTranslation)) {
            verses = interlinearChapter(bookNumber, chapterNumber, bookId, true);
        } else if (HEBREW_INTERLINEAR.equals(resolvedTranslation)) {
            verses = interlinearChapter(bookNumber, chapterNumber, bookId, false);
        } else {
            List<BibleVerseEntity> entities = withUserData(reader.getVerses(resolvedTranslation, bookNumber, chapterNumber), resolvedTranslation, bookNumber, chapterNumber);
            verses = BibleMapper.toDomainVerseList(entities, resolvedTranslation, bookId);
        }
        Map<Integer, String> highlights = highlightsFor(bookNumber, chapterNumber);
        for (BibleVerse verse : verses) {
            verse.setHighlightColor(highlights.get(verse.getVerseNumber()));
        }
        return verses;
    }

    @Override
    public BibleVerse getVerse(String translationId, String bookId, int chapterNumber, int verseNumber) {
        ensureAssetsReady();
        String resolvedTranslation = (translationId != null && !translationId.isEmpty())
                ? translationId
                : resolveDefaultTranslationId();
        int bookNumber = resolveBookNumber(resolvedTranslation, bookId);
        BibleVerseEntity entity = reader.getVerse(resolvedTranslation, bookNumber, chapterNumber, verseNumber);
        entity = withUserData(entity, resolvedTranslation);

        if (entity == null) {
            return null;
        }
        return BibleMapper.toDomain(entity, resolvedTranslation, bookId);
    }

    @Override
    public List<BibleVerse> searchScripture(String translationId, String query) {
        ensureAssetsReady();
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String resolvedTranslation = (translationId != null && !translationId.isEmpty())
                ? translationId
                : resolveDefaultTranslationId();
        String cleanQuery = query.trim();
        List<BibleVerseEntity> entities = reader.search(resolvedTranslation, cleanQuery);
        if ((entities == null || entities.isEmpty()) && cleanQuery.contains("-")) {
            entities = reader.search(resolvedTranslation, cleanQuery.replace("-", " ").trim());
        } else if ((entities == null || entities.isEmpty()) && cleanQuery.contains(" ")) {
            entities = reader.search(resolvedTranslation, cleanQuery.replace(" ", "-").trim());
        }

        if (entities != null) {
            for (BibleVerseEntity found : entities) {
                withUserData(found, resolvedTranslation);
            }
        }
        return BibleMapper.toDomainVerseList(entities, resolvedTranslation);
    }

    @Override
    public List<BibleVerse> getBookmarkedVerses() {
        List<BibleVerseEntity> entities = bibleDao.getBookmarkedVerses();
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return BibleMapper.toDomainVerseList(entities, resolveDefaultTranslationId());
    }

    @Override
    public void bookmarkVerse(String verseId, boolean isFavorite) {
        if (verseId == null || verseId.trim().isEmpty()) {
            return;
        }
        BibleVerseEntity row = userRowFor(verseId.trim());
        if (row != null) {
            row.setFavorite(isFavorite);
            bibleDao.updateVerse(row);
        }
    }

    @Override
    public void saveVerseNote(String verseId, String note) {
        if (verseId == null || verseId.trim().isEmpty()) {
            return;
        }
        BibleVerseEntity row = userRowFor(verseId.trim());
        if (row != null) {
            row.setNote(note);
            bibleDao.updateVerse(row);
        }
    }

    /**
     * The private row that holds a verse's bookmark and note. Verse text lives in the separate text database,
     * so the row is created from that text the first time the person marks the verse.
     */
    private BibleVerseEntity userRowFor(String verseId) {
        BibleVerseEntity existing = bibleDao.getVerseById(verseId);
        if (existing != null) {
            return existing;
        }
        String[] parts = verseId.split("\\.");
        String translation;
        String book;
        int chapter;
        int verse;
        try {
            if (parts.length == 3) {
                translation = "KJV";
                book = parts[0];
                chapter = Integer.parseInt(parts[1]);
                verse = Integer.parseInt(parts[2]);
            } else if (parts.length == 4) {
                translation = parts[0];
                book = parts[1];
                chapter = Integer.parseInt(parts[2]);
                verse = Integer.parseInt(parts[3]);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        int bookNumber = resolveBookNumber(translation, book);
        BibleVerseEntity text = reader.getVerse(translation, bookNumber, chapter, verse);
        if (text == null) {
            return null;
        }
        BibleVerseEntity created = new BibleVerseEntity(verseId, translation, text.getChapterId(), bookNumber,
                chapter, verse, text.getText(), false, null);
        bibleDao.insertVerse(created);
        return created;
    }

    /** Adds the person's bookmark and note (private database) to verses read from the text database. */
    private List<BibleVerseEntity> withUserData(List<BibleVerseEntity> verses, String translationId, int bookNumber, int chapter) {
        if (verses == null || verses.isEmpty()) {
            return verses;
        }
        Map<Integer, BibleVerseEntity> mine = new HashMap<>();
        for (BibleVerseEntity row : bibleDao.getVerses(translationId, bookNumber, chapter)) {
            mine.put(row.getVerseNumber(), row);
        }
        for (BibleVerseEntity verse : verses) {
            BibleVerseEntity row = mine.get(verse.getVerseNumber());
            if (row != null && row != verse) {
                verse.setId(row.getId());
                verse.setFavorite(row.isFavorite());
                verse.setNote(row.getNote());
            }
        }
        return verses;
    }

    private BibleVerseEntity withUserData(BibleVerseEntity verse, String translationId) {
        if (verse == null) {
            return null;
        }
        BibleVerseEntity row = bibleDao.getVerse(translationId, verse.getBookNumber(), verse.getChapterNumber(), verse.getVerseNumber());
        if (row != null && row != verse) {
            verse.setId(row.getId());
            verse.setFavorite(row.isFavorite());
            verse.setNote(row.getNote());
        }
        return verse;
    }

    /** Pseudo-translation id for the Greek New Testament interlinear. */
    public static final String GREEK_INTERLINEAR = "GRK";

    /** Pseudo-translation id for the Hebrew Old Testament interlinear. */
    public static final String HEBREW_INTERLINEAR = "HEB";

    @Override
    public List<BibleTranslation> getTranslationsWithText() {
        ensureAssetsReady();
        List<BibleTranslation> readable = new ArrayList<>();
        for (BibleTranslation translation : getAvailableTranslations()) {
            if (translation.getId() != null && reader.count(translation.getId()) > 0) {
                translation.setDownloaded(true);
                readable.add(translation);
            }
        }
        BibleTranslation greek = new BibleTranslation(GREEK_INTERLINEAR, "Greek Interlinear (New Testament)", "el", true);
        readable.add(greek);
        BibleTranslation hebrew = new BibleTranslation(HEBREW_INTERLINEAR, "Hebrew Interlinear (Old Testament)", "he", true);
        readable.add(hebrew);
        return readable;
    }

    /**
     * Verse list for an interlinear version. The verse text lists each word as {original} then its gloss;
     * the full word-by-word view (transliteration, gloss, Strong's number, parsing) opens from the verse.
     */
    private List<BibleVerse> interlinearChapter(int bookNumber, int chapterNumber, String bookId, boolean greek) {
        List<BibleVerse> verses = new ArrayList<>();
        if (greek ? bookNumber < 40 : bookNumber > 39) {
            return verses;
        }
        String translationId = greek ? GREEK_INTERLINEAR : HEBREW_INTERLINEAR;
        for (BibleVerseEntity entity : reader.getVerses("KJV", bookNumber, chapterNumber)) {
            List<InterlinearWord> words = interlinearWords(greek, bookNumber, chapterNumber, entity.getVerseNumber());
            if (words.isEmpty()) {
                continue;
            }
            // Each word is written as {original} followed by its English gloss. The reader shows the English
            // in bold and the original inside the braces, so the English is what the eye lands on first.
            StringBuilder text = new StringBuilder();
            for (InterlinearWord word : words) {
                if (text.length() > 0) {
                    text.append(' ');
                }
                // {original|H1234}: the Strong's number rides along so the reader can open the dictionary entry.
                text.append('{').append(word.getOriginal());
                if (word.getStrongsNumber() > 0) {
                    text.append('|').append(greek ? "G" : "H").append(word.getStrongsNumber());
                }
                text.append("} ");
                text.append(englishGloss(word));
            }
            BibleVerse verse = BibleMapper.toDomain(entity, translationId, bookId);
            verse.setText(text.toString());
            verses.add(verse);
        }
        return verses;
    }

    /** The English shown in bold: the gloss if it is English, else the transliteration, else a dash. */
    private static String englishGloss(InterlinearWord word) {
        String gloss = word.getGloss() == null ? "" : word.getGloss().trim();
        if (containsLatin(gloss)) {
            return gloss;
        }
        String translit = word.getTransliteration() == null ? "" : word.getTransliteration().trim();
        return containsLatin(translit) ? translit : "\u2014";
    }

    private static boolean containsLatin(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return true;
            }
        }
        return false;
    }

    private List<InterlinearWord> interlinearWords(boolean greek, int bookNumber, int chapterNumber, int verseNumber) {
        return greek
                ? GreekInterlinearStore.getVerse(assetContext, bookNumber, chapterNumber, verseNumber)
                : HebrewInterlinearStore.getVerse(assetContext, bookNumber, chapterNumber, verseNumber);
    }

    @Override
    public Map<Integer, String> getHighlightsForChapter(String bookId, int chapterNumber) {
        ensureAssetsReady();
        return highlightsFor(resolveBookNumber(null, bookId), chapterNumber);
    }

    @Override
    public void setHighlight(String bookId, int chapterNumber, int verseNumber, String colour) {
        if (bookId == null || bookId.trim().isEmpty() || chapterNumber < 1 || verseNumber < 1) {
            return;
        }
        int bookNumber = resolveBookNumber(null, bookId);
        if (colour == null || colour.trim().isEmpty()) {
            bibleDao.deleteHighlight(bookNumber, chapterNumber, verseNumber);
        } else {
            bibleDao.upsertHighlight(new BibleVerseHighlightEntity(
                    bookNumber, chapterNumber, verseNumber, colour.trim(), System.currentTimeMillis()));
        }
    }

    @Override
    public List<BibleVerse> getSavedVerses() {
        ensureAssetsReady();
        String translationId = resolveDefaultTranslationId();
        Map<String, BibleVerse> byReference = new LinkedHashMap<>();
        Map<String, String> colours = new HashMap<>();
        for (BibleVerseHighlightEntity highlight : bibleDao.getAllHighlights()) {
            colours.put(referenceKey(highlight.getBookNumber(), highlight.getChapterNumber(), highlight.getVerseNumber()),
                    highlight.getColor());
        }
        List<BibleVerseEntity> bookmarked = bibleDao.getBookmarkedVerses();
        if (bookmarked != null) {
            for (BibleVerse verse : BibleMapper.toDomainVerseList(bookmarked, translationId)) {
                String key = referenceKey(BibleMapper.resolveBookNumber(verse.getBookId()),
                        verse.getChapterNumber(), verse.getVerseNumber());
                verse.setHighlightColor(colours.get(key));
                byReference.put(key, verse);
            }
        }
        for (BibleVerseHighlightEntity highlight : bibleDao.getAllHighlights()) {
            String key = referenceKey(highlight.getBookNumber(), highlight.getChapterNumber(), highlight.getVerseNumber());
            if (byReference.containsKey(key)) {
                continue;
            }
            BibleVerseEntity entity = reader.getVerse(translationId, highlight.getBookNumber(),
                    highlight.getChapterNumber(), highlight.getVerseNumber());
            if (entity != null) {
                BibleVerse verse = BibleMapper.toDomain(entity, translationId);
                verse.setHighlightColor(highlight.getColor());
                byReference.put(key, verse);
            }
        }
        return new ArrayList<>(byReference.values());
    }

    @Override
    public List<BibleVerse> getVerseInAllTranslations(String bookId, int chapterNumber, int verseNumber) {
        ensureAssetsReady();
        int bookNumber = resolveBookNumber(null, bookId);
        List<BibleVerse> verses = new ArrayList<>();
        for (BibleTranslation translation : getTranslationsWithText()) {
            BibleVerseEntity entity = reader.getVerse(translation.getId(), bookNumber,
                    chapterNumber, verseNumber);
            if (entity != null) {
                verses.add(BibleMapper.toDomain(entity, translation.getId(), bookId));
            }
        }
        return verses;
    }

    @Override
    public List<InterlinearWord> getInterlinear(String bookId, int chapterNumber, int verseNumber) {
        int bookNumber = resolveBookNumber(null, bookId);
        if (bookNumber >= 40) {
            return GreekInterlinearStore.getVerse(assetContext, bookNumber, chapterNumber, verseNumber);
        }
        return HebrewInterlinearStore.getVerse(assetContext, bookNumber, chapterNumber, verseNumber);
    }

    @Override
    public StrongsEntry getStrongsEntry(String code) {
        return StrongsStore.getEntry(assetContext, code);
    }

    @Override
    public List<VerseLocation> getGreekConcordance(int strongsNumber) {
        return GreekInterlinearStore.findOccurrences(assetContext, strongsNumber);
    }

    @Override
    public List<String> getTopics() {
        return bibleDao.getTopicNames();
    }

    @Override
    public List<String> getTopicsForVerse(String bookId, int chapterNumber, int verseNumber) {
        List<String> names = new ArrayList<>();
        for (BibleTopicVerseEntity link : bibleDao.getTopicsForVerse(resolveBookNumber(null, bookId), chapterNumber, verseNumber)) {
            names.add(link.getTopic());
        }
        return names;
    }

    @Override
    public void setVerseTopic(String bookId, int chapterNumber, int verseNumber, String topic, boolean member) {
        if (bookId == null || topic == null || topic.trim().isEmpty()) {
            return;
        }
        int bookNumber = resolveBookNumber(null, bookId);
        String label = topic.trim();
        if (member) {
            bibleDao.upsertTopicVerse(new BibleTopicVerseEntity(label, bookNumber, chapterNumber, verseNumber));
        } else {
            bibleDao.deleteTopicVerse(label, bookNumber, chapterNumber, verseNumber);
        }
    }

    @Override
    public List<BibleVerse> getTopicVerses(String topic) {
        ensureAssetsReady();
        String translationId = resolveDefaultTranslationId();
        List<BibleVerse> verses = new ArrayList<>();
        for (BibleTopicVerseEntity link : bibleDao.getTopicVerses(topic)) {
            BibleVerseEntity entity = reader.getVerse(translationId, link.getBookNumber(),
                    link.getChapterNumber(), link.getVerseNumber());
            if (entity != null) {
                String bookId = BibleDataSeeder.canonicalBookId(link.getBookNumber());
                verses.add(BibleMapper.toDomain(entity, translationId, bookId));
            }
        }
        return verses;
    }

    @Override
    public List<BibleNote> getNotes() {
        List<BibleNote> notes = new ArrayList<>();
        if (noteDao == null) {
            return notes;
        }
        Map<Long, List<VerseLocation>> linksByNote = new HashMap<>();
        for (BibleNoteLinkEntity link : noteDao.getAllLinks()) {
            List<VerseLocation> list = linksByNote.get(link.getNoteId());
            if (list == null) {
                list = new ArrayList<>();
                linksByNote.put(link.getNoteId(), list);
            }
            list.add(new VerseLocation(link.getBookNumber(), link.getChapterNumber(), link.getVerseNumber()));
        }
        for (BibleNoteEntity note : noteDao.getAllNotes()) {
            List<VerseLocation> links = linksByNote.get(note.getId());
            if (links == null) {
                links = new ArrayList<>();
            }
            Collections.sort(links);
            List<String> topicNames = new ArrayList<>();
            if (note.getTopics() != null) {
                for (String name : note.getTopics().split(",")) {
                    if (!name.trim().isEmpty()) {
                        topicNames.add(name.trim());
                    }
                }
            }
            notes.add(new BibleNote(note.getId(), note.getBody(), note.getUpdatedMillis(), links, topicNames));
        }
        return notes;
    }

    @Override
    public long saveNote(long id, String body, List<VerseLocation> links, List<String> topics) {
        if (noteDao == null) {
            return 0L;
        }
        String text = body == null ? "" : body;
        long now = System.currentTimeMillis();
        String topicText = joinTopics(topics);
        long noteId = id;
        if (id == 0L) {
            noteId = noteDao.insertNote(new BibleNoteEntity(0L, text, now, topicText));
        } else {
            noteDao.updateNote(id, text, now, topicText);
        }
        noteDao.deleteLinks(noteId);
        List<BibleNoteLinkEntity> rows = new ArrayList<>();
        for (VerseLocation loc : links) {
            rows.add(new BibleNoteLinkEntity(noteId, loc.bookNumber, loc.chapter, loc.verse));
        }
        if (!rows.isEmpty()) {
            noteDao.insertLinks(rows);
        }
        return noteId;
    }

    private static String joinTopics(List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String topic : topics) {
            String clean = topic.trim().replace(",", " ");
            if (!clean.isEmpty()) {
                if (out.length() > 0) {
                    out.append(',');
                }
                out.append(clean);
            }
        }
        return out.toString();
    }

    @Override
    public void deleteNote(long id) {
        if (noteDao == null || id == 0L) {
            return;
        }
        noteDao.deleteLinks(id);
        noteDao.deleteNote(id);
    }

    private Map<Integer, String> highlightsFor(int bookNumber, int chapterNumber) {
        Map<Integer, String> colours = new HashMap<>();
        if (bibleDao == null) {
            return colours;
        }
        List<BibleVerseHighlightEntity> rows = bibleDao.getHighlightsForChapter(bookNumber, chapterNumber);
        if (rows != null) {
            for (BibleVerseHighlightEntity row : rows) {
                colours.put(row.getVerseNumber(), row.getColor());
            }
        }
        return colours;
    }

    private static String referenceKey(int bookNumber, int chapterNumber, int verseNumber) {
        return bookNumber + "." + chapterNumber + "." + verseNumber;
    }

    // --- Helper Methods ---

    private String resolveDefaultTranslationId() {
        BibleTranslationEntity defaultTranslation = bibleDao.getDefaultTranslation();
        if (defaultTranslation != null && defaultTranslation.getId() != null && !defaultTranslation.getId().isEmpty()) {
            return defaultTranslation.getId();
        }
        List<BibleTranslationEntity> translations = bibleDao.getTranslations();
        if (translations != null && !translations.isEmpty()) {
            BibleTranslationEntity first = translations.get(0);
            if (first != null && first.getId() != null && !first.getId().isEmpty()) {
                return first.getId();
            }
        }
        return "KJV";
    }

    private int resolveBookNumber(String translationId, String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return 1;
        }
        String clean = bookId.trim();

        // 1. Direct integer parse
        try {
            int parsed = Integer.parseInt(clean);
            if (parsed >= 1 && parsed <= 66) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        // 2. Canonical book mapping
        int canonical = BibleMapper.resolveBookNumber(clean);
        if (canonical >= 1 && canonical <= 66 && !clean.equalsIgnoreCase("GEN") && canonical != 1) {
            return canonical;
        }
        if (clean.equalsIgnoreCase("GEN") || clean.equalsIgnoreCase("GENESIS")) {
            return 1;
        }

        // 3. Look up from DB books by translationId
        String transId = (translationId != null && !translationId.isEmpty()) ? translationId : "KJV";
        List<BibleBookEntity> books = bibleDao.getBooksByTranslation(transId);
        if (books != null) {
            for (BibleBookEntity b : books) {
                if (clean.equalsIgnoreCase(b.getId()) || clean.equalsIgnoreCase(b.getName())) {
                    return b.getBookNumber();
                }
            }
        }

        return canonical;
    }
}
