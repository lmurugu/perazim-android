package org.perazimchurch.app.ai.remote;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extracts up to 3 relevant scripture passages from the local Room Bible database
 * based on keyword matching in the user query, capped at 600 total characters.
 * Missing local results remain empty; the assistant must not receive memorized substitute verses.
 */
public final class BibleContextExtractor {

    private static final int MAX_PASSAGES = 3;
    private static final int MAX_TOTAL_CHARS = 600;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "cannot", "could", "did", "do", "does", "doing", "down", "during", "each",
            "few", "for", "from", "further", "had", "has", "have", "having", "he", "her", "here",
            "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it",
            "its", "itself", "me", "more", "most", "my", "myself", "no", "nor", "not", "of", "off",
            "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over",
            "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their",
            "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through",
            "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where",
            "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself",
            "yourselves", "give", "tell", "show", "please", "verse", "verses", "bible"
    ));

    private BibleContextExtractor() {}

    @NonNull
    public static List<String> extractRelevantPassages(@NonNull Context context, @Nullable String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return getFallbackPassages();
        }

        try {
            PerazimDatabase db = PerazimDatabase.getInstance(context);
            BibleDao dao = db.bibleDao();

            List<String> keywords = extractKeywords(rawQuery);
            if (keywords.isEmpty()) {
                return getFallbackPassages();
            }

            Map<Integer, String> bookNames = loadBookNames(dao);
            List<String> results = new ArrayList<>();
            Set<String> seenVerseIds = new HashSet<>();
            int accumulatedChars = 0;

            for (String kw : keywords) {
                if (results.size() >= MAX_PASSAGES) break;

                List<BibleVerseEntity> matches = dao.searchVerses(kw);
                if (matches == null || matches.isEmpty()) continue;

                for (BibleVerseEntity v : matches) {
                    if (results.size() >= MAX_PASSAGES) break;
                    if (seenVerseIds.contains(v.getId())) continue;

                    String bName = bookNames.getOrDefault(v.getBookNumber(), "Book " + v.getBookNumber());
                    String formatted = String.format(Locale.US, "%s %d:%d — %s",
                            bName, v.getChapterNumber(), v.getVerseNumber(), v.getText().trim());

                    if (accumulatedChars + formatted.length() > MAX_TOTAL_CHARS && !results.isEmpty()) {
                        // Exceeds character budget; stop adding
                        break;
                    }

                    results.add(formatted);
                    seenVerseIds.add(v.getId());
                    accumulatedChars += formatted.length();
                }
            }

            return results.isEmpty() ? getFallbackPassages() : results;
        } catch (Throwable t) {
            return getFallbackPassages();
        }
    }

    private static List<String> extractKeywords(String query) {
        String cleaned = query.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase(Locale.US);
        String[] tokens = cleaned.split("\\s+");
        List<String> keywords = new ArrayList<>();

        for (String t : tokens) {
            String token = t.trim();
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private static Map<Integer, String> loadBookNames(BibleDao dao) {
        Map<Integer, String> map = new HashMap<>();
        try {
            List<BibleBookEntity> books = dao.getBooksByTranslation("KJV");
            if (books != null) {
                for (BibleBookEntity b : books) {
                    map.put(b.getBookNumber(), b.getName());
                }
            }
        } catch (Throwable ignored) {}
        return map;
    }

    @NonNull
    public static List<String> getFallbackPassages() {
        return Collections.emptyList();
    }
}
