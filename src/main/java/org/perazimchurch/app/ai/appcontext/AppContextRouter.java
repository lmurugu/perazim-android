package org.perazimchurch.app.ai.appcontext;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Decides whether a question asks about the member's own use of the app (games, Bible notes, sermons, community,
 * routine) and which of those tools should answer it. Pure Java with no Android types, so the rules are tested on the
 * host.
 *
 * <p>A question needs a personal word ("my", or a phrase such as "did I" or "do I miss") before any app topic counts.
 * Generic words such as "verse" or "post" count only with "my", so Bible questions like "what does this verse mean"
 * stay with the Bible and scripture guide. Messages and private chats are detected here and refused.
 */
public final class AppContextRouter {

    /** The tools the guide can use. PRIVATE_BLOCKED is a refusal, never a data source. */
    public enum Topic { GAMES, BIBLE, SERMONS, COMMUNITY, ROUTINE, SUMMARY, PRIVATE_BLOCKED }

    /** A routed question: the topics it touches, in order of relevance, and whether it asks to be taken somewhere. */
    public static final class Route {
        public final List<Topic> topics;
        public final boolean navigate;

        Route(List<Topic> topics, boolean navigate) {
            this.topics = Collections.unmodifiableList(topics);
            this.navigate = navigate;
        }

        public boolean has(Topic topic) {
            return topics.contains(topic);
        }
    }

    /** Words that name the member's own things. "my" is strong: it lets generic words count too. */
    private static final List<String> STRONG_SELF = Arrays.asList("my", "mine", "myself");

    /** Phrases that ask about the member's own activity. They lift only the specific nouns, not generic ones. */
    private static final List<String> WEAK_SELF = Arrays.asList(
            "did i", "have i", "am i", "was i", "where did i", "what did i", "how am i", "what have i",
            "where was i", "do i miss", "did i miss", "do i keep", "i keep missing", "do i get", "did i get",
            "do i fail", "did i fail", "have i read", "did i read");

    private static final List<String> NAVIGATE_PHRASES = Arrays.asList(
            "open", "take me", "show me", "go to", "where is", "where are", "where can i find", "where do i find",
            "bring me", "find my");

    private static final List<String> SUMMARY_PHRASES = Arrays.asList(
            "how am i doing", "my progress", "my activity", "what have i done", "what have i been doing",
            "my summary", "summary of my", "everything i have");

    /** Nouns that name a specific app area. They count with a personal word of either strength. */
    private static final Map<Topic, List<String>> SPECIFIC = new EnumMap<>(Topic.class);
    /** Nouns that are common English. They count only with "my". */
    private static final Map<Topic, List<String>> GENERIC = new EnumMap<>(Topic.class);
    private static final List<String> PRIVATE_NOUNS = Arrays.asList(
            "message", "messages", "messaged", "chat", "chats", "inbox", "dm", "dms", "direct message",
            "direct messages", "private message", "private messages");
    private static final List<String> SEE_PRIVATE_PHRASES = Arrays.asList(
            "can you see", "can you read", "do you see", "do you read", "can the guide see", "can the guide read");

    static {
        SPECIFIC.put(Topic.GAMES, Arrays.asList(
                "game", "games", "scores", "score", "ladder of faith", "word search", "memory match",
                "vine words", "road to jerusalem", "lamp slide", "fill the gap", "old or new", "who said it",
                "verse builder", "story mosaic", "bible quiz", "questions i miss", "questions i missed",
                "questions do i miss", "questions did i miss", "miss most"));
        GENERIC.put(Topic.GAMES, Arrays.asList("wrong", "failing", "fail", "questions", "quiz", "timeline", "stars"));
        SPECIFIC.put(Topic.BIBLE, Arrays.asList(
                "note", "notes", "highlight", "highlights", "highlighted", "bookmark", "bookmarks",
                "favourite verse", "favourite verses", "favorite verse", "favorite verses", "saved verse",
                "saved verses", "bible reader", "last read", "where did i stop", "reading plan",
                "verses i saved", "verses have i saved", "verses i have saved", "verses i bookmarked",
                "verses i highlighted", "verses have i highlighted", "verses have i bookmarked",
                "verses i have bookmarked"));
        GENERIC.put(Topic.BIBLE, Arrays.asList("verse", "verses", "chapter", "chapters", "passage", "reading", "bible"));
        SPECIFIC.put(Topic.SERMONS, Arrays.asList("recent sermons", "saved sermons", "saved sermon", "sermon list"));
        GENERIC.put(Topic.SERMONS, Arrays.asList("sermon", "sermons"));
        SPECIFIC.put(Topic.COMMUNITY, Arrays.asList("community", "prayer wall", "groups i joined",
                "groups have i joined", "groups i have joined"));
        GENERIC.put(Topic.COMMUNITY, Arrays.asList("post", "posts", "event", "events", "group", "groups", "fellowship"));
        SPECIFIC.put(Topic.ROUTINE, Arrays.asList("streak", "streaks", "routine", "routines", "grace points"));
        GENERIC.put(Topic.ROUTINE, Arrays.asList("progress", "points", "xp"));
    }

    private AppContextRouter() {}

    /** Null when the question is not about the member's own app activity. */
    @Nullable
    public static Route route(@Nullable String question) {
        String text = normalise(question);
        if (text.isEmpty()) return null;
        boolean strongSelf = containsAny(text, STRONG_SELF);
        boolean weakSelf = containsAny(text, WEAK_SELF);
        if (!strongSelf && !weakSelf) return null;

        boolean navigate = containsAny(text, NAVIGATE_PHRASES);
        if (containsAny(text, PRIVATE_NOUNS)
                && (strongSelf || containsAny(text, SEE_PRIVATE_PHRASES))) {
            return new Route(Collections.singletonList(Topic.PRIVATE_BLOCKED), navigate);
        }
        if (containsAny(text, SUMMARY_PHRASES)) {
            return new Route(Collections.singletonList(Topic.SUMMARY), navigate);
        }

        List<Topic> matched = new ArrayList<>();
        for (Topic topic : new Topic[] {Topic.GAMES, Topic.BIBLE, Topic.SERMONS, Topic.COMMUNITY, Topic.ROUTINE}) {
            boolean specific = containsAny(text, SPECIFIC.get(topic));
            boolean generic = containsAny(text, possessed(GENERIC.get(topic)));
            if (specific || generic) matched.add(topic);
        }
        if (matched.isEmpty()) return null;
        if (matched.size() > 2) return new Route(Collections.singletonList(Topic.SUMMARY), navigate);
        return new Route(matched, navigate);
    }

    /** Lower case, apostrophes kept, punctuation removed, single spaces, padded so whole words can be matched. */
    static String normalise(@Nullable String question) {
        if (question == null) return "";
        String lower = question.toLowerCase(java.util.Locale.ROOT).replace('’', '\'');
        StringBuilder out = new StringBuilder(" ");
        boolean space = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '\'') {
                out.append(c);
                space = false;
            } else if (!space) {
                out.append(' ');
                space = true;
            }
        }
        if (out.charAt(out.length() - 1) != ' ') out.append(' ');
        return out.toString();
    }

    /**
     * Generic nouns count only when the member's own word comes straight before them: "my posts", "my verses".
     * "my youth group" or "what the Bible says" are not app questions, so they do not match.
     */
    private static List<String> possessed(List<String> nouns) {
        if (nouns == null) return null;
        List<String> out = new ArrayList<>();
        for (String noun : nouns) {
            for (String self : STRONG_SELF) out.add(self + " " + noun);
        }
        return out;
    }

    private static boolean containsAny(String padded, List<String> phrases) {
        if (phrases == null) return false;
        for (String phrase : phrases) {
            if (padded.contains(" " + phrase + " ")) return true;
        }
        return false;
    }
}
