package org.perazimchurch.app.ai.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a question or phrase typed into Bible search into the content words that matter, such as
 * "where in the bible does Jesus weep" becoming [jesus, weep]. Generic English function words only: there are
 * no per-question routes here.
 */
public final class SearchTerms {

    /** Most content words used for one search. Longer questions keep their first words. */
    static final int MAX_TERMS = 6;

    private static final Set<String> FUNCTION_WORDS = new HashSet<>(Arrays.asList(
            "about", "above", "after", "again", "all", "also", "and", "any", "are", "as", "at", "be", "been",
            "being", "but", "by", "can", "could", "did", "do", "does", "for", "from", "give", "had", "has", "have",
            "how", "into", "its", "just", "may", "me", "more", "must", "my", "not", "now", "of", "on", "or", "our",
            "out", "over", "passage", "passages", "say", "says", "scripture", "scriptures", "search", "show", "so",
            "some", "speak", "speaks", "tell", "than", "that", "the", "their", "them", "then", "there", "these",
            "they", "this", "those", "through", "to", "too", "under", "until", "up", "upon", "us", "very", "was",
            "were", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "within",
            "would", "you", "your", "bible", "verse", "verses", "find", "look", "where's", "whats", "whose", "shall",
            "thee", "thou", "thy", "yea", "unto", "wherein", "therein", "whereof", "written", "wrote", "write", "mention",
            "mentioned", "happened", "happen", "happens", "did", "does", "doing", "said", "telling", "concerning",
            "regarding", "actually", "exactly", "quote", "quoted", "phrase", "book", "chapter", "talk", "talks", "talked",
            "talking"));

    private SearchTerms() {
    }

    /** Content words of the query in the order typed, lower case, without duplicates. */
    public static List<String> of(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleaned = query.toLowerCase(Locale.ROOT).replace('’', '\'').replace("'s", "");
        List<String> terms = new ArrayList<>();
        for (String raw : cleaned.split("[^\\p{L}]+")) {
            if (raw.length() < 3 || FUNCTION_WORDS.contains(raw) || terms.contains(raw)) {
                continue;
            }
            terms.add(raw);
            if (terms.size() == MAX_TERMS) {
                break;
            }
        }
        return terms;
    }
}
