package org.perazimchurch.app.ai.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds verses for a question typed into Bible search, such as "Where in the Bible is it written that Satan was
 * cast down?". The whole sentence rarely appears in the Bible, so the question is reduced to its content words
 * ({@link SearchTerms}). Each word is looked up with its synonyms ({@link SearchSynonyms}), and a verse counts for a
 * word only when the word appears as a whole word in one of its forms.
 *
 * <p>Verses are ranked by how many of the question's words they match, then by length (shorter first), then by
 * canonical order. The same code runs for every question. Nothing here depends on a particular one.
 */
public final class QuestionSearch {

    /** Verses returned at most. */
    static final int MAX_RESULTS = 50;

    /** A verse that contains a looked-up word, as the store returns it. */
    public static final class Hit {
        public final int bookNumber;
        public final int chapter;
        public final int verse;
        public final String text;

        public Hit(int bookNumber, int chapter, int verse, String text) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
        }
    }

    /** Finds the verses whose text contains the phrase. Partial-word matches are removed by this class. */
    public interface Lookup {
        List<Hit> find(String phrase);
    }

    /** A verse in the ranked result, with the number of the question's words it matches. */
    public static final class Ranked {
        public final int bookNumber;
        public final int chapter;
        public final int verse;
        public final int termsMatched;

        Ranked(int bookNumber, int chapter, int verse, int termsMatched) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.verse = verse;
            this.termsMatched = termsMatched;
        }
    }

    private QuestionSearch() {
    }

    public static long key(int bookNumber, int chapter, int verse) {
        return bookNumber * 1_000_000L + chapter * 1000L + verse;
    }

    /** Ranked verses for the question, at most {@link #MAX_RESULTS}. Empty when the question has no content words. */
    public static List<Ranked> rank(String query, Lookup lookup) {
        List<String> terms = SearchTerms.of(query);
        if (terms.isEmpty() || lookup == null) {
            return Collections.emptyList();
        }
        Map<Long, Found> found = new HashMap<>();
        for (int term = 0; term < terms.size(); term++) {
            Set<String> seenForms = new HashSet<>();
            for (String form : SearchSynonyms.forms(terms.get(term))) {
                if (!seenForms.add(form)) {
                    continue;
                }
                Pattern wholeWord = wholeWord(form);
                List<Hit> hits = lookup.find(form);
                if (hits == null) {
                    continue;
                }
                for (Hit hit : hits) {
                    if (hit == null || hit.text == null || !wholeWord.matcher(hit.text).find()) {
                        continue;
                    }
                    long key = key(hit.bookNumber, hit.chapter, hit.verse);
                    Found entry = found.get(key);
                    if (entry == null) {
                        entry = new Found(key, hit.text.length());
                        found.put(key, entry);
                    }
                    entry.terms.add(term);
                }
            }
        }

        List<Found> sorted = new ArrayList<>(found.values());
        Collections.sort(sorted, new Comparator<Found>() {
            @Override
            public int compare(Found a, Found b) {
                if (a.terms.size() != b.terms.size()) {
                    return Integer.compare(b.terms.size(), a.terms.size());
                }
                if (a.textLength != b.textLength) {
                    return Integer.compare(a.textLength, b.textLength);
                }
                return Long.compare(a.key, b.key);
            }
        });

        List<Ranked> ranked = new ArrayList<>();
        for (Found entry : sorted) {
            if (ranked.size() >= MAX_RESULTS) {
                break;
            }
            ranked.add(new Ranked(bookOf(entry.key), chapterOf(entry.key), verseOf(entry.key), entry.terms.size()));
        }
        return ranked;
    }

    /**
     * The word as a whole word, with the common English endings (s, es, d, ed, eth, est, th, en, ing) allowed, so
     * "casteth" and "fallen" count, while "outcast" and "drown" do not count for "cast" and "down".
     */
    static Pattern wholeWord(String form) {
        return Pattern.compile("(?<!\\p{L})" + Pattern.quote(form) + "(?:s|es|d|ed|eth|est|th|en|ing)?(?!\\p{L})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static final class Found {
        final long key;
        final int textLength;
        final Set<Integer> terms = new HashSet<>();

        Found(long key, int textLength) {
            this.key = key;
            this.textLength = textLength;
        }
    }

    static int bookOf(long key) {
        return (int) (key / 1_000_000L);
    }

    static int chapterOf(long key) {
        return (int) ((key / 1000L) % 1000L);
    }

    static int verseOf(long key) {
        return (int) (key % 1000L);
    }
}
