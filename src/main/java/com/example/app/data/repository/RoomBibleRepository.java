package com.example.app.data.repository;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.dao.BibleDao;
import com.example.app.data.local.entity.BibleBookEntity;
import com.example.app.data.local.entity.BibleTranslationEntity;
import com.example.app.data.local.entity.BibleVerseEntity;
import com.example.app.data.mapper.BibleMapper;
import com.example.app.domain.model.BibleBook;
import com.example.app.domain.model.BibleTranslation;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.repository.BibleRepository;

import java.util.Collections;
import java.util.List;

/**
 * Room-backed repository implementation for Holy Scripture.
 * Manages translations, books, verses, bookmarks, and verse notes.
 * Compliant with Guidebook §34 and Phase 1.2 Architecture Target.
 */
public class RoomBibleRepository implements BibleRepository {

    private final BibleDao bibleDao;

    public RoomBibleRepository(BibleDao bibleDao) {
        this.bibleDao = bibleDao;
    }

    public RoomBibleRepository(PerazimDatabase database) {
        this(database.bibleDao());
    }

    @Override
    public List<BibleTranslation> getAvailableTranslations() {
        List<BibleTranslationEntity> entities = bibleDao.getTranslations();
        return BibleMapper.toDomainTranslationList(entities);
    }

    @Override
    public List<BibleBook> getBooks() {
        String translationId = resolveDefaultTranslationId();
        List<BibleBookEntity> bookEntities = bibleDao.getBooksByTranslation(translationId);
        if ((bookEntities == null || bookEntities.isEmpty()) && !"KJV".equalsIgnoreCase(translationId)) {
            // Fallback to KJV if default translation returned no books
            bookEntities = bibleDao.getBooksByTranslation("KJV");
        }
        return BibleMapper.toDomainBookList(bookEntities);
    }

    @Override
    public List<BibleVerse> getVersesForChapter(String translationId, String bookId, int chapterNumber) {
        int bookNumber = resolveBookNumber(translationId, bookId);
        List<BibleVerseEntity> entities = bibleDao.getVerses(bookNumber, chapterNumber);
        String resolvedTranslation = (translationId != null && !translationId.isEmpty())
                ? translationId
                : resolveDefaultTranslationId();
        return BibleMapper.toDomainVerseList(entities, resolvedTranslation, bookId);
    }

    @Override
    public BibleVerse getVerse(String translationId, String bookId, int chapterNumber, int verseNumber) {
        int bookNumber = resolveBookNumber(translationId, bookId);
        BibleVerseEntity entity = bibleDao.getVerse(bookNumber, chapterNumber, verseNumber);
        if (entity == null) {
            return null;
        }
        String resolvedTranslation = (translationId != null && !translationId.isEmpty())
                ? translationId
                : resolveDefaultTranslationId();
        return BibleMapper.toDomain(entity, resolvedTranslation, bookId);
    }

    @Override
    public List<BibleVerse> searchScripture(String translationId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<BibleVerseEntity> entities = bibleDao.searchVerses(query.trim());
        String resolvedTranslation = (translationId != null && !translationId.isEmpty())
                ? translationId
                : resolveDefaultTranslationId();
        return BibleMapper.toDomainVerseList(entities, resolvedTranslation);
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
            // Fallback resolution for identifiers like "book.chapter.verse" or "2SA.5.20"
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
            // Fallback resolution for identifiers like "book.chapter.verse" or "2SA.5.20"
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
            }
        }
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
