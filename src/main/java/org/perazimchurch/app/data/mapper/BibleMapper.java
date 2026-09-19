package org.perazimchurch.app.data.mapper;

import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleChapterEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleChapter;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleVerse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mapper for Holy Scripture entities and domain models.
 * Compliant with Guidebook §34 and Phase 1.2 Architecture Target.
 */
public final class BibleMapper {

    private static final String[] BOOK_NUMBER_TO_ID = new String[]{
            "",
            "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA",
            "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO",
            "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO",
            "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL", "MAT",
            "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP",
            "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE",
            "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
    };

    private static final Map<String, Integer> BOOK_NAME_OR_ID_TO_NUMBER = new HashMap<>();

    static {
        String[][] bookAliases = {
                {"GEN", "GENESIS"},
                {"EXO", "EXODUS"},
                {"LEV", "LEVITICUS"},
                {"NUM", "NUMBERS"},
                {"DEU", "DEUTERONOMY"},
                {"JOS", "JOSHUA"},
                {"JDG", "JUDGES"},
                {"RUT", "RUTH"},
                {"1SA", "1 SAMUEL", "1SAMUEL", "FIRST SAMUEL"},
                {"2SA", "2 SAMUEL", "2SAMUEL", "SECOND SAMUEL"},
                {"1KI", "1 KINGS", "1KINGS", "FIRST KINGS"},
                {"2KI", "2 KINGS", "2KINGS", "SECOND KINGS"},
                {"1CH", "1 CHRONICLES", "1CHRONICLES", "FIRST CHRONICLES"},
                {"2CH", "2 CHRONICLES", "2CHRONICLES", "SECOND CHRONICLES"},
                {"EZR", "EZRA"},
                {"NEH", "NEHEMIAH"},
                {"EST", "ESTHER"},
                {"JOB", "JOB"},
                {"PSA", "PSALMS", "PSALM"},
                {"PRO", "PROVERBS"},
                {"ECC", "ECCLESIASTES"},
                {"SNG", "SONG OF SOLOMON", "SONG OF SONGS", "CANTICLES"},
                {"ISA", "ISAIAH"},
                {"JER", "JEREMIAH"},
                {"LAM", "LAMENTATIONS"},
                {"EZK", "EZEKIEL"},
                {"DAN", "DANIEL"},
                {"HOS", "HOSEA"},
                {"JOL", "JOEL"},
                {"AMO", "AMOS"},
                {"OBA", "OBADIAH"},
                {"JON", "JONAH"},
                {"MIC", "MICAH"},
                {"NAM", "NAHUM"},
                {"HAB", "HABAKKUK"},
                {"ZEP", "ZEPHANIAH"},
                {"HAG", "HAGGAI"},
                {"ZEC", "ZECHARIAH"},
                {"MAL", "MALACHI"},
                {"MAT", "MATTHEW"},
                {"MRK", "MARK"},
                {"LUK", "LUKE"},
                {"JHN", "JOHN"},
                {"ACT", "ACTS"},
                {"ROM", "ROMANS"},
                {"1CO", "1 CORINTHIANS", "1CORINTHIANS", "FIRST CORINTHIANS"},
                {"2CO", "2 CORINTHIANS", "2CORINTHIANS", "SECOND CORINTHIANS"},
                {"GAL", "GALATIANS"},
                {"EPH", "EPHESIANS"},
                {"PHP", "PHILIPPIANS"},
                {"COL", "COLOSSIANS"},
                {"1TH", "1 THESSALONIANS", "1THESSALONIANS", "FIRST THESSALONIANS"},
                {"2TH", "2 THESSALONIANS", "2THESSALONIANS", "SECOND THESSALONIANS"},
                {"1TI", "1 TIMOTHY", "1TIMOTHY", "FIRST TIMOTHY"},
                {"2TI", "2 TIMOTHY", "2TIMOTHY", "SECOND TIMOTHY"},
                {"TIT", "TITUS"},
                {"PHM", "PHILEMON"},
                {"HEB", "HEBREWS"},
                {"JAS", "JAMES"},
                {"1PE", "1 PETER", "1PETER", "FIRST PETER"},
                {"2PE", "2 PETER", "2PETER", "SECOND PETER"},
                {"1JN", "1 JOHN", "1JOHN", "FIRST JOHN"},
                {"2JN", "2 JOHN", "2JOHN", "SECOND JOHN"},
                {"3JN", "3 JOHN", "3JOHN", "THIRD JOHN"},
                {"JUD", "JUDE"},
                {"REV", "REVELATION", "REVELATIONS"}
        };

        for (int i = 0; i < bookAliases.length; i++) {
            int bookNumber = i + 1;
            for (String alias : bookAliases[i]) {
                BOOK_NAME_OR_ID_TO_NUMBER.put(alias.toUpperCase(Locale.US), bookNumber);
            }
        }
    }

    private BibleMapper() {
        // Prevent instantiation of utility mapper class
    }

    // --- Translation Mappings ---

    public static BibleTranslation toDomain(BibleTranslationEntity entity) {
        if (entity == null) {
            return null;
        }
        String name = entity.getFullName();
        if (name == null || name.trim().isEmpty()) {
            name = entity.getShortName();
        }
        if (name == null || name.trim().isEmpty()) {
            name = entity.getId();
        }
        return new BibleTranslation(
                entity.getId(),
                name,
                entity.getLanguage() != null ? entity.getLanguage() : "en",
                true
        );
    }

    public static BibleTranslationEntity toEntity(BibleTranslation domain) {
        if (domain == null) {
            return null;
        }
        String id = domain.getId() != null ? domain.getId() : "";
        String name = domain.getName() != null ? domain.getName() : id;
        String lang = domain.getLanguage() != null ? domain.getLanguage() : "en";
        return new BibleTranslationEntity(
                id,
                id,
                name,
                lang,
                "",
                false,
                ""
        );
    }

    public static List<BibleTranslation> toDomainTranslationList(List<BibleTranslationEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<BibleTranslation> list = new ArrayList<>(entities.size());
        for (BibleTranslationEntity entity : entities) {
            BibleTranslation domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<BibleTranslationEntity> toEntityTranslationList(List<BibleTranslation> domainList) {
        if (domainList == null) {
            return Collections.emptyList();
        }
        List<BibleTranslationEntity> list = new ArrayList<>(domainList.size());
        for (BibleTranslation domain : domainList) {
            BibleTranslationEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }

    // --- Book Mappings ---

    public static BibleBook toDomain(BibleBookEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BibleBook(
                entity.getId(),
                entity.getName(),
                entity.getBookNumber(),
                entity.getTestament(),
                entity.getChapterCount()
        );
    }

    public static BibleBookEntity toEntity(BibleBook domain) {
        return toEntity(domain, "KJV");
    }

    public static BibleBookEntity toEntity(BibleBook domain, String translationId) {
        if (domain == null) {
            return null;
        }
        String id = domain.getId() != null ? domain.getId() : getBookIdFromNumber(domain.getBookNumber());
        return new BibleBookEntity(
                id,
                translationId != null ? translationId : "KJV",
                domain.getBookNumber(),
                domain.getName() != null ? domain.getName() : "",
                domain.getTestament() != null ? domain.getTestament() : "",
                domain.getChapterCount()
        );
    }

    public static List<BibleBook> toDomainBookList(List<BibleBookEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<BibleBook> list = new ArrayList<>(entities.size());
        for (BibleBookEntity entity : entities) {
            BibleBook domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<BibleBookEntity> toEntityBookList(List<BibleBook> domainList) {
        return toEntityBookList(domainList, "KJV");
    }

    public static List<BibleBookEntity> toEntityBookList(List<BibleBook> domainList, String translationId) {
        if (domainList == null) {
            return Collections.emptyList();
        }
        List<BibleBookEntity> list = new ArrayList<>(domainList.size());
        for (BibleBook domain : domainList) {
            BibleBookEntity entity = toEntity(domain, translationId);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }

    // --- Chapter Mappings ---

    public static BibleChapter toDomain(BibleChapterEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BibleChapter(
                entity.getId(),
                entity.getBookId(),
                entity.getChapterNumber(),
                entity.getVerseCount()
        );
    }

    public static BibleChapterEntity toEntity(BibleChapter domain) {
        if (domain == null) {
            return null;
        }
        String id = domain.getId() != null ? domain.getId() : (domain.getBookId() + "." + domain.getChapterNumber());
        return new BibleChapterEntity(
                id,
                domain.getBookId() != null ? domain.getBookId() : "",
                domain.getChapterNumber(),
                domain.getVerseCount()
        );
    }

    // --- Verse Mappings ---

    public static BibleVerse toDomain(BibleVerseEntity entity) {
        return toDomain(entity, "KJV", null);
    }

    public static BibleVerse toDomain(BibleVerseEntity entity, String translationId) {
        return toDomain(entity, translationId, null);
    }

    public static BibleVerse toDomain(BibleVerseEntity entity, String translationId, String bookId) {
        if (entity == null) {
            return null;
        }
        String resolvedTransId = (translationId != null && !translationId.isEmpty()) ? translationId : "KJV";
        String resolvedBookId = bookId;
        if (resolvedBookId == null || resolvedBookId.isEmpty()) {
            resolvedBookId = extractBookId(entity);
        }
        return new BibleVerse(
                entity.getId(),
                resolvedTransId,
                resolvedBookId,
                entity.getChapterNumber(),
                entity.getVerseNumber(),
                entity.getText(),
                entity.isFavorite(),
                entity.getNote()
        );
    }

    public static BibleVerseEntity toEntity(BibleVerse domain) {
        if (domain == null) {
            return null;
        }
        String bId = domain.getBookId() != null ? domain.getBookId() : "GEN";
        String id = domain.getId();
        if (id == null || id.trim().isEmpty()) {
            id = bId + "." + domain.getChapterNumber() + "." + domain.getVerseNumber();
        }
        String chapterId = bId + "." + domain.getChapterNumber();
        int bookNumber = resolveBookNumber(bId);
        return new BibleVerseEntity(
                id,
                chapterId,
                bookNumber,
                domain.getChapterNumber(),
                domain.getVerseNumber(),
                domain.getText() != null ? domain.getText() : "",
                domain.isFavorite(),
                domain.getNote()
        );
    }

    public static List<BibleVerse> toDomainVerseList(List<BibleVerseEntity> entities) {
        return toDomainVerseList(entities, "KJV", null);
    }

    public static List<BibleVerse> toDomainVerseList(List<BibleVerseEntity> entities, String translationId) {
        return toDomainVerseList(entities, translationId, null);
    }

    public static List<BibleVerse> toDomainVerseList(List<BibleVerseEntity> entities, String translationId, String bookId) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<BibleVerse> list = new ArrayList<>(entities.size());
        for (BibleVerseEntity entity : entities) {
            BibleVerse domain = toDomain(entity, translationId, bookId);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<BibleVerseEntity> toEntityVerseList(List<BibleVerse> domainList) {
        if (domainList == null) {
            return Collections.emptyList();
        }
        List<BibleVerseEntity> list = new ArrayList<>(domainList.size());
        for (BibleVerse domain : domainList) {
            BibleVerseEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }

    // --- Helper Utilities ---

    public static String extractBookId(BibleVerseEntity entity) {
        if (entity == null) {
            return "GEN";
        }
        if (entity.getChapterId() != null && entity.getChapterId().contains(".")) {
            String[] parts = entity.getChapterId().split("\\.");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                return parts[0].trim().toUpperCase(Locale.US);
            }
        }
        if (entity.getId() != null && entity.getId().contains(".")) {
            String[] parts = entity.getId().split("\\.");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                return parts[0].trim().toUpperCase(Locale.US);
            }
        }
        return getBookIdFromNumber(entity.getBookNumber());
    }

    public static int resolveBookNumber(String bookIdOrName) {
        if (bookIdOrName == null || bookIdOrName.trim().isEmpty()) {
            return 1;
        }
        String clean = bookIdOrName.trim().toUpperCase(Locale.US);
        try {
            int parsed = Integer.parseInt(clean);
            if (parsed >= 1 && parsed <= 66) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        Integer canonical = BOOK_NAME_OR_ID_TO_NUMBER.get(clean);
        if (canonical != null) {
            return canonical;
        }
        return 1;
    }

    public static String getBookIdFromNumber(int bookNumber) {
        if (bookNumber >= 1 && bookNumber < BOOK_NUMBER_TO_ID.length) {
            return BOOK_NUMBER_TO_ID[bookNumber];
        }
        return String.valueOf(bookNumber);
    }
}
