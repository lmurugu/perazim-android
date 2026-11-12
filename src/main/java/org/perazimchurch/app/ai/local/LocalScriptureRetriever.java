package org.perazimchurch.app.ai.local;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleBookEntity;
import org.perazimchurch.app.data.local.entity.BibleTranslationEntity;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Retrieves only locally stored public-domain KJV verses for grounded offline answers. */
public final class LocalScriptureRetriever {
    private static final String TRANSLATION_ID = "KJV";
    private static final int MAX_TERMS = 6;
    private static final int MAX_PASSAGES = 3;
    private static final int MAX_TOTAL_CHARS = 900;
    private static final Pattern REFERENCE_SHAPE = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:[1-3]\\s*)?[\\p{L}]+(?:\\s+[\\p{L}]+){0,3}\\s+[1-9]\\d{0,2}:[1-9]\\d{0,2}(?:\\s*[-–]\\s*[1-9]\\d{0,2})?(?!\\d)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Set<String> SCRIPTURE_TERMS = new HashSet<>(Arrays.asList(
            "bible", "scripture", "verse", "verses", "chapter", "kjv", "god", "jesus", "christ",
            "messiah", "spirit", "sin", "sins", "salvation", "saved", "redeem", "forgive",
            "forgiveness", "faith", "prayer", "pray", "grace", "heaven", "hell", "gospel",
            "resurrection", "disciple", "disciples", "worship", "lord", "love"));
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "about", "after", "also", "and", "are", "because", "bible", "can", "could", "does",
            "for", "from", "give", "help", "how", "into", "just", "like", "please", "read",
            "show", "tell", "that", "the", "their", "them", "there", "these", "they", "this",
            "those", "through", "what", "when", "where", "which", "who", "with", "would", "you"));

    private LocalScriptureRetriever() {}

    @NonNull
    public static JSONArray retrieve(@NonNull Context context, String question) throws Exception {
        return retrieve(PerazimDatabase.getInstance(context).bibleDao(), question);
    }

    static JSONArray retrieve(BibleDao dao, String question) throws Exception {
        if (question == null || question.trim().isEmpty() || !isScriptureRelated(question)) return new JSONArray();
        BibleTranslationEntity translation = dao.getTranslationById(TRANSLATION_ID);
        if (translation == null || !"Public Domain".equalsIgnoreCase(translation.getLicenseInfo())) {
            return new JSONArray();
        }

        List<String> terms = queryTerms(question);
        Map<Integer, String> names = new HashMap<>();
        List<BibleBookEntity> books = dao.getBooksByTranslation(TRANSLATION_ID);
        if (books != null) {
            for (BibleBookEntity book : books) {
                if (book.getName() != null) names.put(book.getBookNumber(), book.getName());
            }
        }

        JSONArray exactReference = retrieveExactReference(dao, books, question);
        if (exactReference != null) return exactReference;
        if (terms.isEmpty()) return new JSONArray();

        Map<String, Candidate> candidates = new HashMap<>();
        for (int first = 0; first < terms.size(); first++) {
            for (int second = first + 1; second < terms.size(); second++) {
                List<BibleVerseEntity> matches = dao.searchVersesByTerms(TRANSLATION_ID,
                        terms.get(first), terms.get(second));
                if (matches == null) continue;
                for (BibleVerseEntity verse : matches) {
                    if (verse == null || verse.getText() == null || verse.getText().trim().isEmpty()) continue;
                    String id = verse.getBookNumber() + ":" + verse.getChapterNumber() + ":" + verse.getVerseNumber();
                    Candidate candidate = candidates.get(id);
                    if (candidate == null) {
                        String book = names.get(verse.getBookNumber());
                        if (book == null || verse.getChapterNumber() <= 0 || verse.getVerseNumber() <= 0) continue;
                        candidate = new Candidate(book, verse);
                        candidates.put(id, candidate);
                    }
                }
            }
        }
        for (Candidate candidate : candidates.values()) {
            String text = candidate.verse.getText().toLowerCase(Locale.ROOT);
            for (String term : terms) if (text.contains(term)) candidate.score++;
        }

        List<Candidate> ranked = new ArrayList<>(candidates.values());
        Collections.sort(ranked, Comparator.comparingInt((Candidate item) -> item.score).reversed()
                .thenComparingInt(item -> item.verse.getBookNumber())
                .thenComparingInt(item -> item.verse.getChapterNumber())
                .thenComparingInt(item -> item.verse.getVerseNumber()));

        JSONArray evidence = new JSONArray();
        int characters = 0;
        for (Candidate candidate : ranked) {
            String text = candidate.verse.getText().trim();
            String location = candidate.book + " " + candidate.verse.getChapterNumber()
                    + ":" + candidate.verse.getVerseNumber();
            int entryChars = text.length() + location.length();
            if (entryChars > 600 || characters + entryChars > MAX_TOTAL_CHARS) continue;
            JSONObject item = new JSONObject();
            item.put("sourceId", "Bible");
            item.put("title", "King James Version");
            item.put("authorityClass", "SCRIPTURE");
            item.put("version", TRANSLATION_ID);
            item.put("location", location);
            item.put("text", text);
            evidence.put(item);
            characters += entryChars;
            if (evidence.length() == MAX_PASSAGES) break;
        }
        return evidence;
    }

    static boolean isScriptureRelated(String question) {
        if (question == null) return false;
        if (hasExplicitReference(question)) return true;
        String normalized = question.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        if (normalized.isEmpty()) return false;
        for (String term : normalized.split("\\s+")) if (SCRIPTURE_TERMS.contains(term)) return true;
        return false;
    }

    static boolean hasExplicitReference(String question) {
        return question != null && REFERENCE_SHAPE.matcher(question).find();
    }

    private static JSONArray retrieveExactReference(BibleDao dao, List<BibleBookEntity> books,
                                                    String question) throws Exception {
        if (books == null || books.isEmpty() || !REFERENCE_SHAPE.matcher(question).find()) return null;
        BibleBookEntity matchBook = null;
        int chapter = -1;
        int verseNumber = -1;
        int requestedEnd = -1;
        List<BibleBookEntity> referenceBooks = new ArrayList<>(books);
        Collections.sort(referenceBooks, Comparator.comparingInt(
                (BibleBookEntity book) -> book.getName() == null ? 0 : book.getName().length()).reversed());
        for (BibleBookEntity book : referenceBooks) {
            String name = book.getName();
            if (name == null || name.trim().isEmpty()) continue;
            StringBuilder namePattern = new StringBuilder();
            String[] words = name.trim().split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (i > 0) namePattern.append("\\s+");
                namePattern.append(Pattern.quote(words[i]));
            }
            Pattern exact = Pattern.compile("(?<![\\p{L}\\p{N}])" + namePattern
                    + "\\s+([1-9]\\d{0,2}):([1-9]\\d{0,2})(?:\\s*[-–]\\s*([1-9]\\d{0,2}))?(?!\\d)",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = exact.matcher(question);
            if (matcher.find()) {
                matchBook = book;
                chapter = Integer.parseInt(matcher.group(1));
                verseNumber = Integer.parseInt(matcher.group(2));
                String rangeEnd = matcher.group(3);
                if (rangeEnd != null) {
                    requestedEnd = Integer.parseInt(rangeEnd);
                    if (requestedEnd < verseNumber || requestedEnd - verseNumber + 1 > MAX_PASSAGES) {
                        return new JSONArray();
                    }
                }
                break;
            }
        }
        if (matchBook == null) return null;
        List<BibleVerseEntity> chapterVerses = dao.getVerses(TRANSLATION_ID, matchBook.getBookNumber(), chapter);
        if (chapterVerses == null || chapterVerses.isEmpty()) return new JSONArray();
        int targetIndex = -1;
        for (int i = 0; i < chapterVerses.size(); i++) {
            if (chapterVerses.get(i).getVerseNumber() == verseNumber) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) return new JSONArray();
        JSONArray evidence = new JSONArray();
        int requestedCount = requestedEnd < 0 ? 1 : requestedEnd - verseNumber + 1;
        int first = requestedCount == 1 ? Math.max(0, targetIndex - 1) : targetIndex;
        int last = requestedCount == 1 ? Math.min(chapterVerses.size() - 1, targetIndex + 1)
                : targetIndex + requestedCount - 1;
        if (last >= chapterVerses.size()) return new JSONArray();
        for (int i = targetIndex; i <= last && requestedCount > 1; i++) {
            if (chapterVerses.get(i).getVerseNumber() != verseNumber + i - targetIndex) return new JSONArray();
        }
        int characters = 0;
        for (int i = first; i <= last && evidence.length() < MAX_PASSAGES; i++) {
            BibleVerseEntity verse = chapterVerses.get(i);
            String text = verse.getText() == null ? "" : verse.getText().trim();
            if (text.isEmpty()) continue;
            String location = matchBook.getName() + " " + chapter + ":" + verse.getVerseNumber();
            int entryChars = text.length() + location.length();
            if (entryChars > 600 || characters + entryChars > MAX_TOTAL_CHARS) continue;
            JSONObject item = new JSONObject();
            item.put("sourceId", "Bible");
            item.put("title", "King James Version");
            item.put("authorityClass", "SCRIPTURE");
            item.put("version", TRANSLATION_ID);
            item.put("location", location);
            item.put("text", text);
            boolean requestedPassage = verse.getVerseNumber() >= verseNumber
                    && verse.getVerseNumber() <= (requestedEnd < 0 ? verseNumber : requestedEnd);
            item.put("citationEligible", requestedPassage);
            evidence.put(item);
            characters += entryChars;
        }
        return evidence;
    }

    private static List<String> queryTerms(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ");
        String[] tokens = normalized.trim().split("\\s+");
        List<String> terms = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String token : tokens) {
            if (token.length() < 3 || STOP_WORDS.contains(token) || !seen.add(token)) continue;
            terms.add(token);
            if (terms.size() == MAX_TERMS) break;
        }
        return terms;
    }

    private static final class Candidate {
        final String book;
        final BibleVerseEntity verse;
        int score;

        Candidate(String book, BibleVerseEntity verse) {
            this.book = book;
            this.verse = verse;
        }
    }
}
