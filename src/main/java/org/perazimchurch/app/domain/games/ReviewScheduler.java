package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Chooses the next Bible question from the player's own results, on the phone only.
 * Each round mixes three kinds of question: unseen ones, ones recently missed, and known ones for review.
 * Difficulty steps up only when the last five answers were mostly right. Nothing is hidden or forced.
 * Pure domain logic with no Android types, so the rules are covered by unit tests.
 */
public final class ReviewScheduler {
    /** Share of picks taken from unseen questions, in percent. */
    public static final int UNSEEN_PERCENT = 50;
    /** Share of picks taken from recently missed questions, in percent. The rest is review of known ones. */
    public static final int MISSED_PERCENT = 30;
    /** How many recent answers decide whether difficulty steps up. */
    public static final int STEP_UP_WINDOW = 5;
    /** Correct answers needed in the window to step up. */
    public static final int STEP_UP_CORRECT = 4;

    /** Per-question record kept on this phone. */
    public static final class Record {
        public final int correct;
        public final int missed;
        public final long lastSeenDay;

        public Record(int correct, int missed, long lastSeenDay) {
            this.correct = correct;
            this.missed = missed;
            this.lastSeenDay = lastSeenDay;
        }
    }

    private ReviewScheduler() { }

    /**
     * Picks one question id from {@code candidates}. {@code history} maps ids to their record;
     * a missing id counts as unseen. Returns null when there are no candidates.
     */
    public static String pick(List<String> candidates, Map<String, Record> history, Random random) {
        return pick(candidates, history, random, UNSEEN_PERCENT, MISSED_PERCENT);
    }

    /**
     * Same as {@link #pick(List, Map, Random)} with the question shares from the cached game settings.
     * Shares outside 0 to 100 or adding past 100 fall back to the built-in values.
     */
    public static String pick(List<String> candidates, Map<String, Record> history, Random random,
                              int unseenPercent, int missedPercent) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (unseenPercent < 0 || missedPercent < 0 || unseenPercent + missedPercent > 100) {
            unseenPercent = UNSEEN_PERCENT;
            missedPercent = MISSED_PERCENT;
        }
        List<String> unseen = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        List<String> known = new ArrayList<>();
        for (String id : candidates) {
            Record record = history.get(id);
            if (record == null) unseen.add(id);
            else if (record.missed > record.correct) missed.add(id);
            else known.add(id);
        }
        int roll = random.nextInt(100);
        List<String> chosen = roll < unseenPercent ? unseen : roll < unseenPercent + missedPercent ? missed : known;
        if (!chosen.isEmpty()) return chosen.get(random.nextInt(chosen.size()));
        // The chosen group is empty: pick evenly from the groups that have questions.
        List<List<String>> filled = new ArrayList<>();
        for (List<String> group : Arrays.asList(unseen, missed, known)) if (!group.isEmpty()) filled.add(group);
        List<String> group = filled.get(random.nextInt(filled.size()));
        return group.get(random.nextInt(group.size()));
    }

    /** True when the last answers (newest last) show the player is ready for harder questions. */
    public static boolean shouldStepUp(List<Boolean> recentAnswers) {
        if (recentAnswers == null || recentAnswers.size() < STEP_UP_WINDOW) return false;
        List<Boolean> window = recentAnswers.subList(recentAnswers.size() - STEP_UP_WINDOW, recentAnswers.size());
        int correct = 0;
        for (Boolean answer : window) if (Boolean.TRUE.equals(answer)) correct++;
        return correct >= STEP_UP_CORRECT;
    }
}
