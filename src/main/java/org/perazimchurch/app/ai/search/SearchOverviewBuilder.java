package org.perazimchurch.app.ai.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the AI overview for a Bible search from verified sources only.
 *
 * <p>For each content word of the query, two routes find verses: the KJV text contains the word, or an interlinear
 * word whose English gloss is the word (so "weep" finds John 11:35, where the Greek word is glossed "weep").
 * The Hebrew or Greek words behind those glosses give the root notes. Verses are ranked by how many of the query's
 * words they match, then by canonical order, and a verse is cited only when its KJV text is available.
 *
 * <p>There are no per-question routes: the same code runs for every query.
 */
public final class SearchOverviewBuilder {

    /** Verses listed in the overview. The rest of the matches appear in the search results below it. */
    static final int MAX_VERSES = 12;
    /** Root words shown for one English word. */
    static final int MAX_ROOTS_PER_TERM = 2;
    /** Other verses listed for one root word. */
    static final int MAX_ALSO_IN = 3;
    /** Verses examined per ranking tier. Bounds the database reads for very common words. */
    static final int MAX_TIER_CANDIDATES = 200;
    /** Longest Strong's meaning shown, cut at a word boundary. */
    static final int MAX_MEANING_CHARS = 160;

    /** A KJV verse whose text contains a search word. */
    public static final class VerseHit {
        public final int bookNumber;
        public final int chapter;
        public final int verse;
        public final String text;

        public VerseHit(int bookNumber, int chapter, int verse, String text) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
        }
    }

    /** An interlinear word whose English gloss is the search word. */
    public static final class GlossHit {
        public final int bookNumber;
        public final int chapter;
        public final int verse;
        public final int strongsNumber;
        public final String original;
        public final String transliteration;

        public GlossHit(int bookNumber, int chapter, int verse, int strongsNumber, String original,
                        String transliteration) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.verse = verse;
            this.strongsNumber = strongsNumber;
            this.original = original;
            this.transliteration = transliteration;
        }
    }

    /** A Strong's lexicon entry. Null from the lexicon means the number is not in it. */
    public static final class LexiconEntry {
        public final String original;
        public final String transliteration;
        public final String definition;

        public LexiconEntry(String original, String transliteration, String definition) {
            this.original = original;
            this.transliteration = transliteration;
            this.definition = definition;
        }
    }

    /** Everything the builder reads. The Android adapter implements this from the bundled assets. */
    public interface Sources {
        /** KJV verses whose text contains the word, in canonical order. */
        List<VerseHit> keywordVerses(String word);

        /** Interlinear words whose English gloss is the word, in canonical order. */
        List<GlossHit> glossWords(String word);

        /** KJV text of one verse, or null when it is not available. */
        String kjvVerse(int bookNumber, int chapter, int verse);

        /** Strong's entry for a number; {@code hebrew} selects the Hebrew (H) or Greek (G) lexicon. */
        LexiconEntry lexicon(int strongsNumber, boolean hebrew);
    }

    private final Sources sources;

    public SearchOverviewBuilder(Sources sources) {
        this.sources = sources;
    }

    public SearchOverview build(String query) {
        List<String> terms = SearchTerms.of(query);
        if (terms.isEmpty()) {
            return new SearchOverview(terms, Collections.<SearchOverview.RootNote>emptyList(),
                    Collections.<SearchOverview.VerseRef>emptyList(), 0);
        }

        // How many of the query's words each verse matches (a word counts once per verse).
        Map<Long, Integer> termsMatched = new HashMap<>();
        Map<Long, String> kjvText = new HashMap<>();
        List<SearchOverview.RootNote> roots = new ArrayList<>();

        for (String term : terms) {
            Set<Long> keys = new HashSet<>();
            for (VerseHit hit : sources.keywordVerses(term)) {
                long key = key(hit.bookNumber, hit.chapter, hit.verse);
                keys.add(key);
                if (hit.text != null && !hit.text.trim().isEmpty()) {
                    kjvText.putIfAbsent(key, hit.text.trim());
                }
            }

            Map<Integer, Integer> codeCounts = new LinkedHashMap<>();
            Map<Integer, GlossHit> firstWord = new HashMap<>();
            Map<Integer, List<Long>> codeKeys = new HashMap<>();
            for (GlossHit gloss : sources.glossWords(term)) {
                long key = key(gloss.bookNumber, gloss.chapter, gloss.verse);
                keys.add(key);
                Integer count = codeCounts.get(gloss.strongsNumber);
                codeCounts.put(gloss.strongsNumber, count == null ? 1 : count + 1);
                firstWord.putIfAbsent(gloss.strongsNumber, gloss);
                List<Long> list = codeKeys.get(gloss.strongsNumber);
                if (list == null) {
                    list = new ArrayList<>();
                    codeKeys.put(gloss.strongsNumber, list);
                }
                if (!list.contains(key)) {
                    list.add(key);
                }
            }

            for (Long key : keys) {
                termsMatched.merge(key, 1, Integer::sum);
            }
            roots.addAll(rootsFor(term, codeCounts, firstWord, codeKeys));
        }

        // Rank: verses matching more of the query's words come first. Within the same number of words, the
        // shorter verse comes first, since its words sit closer together. Canonical order breaks any remaining tie.
        List<Long> canonical = new ArrayList<>(termsMatched.keySet());
        Collections.sort(canonical);
        List<SearchOverview.VerseRef> cited = new ArrayList<>();
        for (int tier = terms.size(); tier >= 1 && cited.size() < MAX_VERSES; tier--) {
            List<Candidate> found = new ArrayList<>();
            int examined = 0;
            for (Long key : canonical) {
                if (termsMatched.get(key) != tier) {
                    continue;
                }
                if (examined++ == MAX_TIER_CANDIDATES) {
                    break;
                }
                String text = kjvText.get(key);
                if (text == null) {
                    text = sources.kjvVerse(bookOf(key), chapterOf(key), verseOf(key));
                }
                // A verse with no verified KJV text is left out rather than shown without its Scripture.
                if (text == null || text.trim().isEmpty()) {
                    continue;
                }
                found.add(new Candidate(key, text.trim()));
            }
            Collections.sort(found, new Comparator<Candidate>() {
                @Override
                public int compare(Candidate a, Candidate b) {
                    if (a.text.length() != b.text.length()) {
                        return Integer.compare(a.text.length(), b.text.length());
                    }
                    return Long.compare(a.key, b.key);
                }
            });
            for (Candidate c : found) {
                if (cited.size() >= MAX_VERSES) {
                    break;
                }
                cited.add(new SearchOverview.VerseRef(bookOf(c.key), chapterOf(c.key), verseOf(c.key), c.text));
            }
        }

        return new SearchOverview(terms, roots, cited, termsMatched.size());
    }

    /** A verse in one ranking tier, with its KJV text. */
    private static final class Candidate {
        final long key;
        final String text;

        Candidate(long key, String text) {
            this.key = key;
            this.text = text;
        }
    }

    private List<SearchOverview.RootNote> rootsFor(String term, Map<Integer, Integer> codeCounts,
                                                   Map<Integer, GlossHit> firstWord,
                                                   Map<Integer, List<Long>> codeKeys) {
        List<Integer> codes = new ArrayList<>(codeCounts.keySet());
        Collections.sort(codes, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Integer.compare(codeCounts.get(b), codeCounts.get(a));
            }
        });
        List<SearchOverview.RootNote> notes = new ArrayList<>();
        for (Integer number : codes) {
            if (notes.size() >= MAX_ROOTS_PER_TERM) {
                break;
            }
            GlossHit word = firstWord.get(number);
            boolean hebrew = word.bookNumber <= 39;
            LexiconEntry entry = sources.lexicon(number, hebrew);
            String original = word.original;
            String translit = word.transliteration;
            String meaning = "";
            if (entry != null) {
                if (original == null || original.isEmpty()) {
                    original = entry.original;
                }
                if (translit == null || translit.isEmpty()) {
                    translit = entry.transliteration;
                }
                meaning = cleanMeaning(entry.definition);
            }
            List<SearchOverview.VerseRef> alsoIn = new ArrayList<>();
            List<Long> keys = new ArrayList<>(codeKeys.get(number));
            Collections.sort(keys);
            for (Long key : keys) {
                if (alsoIn.size() >= MAX_ALSO_IN) {
                    break;
                }
                alsoIn.add(new SearchOverview.VerseRef(bookOf(key), chapterOf(key), verseOf(key), null));
            }
            String code = (hebrew ? "H" : "G") + number;
            notes.add(new SearchOverview.RootNote(term, code, original, translit, meaning, alsoIn));
        }
        return notes;
    }

    /** Strips the "from 1144;" derivation and tidies the Strong's gloss into a plain phrase. */
    static String cleanMeaning(String definition) {
        if (definition == null) {
            return "";
        }
        String text = definition.trim()
                .replaceFirst("^(of|from)\\s+[^;]*;\\s*", "")
                .replace(":--", ": ")
                .replace("--", ", ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.length() <= MAX_MEANING_CHARS) {
            return text;
        }
        int cut = text.lastIndexOf(' ', MAX_MEANING_CHARS);
        return (cut > 0 ? text.substring(0, cut) : text.substring(0, MAX_MEANING_CHARS)) + "…";
    }

    static long key(int bookNumber, int chapter, int verse) {
        return bookNumber * 1_000_000L + chapter * 1000L + verse;
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
