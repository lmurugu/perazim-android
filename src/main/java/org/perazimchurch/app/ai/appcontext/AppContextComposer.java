package org.perazimchurch.app.ai.appcontext;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.appcontext.AppContextRouter.Route;
import org.perazimchurch.app.ai.appcontext.AppContextRouter.Topic;

/**
 * Writes the guide's answer about the member's own app use from compact facts. Plain sentences only: the model is not
 * asked to invent or summarise personal data, so every number and name comes from the snapshot. Pure Java.
 */
public final class AppContextComposer {

    static final int MAX_CHARS = 900;
    static final String FOOTER = "Read from this phone only. Nothing is sent.";
    private static final String OFF_TEXT = "App activity is turned off, so I cannot see your games, notes or routine. "
            + "Turn it on under Choose your guide.";
    private static final String PRIVATE_TEXT = "I cannot see your personal messages or private chats, and I never read them. "
            + "I can help with your games, Bible notes and highlights, sermons, public community posts and routine.";

    /** What the dialog shows: text, an optional "Open ..." action, and follow-up suggestions. */
    public static final class Reply {
        public final String text;
        public final String actionIntent;
        public final String actionLabel;
        public final String actionPayload;
        public final List<String> chips;

        Reply(String text, String actionIntent, String actionLabel, String actionPayload, List<String> chips) {
            this.text = text;
            this.actionIntent = actionIntent;
            this.actionLabel = actionLabel;
            this.actionPayload = actionPayload;
            this.chips = Collections.unmodifiableList(chips);
        }

        public boolean hasAction() {
            return actionIntent != null && actionLabel != null;
        }
    }

    /** One topic's contribution: its lines and the single action that best matches it. */
    private static final class Part {
        final List<String> lines = new ArrayList<>();
        String intent;
        String label;
        String payload;
        final List<String> chips = new ArrayList<>();
    }

    private AppContextComposer() {}

    public static Reply disabled() {
        return new Reply(OFF_TEXT, AiResponse.INTENT_NONE, null, null, Collections.<String>emptyList());
    }

    public static Reply privateRefusal() {
        return new Reply(PRIVATE_TEXT, AiResponse.INTENT_NONE, null, null,
                java.util.Arrays.asList("What is my streak?", "Show my Bible notes"));
    }

    /** Answers a routed question. {@code question} lets the Bible part prefer notes when the member asks about them. */
    public static Reply compose(Route route, AppSnapshot s, @Nullable String question) {
        if (route.has(Topic.PRIVATE_BLOCKED)) return privateRefusal();
        String asked = question == null ? "" : AppContextRouter.normalise(question);
        List<Part> parts = new ArrayList<>();
        if (route.has(Topic.SUMMARY)) {
            Part summary = new Part();
            for (Topic topic : new Topic[] {Topic.GAMES, Topic.BIBLE, Topic.ROUTINE, Topic.COMMUNITY, Topic.SERMONS}) {
                Part p = part(topic, s, "");
                if (!p.lines.isEmpty()) summary.lines.add(p.lines.get(0));
            }
            if (summary.lines.isEmpty()) summary.lines.add("There is no activity on this phone yet. Open Home to start.");
            summary.intent = AiResponse.INTENT_NONE;
            summary.chips.add("What are my game stars?");
            summary.chips.add("Show my Bible notes");
            parts.add(summary);
        } else {
            for (Topic topic : route.topics) parts.add(part(topic, s, asked));
        }

        Part first = parts.get(0);
        List<String> lines = new ArrayList<>();
        List<String> chips = new ArrayList<>();
        for (Part p : parts) {
            lines.addAll(p.lines);
            for (String chip : p.chips) if (!chips.contains(chip) && chips.size() < 3) chips.add(chip);
        }
        String text = join(lines);
        if (first.intent != null && first.intent.equals(AiResponse.INTENT_NONE)) {
            return new Reply(text + "\n" + FOOTER, AiResponse.INTENT_NONE, null, null, chips);
        }
        return new Reply(text + "\n" + FOOTER, first.intent, first.label, first.payload, chips);
    }

    /** True when the normalised question names this word or a form of it, such as "note" for "notes". */
    static boolean asks(String asked, String word) {
        return asked.contains(" " + word);
    }

    private static Part part(Topic topic, AppSnapshot s, String asked) {
        switch (topic) {
            case GAMES: return games(s);
            case BIBLE: return bible(s, asked);
            case SERMONS: return sermons(s);
            case COMMUNITY: return community(s, asked);
            case ROUTINE: return routine(s);
            default: return new Part();
        }
    }

    private static Part games(AppSnapshot s) {
        Part p = new Part();
        p.intent = AiResponse.INTENT_OPEN_GAMES;
        p.label = "🎮 Open Games";
        p.chips.add("Which questions do I miss most?");
        List<AppSnapshot.Game> played = new ArrayList<>();
        for (AppSnapshot.Game g : s.games) if (g.bestStars > 0 || g.unlockedLevel > 1) played.add(g);
        if (played.isEmpty() && s.missedQuestions.isEmpty()) {
            p.lines.add("You have not played any games on this phone yet. Open Games from Home to start with Bible Quiz.");
            return p;
        }
        if (!played.isEmpty()) {
            StringBuilder line = new StringBuilder("Your games: ");
            for (int i = 0; i < played.size() && i < 3; i++) {
                AppSnapshot.Game g = played.get(i);
                if (i > 0) line.append("; ");
                line.append(g.title).append(" ").append(g.bestStars).append(" of 3 stars");
                if (g.unlockedLevel > 1) line.append(", level ").append(g.unlockedLevel).append(" open");
            }
            p.lines.add(line.append('.').toString());
        }
        if (!s.missedQuestions.isEmpty()) {
            StringBuilder line = new StringBuilder("Questions you miss most: ");
            for (int i = 0; i < s.missedQuestions.size() && i < 2; i++) {
                AppSnapshot.MissedQuestion q = s.missedQuestions.get(i);
                if (i > 0) line.append("; ");
                line.append('"').append(clip(q.prompt, 90)).append("\" (missed ").append(q.missed)
                        .append(", right ").append(q.correct).append(')');
            }
            p.lines.add(line.append('.').toString());
            p.lines.add("Missed questions come back more often, so replaying those games is the quickest way to improve.");
        }
        return p;
    }

    private static Part bible(AppSnapshot s, String asked) {
        Part p = new Part();
        p.intent = AiResponse.INTENT_OPEN_BIBLE;
        p.label = "📖 Open Holy Bible Reader";
        p.chips.add("Show my Bible notes");
        p.chips.add("What verses have I bookmarked?");
        boolean reading = asks(asked, "read") || asks(asked, "stop") || asks(asked, "last");
        boolean notes = asks(asked, "note");
        boolean saved = asks(asked, "save") || asks(asked, "verse") || asks(asked, "bookmark")
                || asks(asked, "favourite") || asks(asked, "favorite") || asks(asked, "highlight");
        boolean all = !reading && !notes && !saved;
        if (all || notes) {
            if (s.noteCount == 0) {
                p.lines.add("You have no Bible notes yet.");
            } else {
                p.lines.add("You have " + s.noteCount + (s.noteCount == 1 ? " Bible note" : " Bible notes")
                        + (s.latestNotePreview.isEmpty() ? "." : ". Latest: \"" + clip(s.latestNotePreview, 80) + "\""));
            }
        }
        if (all || notes || saved) {
            if (s.highlightCount > 0) p.lines.add("Highlighted verses: " + s.highlightCount + ".");
        }
        if (all || saved) {
            if (s.favouriteCount > 0) {
                StringBuilder line = new StringBuilder("Saved verses: ");
                for (int i = 0; i < s.favouriteVerses.size() && i < 3; i++) {
                    if (i > 0) line.append(", ");
                    line.append(s.favouriteVerses.get(i).label());
                }
                if (s.favouriteCount > 3) line.append(" and ").append(s.favouriteCount - 3).append(" more");
                p.lines.add(line.append('.').toString());
            } else if (saved && !all) {
                p.lines.add("You have not saved any verses yet.");
            }
        }
        if (all || reading) {
            if (s.lastReading != null) {
                p.lines.add("You last read " + s.lastReading.label() + ". Chapters read today: " + s.chaptersToday + ".");
            }
        }
        AppSnapshot.Passage target = null;
        if (notes && s.latestNotePassage != null) target = s.latestNotePassage;
        else if (reading && s.lastReading != null) target = s.lastReading;
        else if (saved && !s.favouriteVerses.isEmpty()) target = s.favouriteVerses.get(0);
        else if (s.lastReading != null) target = s.lastReading;
        else if (s.latestNotePassage != null) target = s.latestNotePassage;
        else if (!s.favouriteVerses.isEmpty()) target = s.favouriteVerses.get(0);
        if (target != null) {
            p.label = "📖 Open " + target.label();
            p.payload = target.payload();
        }
        return p;
    }

    private static Part sermons(AppSnapshot s) {
        Part p = new Part();
        p.intent = AiResponse.INTENT_NAVIGATE_SERMONS;
        p.label = "🎙️ Open Sermons";
        p.chips.add("Show my recent sermons");
        if (!s.recentSermons.isEmpty()) {
            p.lines.add("Recent sermons: " + join(s.recentSermons, 3) + ".");
        } else {
            p.lines.add("No sermons are loaded on this phone yet.");
        }
        if (s.savedSermonCount > 0) p.lines.add("Saved for offline listening: " + s.savedSermonCount + ".");
        return p;
    }

    private static Part community(AppSnapshot s, String asked) {
        Part p = new Part();
        p.intent = AiResponse.INTENT_NAVIGATE_FELLOWSHIP;
        p.label = "🙏 Open Community";
        p.chips.add("What groups have I joined?");
        boolean groups = asks(asked, "group");
        boolean events = asks(asked, "event");
        boolean posts = asks(asked, "post");
        boolean all = !groups && !events && !posts;
        if (all || posts) {
            if (s.myPostCount > 0) {
                p.lines.add("You have shared " + s.myPostCount + (s.myPostCount == 1 ? " post." : " posts."));
            } else {
                p.lines.add("You have not shared a community post yet.");
            }
            if (!s.recentPublicPosts.isEmpty()) p.lines.add("Latest public posts: " + join(s.recentPublicPosts, 2) + ".");
        }
        if (all || groups) {
            if (!s.joinedGroups.isEmpty()) {
                p.lines.add("Groups you have joined: " + join(s.joinedGroups, 5) + ".");
            } else if (groups) {
                p.lines.add("You have not joined a group yet.");
            }
        }
        if (all || events) {
            if (!s.upcomingEvents.isEmpty()) p.lines.add("Coming up: " + join(s.upcomingEvents, 2) + ".");
            else if (events) p.lines.add("No upcoming events are loaded on this phone.");
        }
        return p;
    }

    private static Part routine(AppSnapshot s) {
        Part p = new Part();
        p.intent = AiResponse.INTENT_OPEN_PROFILE;
        p.label = "👤 Open Profile";
        p.chips.add("What is my streak?");
        if (s.streak > 0) {
            p.lines.add("Your streak is " + s.streak + (s.streak == 1 ? " day." : " days."));
        } else {
            p.lines.add("You do not have a streak yet.");
        }
        p.lines.add(s.routineDoneToday
                ? "Today's routine is done."
                : "Today's routine is not finished yet: Read, Reflect and Pray.");
        if (s.lifetimeRoutineDays > 0) p.lines.add("Routine days so far: " + s.lifetimeRoutineDays + ".");
        return p;
    }

    static String join(List<String> lines) {
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
        String text = out.toString();
        if (text.length() <= MAX_CHARS) return text;
        int cut = text.lastIndexOf(". ", MAX_CHARS);
        return cut > 0 ? text.substring(0, cut + 1) : text.substring(0, MAX_CHARS);
    }

    private static String join(List<String> items, int max) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < items.size() && i < max; i++) {
            if (i > 0) out.append("; ");
            out.append(clip(items.get(i), 60));
        }
        return out.toString();
    }

    static String clip(String text, int max) {
        String t = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, Math.max(0, max - 3)).trim() + "...";
    }
}
