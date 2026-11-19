package org.perazimchurch.app.data.repository;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseHighlightEntity;
import org.perazimchurch.app.data.local.entity.BibleTopicVerseEntity;
import org.perazimchurch.app.data.local.seeder.BibleDataSeeder;
import org.perazimchurch.app.data.mapper.BibleMapper;
import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.data.interlinear.GreekInterlinearStore;
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

    public RoomBibleRepository(BibleDao bibleDao) {
        this.bibleDao = bibleDao;
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
        this.assetContext = context != null ? context.getApplicationContext() : null;
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
            verses = greekInterlinearChapter(bookNumber, chapterNumber, bookId);
        } else {
            List<BibleVerseEntity> entities = bibleDao.getVerses(resolvedTranslation, bookNumber, chapterNumber);
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
        BibleVerseEntity entity = bibleDao.getVerse(resolvedTranslation, bookNumber, chapterNumber, verseNumber);

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
        List<BibleVerseEntity> entities = bibleDao.searchVerses(resolvedTranslation, cleanQuery);
        if ((entities == null || entities.isEmpty()) && cleanQuery.contains("-")) {
            entities = bibleDao.searchVerses(resolvedTranslation, cleanQuery.replace("-", " ").trim());
        } else if ((entities == null || entities.isEmpty()) && cleanQuery.contains(" ")) {
            entities = bibleDao.searchVerses(resolvedTranslation, cleanQuery.replace(" ", "-").trim());
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
        bibleDao.updateBookmark(verseId, isFavorite);
        BibleVerseEntity entity = bibleDao.getVerseById(verseId);
        if (entity != null) {
            entity.setFavorite(isFavorite);
            bibleDao.updateVerse(entity);
        } else {
            // Fallback resolution for identifiers like "book.chapter.verse" or "2SA.5.20" or "KJV.2SA.5.20"
            String[] parts = verseId.split("\\.");
            if (parts.length == 3) {
                try {
                    int bNum = resolveBookNumber(null, parts[0]);
                    int cNum = Integer.parseInt(parts[1]);
                    int vNum = Integer.parseInt(parts[2]);
                    BibleVerseEntity verseEntity = bibleDao.getVerse(bNum, cNum, vNum);
                    if (verseEntity != null) {
                        verseEntity.setFavorite(isFavorite);
                        bibleDao.updateVerse(verseEntity);
                    }
                } catch (Exception ignored) {
                }
            } else if (parts.length == 4) {
                try {
                    int bNum = resolveBookNumber(parts[0], parts[1]);
                    int cNum = Integer.parseInt(parts[2]);
                    int vNum = Integer.parseInt(parts[3]);
                    BibleVerseEntity verseEntity = bibleDao.getVerse(bNum, cNum, vNum);
                    if (verseEntity != null) {
                        verseEntity.setFavorite(isFavorite);
                        bibleDao.updateVerse(verseEntity);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void saveVerseNote(String verseId, String note) {
        if (verseId == null || verseId.trim().isEmpty()) {
            return;
        }
        bibleDao.updateVerseNote(verseId, note);
        BibleVerseEntity entity = bibleDao.getVerseById(verseId);
        if (entity != null) {
            entity.setNote(note);
            bibleDao.updateVerse(entity);
        } else {
            // Fallback resolution for identifiers like "book.chapter.verse" or "2SA.5.20" or "KJV.2SA.5.20"
            String[] parts = verseId.split("\\.");
            if (parts.length == 3) {
                try {
                    int bNum = resolveBookNumber(null, parts[0]);
                    int cNum = Integer.parseInt(parts[1]);
                    int vNum = Integer.parseInt(parts[2]);
                    BibleVerseEntity verseEntity = bibleDao.getVerse(bNum, cNum, vNum);
                    if (verseEntity != null) {
                        verseEntity.setNote(note);
                        bibleDao.updateVerse(verseEntity);
                    }
                } catch (Exception ignored) {
                }
            } else if (parts.length == 4) {
                try {
                    int bNum = resolveBookNumber(parts[0], parts[1]);
                    int cNum = Integer.parseInt(parts[2]);
                    int vNum = Integer.parseInt(parts[3]);
                    BibleVerseEntity verseEntity = bibleDao.getVerse(bNum, cNum, vNum);
                    if (verseEntity != null) {
                        verseEntity.setNote(note);
                        bibleDao.updateVerse(verseEntity);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Pseudo-translation id for the Greek New Testament interlinear. */
    public static final String GREEK_INTERLINEAR = "GRK";

    @Override
    public List<BibleTranslation> getTranslationsWithText() {
        ensureAssetsReady();
        List<BibleTranslation> readable = new ArrayList<>();
        for (BibleTranslation translation : getAvailableTranslations()) {
            if (translation.getId() != null && bibleDao.countVerses(translation.getId()) > 0) {
                translation.setDownloaded(true);
                readable.add(translation);
            }
        }
        BibleTranslation greek = new BibleTranslation(GREEK_INTERLINEAR, "Greek Interlinear (New Testament)", "el", true);
        readable.add(greek);
        return readable;
    }

    /** Verse list for the Greek interlinear: each verse shows its words with Strong's numbers and parsing. */
    private List<BibleVerse> greekInterlinearChapter(int bookNumber, int chapterNumber, String bookId) {
        List<BibleVerse> verses = new ArrayList<>();
        if (bookNumber < 40) {
            return verses;
        }
        for (BibleVerseEntity entity : bibleDao.getVerses("KJV", bookNumber, chapterNumber)) {
            List<InterlinearWord> words = GreekInterlinearStore.getVerse(assetContext, bookNumber, chapterNumber, entity.getVerseNumber());
            if (words.isEmpty()) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            for (InterlinearWord word : words) {
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append(word.getOriginal());
                if (word.getStrongsNumber() > 0) {
                    text.append(" G").append(word.getStrongsNumber());
                }
                if (!word.getMorphology().isEmpty()) {
                    text.append(" (").append(word.getMorphology()).append(')');
                }
            }
            BibleVerse verse = BibleMapper.toDomain(entity, GREEK_INTERLINEAR, bookId);
            verse.setText(text.toString());
            verses.add(verse);
        }
        return verses;
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
            BibleVerseEntity entity = bibleDao.getVerseInTranslation(translationId, highlight.getBookNumber(),
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
            BibleVerseEntity entity = bibleDao.getVerseInTranslation(translation.getId(), bookNumber,
                    chapterNumber, verseNumber);
            if (entity != null) {
                verses.add(BibleMapper.toDomain(entity, translation.getId(), bookId));
            }
        }
        return verses;
    }

    @Override
    public List<InterlinearWord> getGreekInterlinear(String bookId, int chapterNumber, int verseNumber) {
        int bookNumber = resolveBookNumber(null, bookId);
        if (bookNumber < 40) {
            return Collections.emptyList();
        }
        return GreekInterlinearStore.getVerse(assetContext, bookNumber, chapterNumber, verseNumber);
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
            BibleVerseEntity entity = bibleDao.getVerseInTranslation(translationId, link.getBookNumber(),
                    link.getChapterNumber(), link.getVerseNumber());
            if (entity != null) {
                String bookId = BibleDataSeeder.canonicalBookId(link.getBookNumber());
                verses.add(BibleMapper.toDomain(entity, translationId, bookId));
            }
        }
        return verses;
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
