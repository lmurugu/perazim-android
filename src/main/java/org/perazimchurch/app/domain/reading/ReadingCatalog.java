package org.perazimchurch.app.domain.reading;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

/** Bundled KJV passages and authored prompts. No model or network is required. */
public final class ReadingCatalog {
    public static final int MAX_DAYS = 30;
    private static final Set<String> STOP = tokens("a an and about for from i in is me my of on please the to what when with bible god help need how");

    public static final class Reading {
        public final String id, reference, scripture, focus, reflection;
        private final String address;
        public final String book;
        public final int chapter, firstVerse;

        private Reading(String topic, JSONObject data) throws JSONException {
            address = data.getString("id");
            id = topic + ":" + address;
            reference = data.getString("ref");
            scripture = data.getString("passage");
            focus = data.getString("focus");
            reflection = data.getString("reflection");
            String[] parts = address.split("\\.", 3);
            book = parts[0];
            chapter = Integer.parseInt(parts[1]);
            firstVerse = Integer.parseInt(parts[2].split(",", 2)[0]);
        }

        public static Reading fromResearch(String id, String reference, String text, String topic)
                throws JSONException {
            return new Reading("research", new JSONObject()
                    .put("id", id).put("ref", reference).put("passage", text)
                    .put("focus", topic)
                    .put("reflection", "Read " + reference + " in its context. What does the passage actually say? "
                            + "Write one connection you see with your topic and one question you still have."));
        }

        public JSONObject toResearchJson() throws JSONException {
            return new JSONObject().put("id", address).put("ref", reference).put("passage", scripture);
        }

        public static Reading restoreResearch(JSONObject data, String topic) throws JSONException {
            return fromResearch(data.getString("id"), data.getString("ref"), data.getString("passage"), topic);
        }
    }

    public static final class Topic {
        public final String id, title;
        public final List<Reading> readings;
        private final Set<String> terms;

        private Topic(JSONObject data) throws JSONException {
            id = data.getString("id");
            title = data.getString("title");
            terms = tokens(title + " " + data.getString("keywords"));
            List<Reading> entries = new ArrayList<>();
            JSONArray list = data.getJSONArray("readings");
            for (int i = 0; i < list.length(); i++) entries.add(new Reading(id, list.getJSONObject(i)));
            readings = Collections.unmodifiableList(entries);
        }
    }

    public final List<Topic> topics;
    private final Map<String, Reading> byId = new LinkedHashMap<>();
    private final List<Reading> daily;

    public ReadingCatalog(Reader source) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(source);
        StringBuilder json = new StringBuilder();
        for (String line; (line = reader.readLine()) != null;) json.append(line);
        JSONArray list = new JSONArray(json.toString());
        List<Topic> loaded = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            Topic topic = new Topic(list.getJSONObject(i));
            loaded.add(topic);
            for (Reading reading : topic.readings) byId.putIfAbsent(reading.id, reading);
        }
        topics = Collections.unmodifiableList(loaded);
        Map<String, Reading> distinct = new LinkedHashMap<>();
        for (Reading reading : byId.values()) distinct.putIfAbsent(reading.address, reading);
        daily = new ArrayList<>(distinct.values());
        Collections.shuffle(daily, new Random(502020L));
    }

    public Reading find(String id) { return byId.get(id); }

    public Reading dailyReading(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            long day = format.parse(date).getTime() / 86_400_000L;
            return daily.get((int) Math.floorMod(day, (long) daily.size()));
        } catch (ParseException e) {
            return daily.get(0);
        }
    }

    /** Return only topics supported by words in the catalog. */
    public List<Topic> matches(String query) {
        Set<String> words = tokens(query);
        words.removeAll(STOP);
        Map<Topic, Integer> scores = new LinkedHashMap<>();
        for (Topic topic : topics) {
            int score = 4 * overlap(words, topic.terms);
            int passageScore = 0;
            for (Reading reading : topic.readings) {
                passageScore = Math.max(passageScore, overlap(words, tokens(reading.focus + " " + reading.scripture)));
            }
            score += passageScore;
            if (score > 0) scores.put(topic, score);
        }
        List<Topic> results = new ArrayList<>(scores.keySet());
        results.sort(Comparator.comparingInt((Topic topic) -> scores.get(topic)).reversed());
        return results;
    }

    /** Prefer readings that match the user's wording, keeping the chosen theme as a fallback. */
    public List<String> select(Topic topic, String query, int days) {
        if (days < 1 || days > MAX_DAYS) throw new IllegalArgumentException("Choose 1 to 30 days");
        List<Reading> order = new ArrayList<>(topic.readings);
        Set<String> words = tokens(query);
        words.removeAll(STOP);
        if (!words.isEmpty()) order.sort(Comparator.comparingInt((Reading item) ->
                2 * overlap(words, tokens(item.focus)) + overlap(words, tokens(item.scripture))).reversed());
        List<String> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) result.add(order.get(i).id);
        return result;
    }

    private static int overlap(Set<String> a, Set<String> b) {
        int matched = 0;
        for (String word : a) if (b.contains(word)) matched++;
        return matched;
    }

    private static Set<String> tokens(String value) {
        Set<String> result = new HashSet<>();
        for (String word : value.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (!word.isEmpty()) result.add(word);
        }
        return result;
    }
}
