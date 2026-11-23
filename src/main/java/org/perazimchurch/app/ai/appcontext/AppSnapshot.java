package org.perazimchurch.app.ai.appcontext;

import java.util.ArrayList;
import java.util.List;

/**
 * The few compact facts about the member's own app use that one answer needs. Built on this phone by
 * {@link AppContextReader} and read by {@link AppContextComposer}. It holds no raw rows and no private messages.
 */
public final class AppSnapshot {

    /** A Bible passage the member can open: book name, chapter and, when known, a verse. */
    public static final class Passage {
        public final String bookId;
        public final String bookName;
        public final int chapter;
        public final int verse;

        public Passage(String bookId, String bookName, int chapter, int verse) {
            this.bookId = bookId;
            this.bookName = bookName;
            this.chapter = chapter;
            this.verse = verse;
        }

        public String label() {
            return verse > 0 ? bookName + " " + chapter + ":" + verse : bookName + " " + chapter;
        }

        /** Payload for the guide's open-passage action: "bookId|chapter". */
        public String payload() {
            return bookId + "|" + chapter;
        }
    }

    /** One game the member has played on this phone. */
    public static final class Game {
        public final String title;
        public final int bestStars;
        public final int unlockedLevel;
        public final int levelCount;

        public Game(String title, int bestStars, int unlockedLevel, int levelCount) {
            this.title = title;
            this.bestStars = bestStars;
            this.unlockedLevel = unlockedLevel;
            this.levelCount = levelCount;
        }
    }

    /** A Bible question the member has answered wrong more often than right, kept on this phone. */
    public static final class MissedQuestion {
        public final String game;
        public final String prompt;
        public final int missed;
        public final int correct;

        public MissedQuestion(String game, String prompt, int missed, int correct) {
            this.game = game;
            this.prompt = prompt;
            this.missed = missed;
            this.correct = correct;
        }
    }

    public final List<Game> games = new ArrayList<>();
    public final List<MissedQuestion> missedQuestions = new ArrayList<>();

    public int noteCount;
    public String latestNotePreview = "";
    public Passage latestNotePassage;
    public int highlightCount;
    public int favouriteCount;
    public final List<Passage> favouriteVerses = new ArrayList<>();
    public Passage lastReading;
    public int chaptersToday;

    public final List<String> recentSermons = new ArrayList<>();
    public int savedSermonCount;

    public int myPostCount;
    public final List<String> recentPublicPosts = new ArrayList<>();
    public final List<String> joinedGroups = new ArrayList<>();
    public final List<String> upcomingEvents = new ArrayList<>();

    public int streak;
    public int lifetimeRoutineDays;
    public boolean routineDoneToday;
}
