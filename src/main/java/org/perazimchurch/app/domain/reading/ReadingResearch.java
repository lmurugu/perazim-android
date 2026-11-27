package org.perazimchurch.app.domain.reading;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Ranks verbatim passages from the bundled KJV. The reader's topic is kept in their own words. */
public final class ReadingResearch {
    private static final java.util.regex.Pattern SEPARATORS = java.util.regex.Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "a", "an", "and", "about", "at", "for", "from", "how", "i", "in", "is", "it", "me", "my",
            "of", "on", "or", "the", "to", "what", "with", "your", "bible", "please", "help", "says"));
    private static final Map<String, List<String>> TERMS = new HashMap<>();
    static {
        TERMS.put("loneliness", Arrays.asList("alone", "solitary", "forsaken"));
        TERMS.put("lonely", Arrays.asList("alone", "solitary"));
        TERMS.put("grief", Arrays.asList("grief", "sorrow", "mourning", "mourn"));
        TERMS.put("burnout", Arrays.asList("weary", "faint", "rest"));
        TERMS.put("jealousy", Arrays.asList("jealousy", "envy", "envying"));
    }

    private ReadingResearch() { }

    private static final class Candidate {
        final ReadingCatalog.Reading reading;
        final int score, position;
        Candidate(ReadingCatalog.Reading reading, int score, int position) {
            this.reading = reading;
            this.score = score;
            this.position = position;
        }
    }

    public static List<ReadingCatalog.Reading> search(String topic, Reader corpus, JSONArray manifest)
            throws IOException, JSONException {
        String phrase = normalize(topic);
        Set<String> keywords = words(phrase);
        keywords.removeAll(STOP);
        if (keywords.isEmpty()) return java.util.Collections.emptyList();

        Map<Integer, JSONObject> books = new HashMap<>();
        for (int i = 0; i < manifest.length(); i++) {
            JSONObject book = manifest.getJSONObject(i);
            books.put(book.getInt("bookNumber"), book);
        }
        Comparator<Candidate> rank = Comparator.comparingInt((Candidate c) -> c.score).reversed()
                .thenComparingInt(c -> c.position);
        PriorityQueue<Candidate> nearest = new PriorityQueue<>(ReadingCatalog.MAX_DAYS, rank.reversed());
        BufferedReader reader = new BufferedReader(corpus);
        int position = 0;
        for (String line; (line = reader.readLine()) != null;) {
            position++;
            String[] parts = line.split("\t", 4);
            if (parts.length != 4) continue;
            String verse = normalize(parts[3]);
            int score = score(phrase, keywords, verse);
            if (score == 0 || (nearest.size() == ReadingCatalog.MAX_DAYS && nearest.peek().score >= score)) continue;
            JSONObject book = books.get(Integer.parseInt(parts[0]));
            if (book == null) continue;
            String id = book.getString("id") + "." + parts[1] + "." + parts[2];
            String reference = book.getString("name") + " " + parts[1] + ":" + parts[2];
            if (nearest.size() == ReadingCatalog.MAX_DAYS) nearest.remove();
            nearest.add(new Candidate(ReadingCatalog.Reading.fromResearch(id, reference, parts[3], topic), score, position));
        }
        List<Candidate> ranked = new ArrayList<>(nearest);
        ranked.sort(rank);
        List<ReadingCatalog.Reading> result = new ArrayList<>();
        for (Candidate candidate : ranked) result.add(candidate.reading);
        return result;
    }

    private static int score(String phrase, Set<String> keywords, String passage) {
        if ((" " + passage + " ").contains(" " + phrase + " ")) return 50 + keywords.size();
        int matched = 0, strength = 0;
        Set<String> words = words(passage);
        for (String term : keywords) {
            if (words.contains(term)) {
                matched++;
                strength += 4;
            } else {
                List<String> equivalents = TERMS.get(term);
                if (equivalents != null && equivalents.stream().anyMatch(words::contains)) {
                    matched++;
                    strength++;
                }
            }
        }
        return matched >= Math.min(2, keywords.size()) ? strength : 0;
    }

    private static String normalize(String value) {
        return SEPARATORS.matcher(value.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    private static Set<String> words(String normalized) {
        Set<String> items = new HashSet<>(Arrays.asList(normalized.split(" ")));
        items.remove("");
        return items;
    }
}
