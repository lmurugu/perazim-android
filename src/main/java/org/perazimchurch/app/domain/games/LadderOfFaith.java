package org.perazimchurch.app.domain.games;

import org.perazimchurch.app.domain.games.BibleGames.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One run of Ladder of Faith: 20 rungs in climbing order, three lives, and one use each of three lifelines.
 * Each run draws its questions at random from a large bank: rungs 1-7 from the easy tier, 8-14 from the medium
 * tier and 15-20 from the hard tier, so runs differ but always get harder as you climb.
 * A wrong answer costs a life; the run ends when lives run out or the top rung is answered.
 * Pure domain logic with no Android types, so the rules are covered by unit tests.
 */
public final class LadderOfFaith {
    public static final int RUNGS = 20;
    public static final int LIVES = 3;
    /** Rungs that trigger a milestone celebration when answered correctly. */
    public static final int[] MILESTONES = {5, 10, 15, 20};
    /** Rungs 1-7 are easy, 8-14 medium, 15-20 hard. */
    public static final int EASY_RUNGS = 7;
    public static final int MEDIUM_RUNGS = 7;

    /** Things the UI may celebrate or announce. Sound and haptics are chosen by the feedback layer. */
    public enum Event { CORRECT, WRONG, MILESTONE, LIFE_LOST, LIFELINE_USED, RUN_COMPLETE }

    private final List<Card> rungs;
    private final int easyCount;
    private final int mediumCount;
    private final List<Event> events = new ArrayList<>();
    private int index;
    private int score;
    private int lives = LIVES;
    private boolean answered;
    private boolean correct;
    private boolean milestone;
    private boolean fiftyUsed;
    private boolean peekUsed;
    private boolean passUsed;
    private List<String> hidden = Collections.emptyList();
    private boolean peeked;
    /** Set once the top rung has been answered, right or wrong; the run is then over. */
    private boolean topAnswered;

    public LadderOfFaith() { this(System.nanoTime()); }

    /** The same seed always produces the same run, which keeps tests and replays predictable. */
    public LadderOfFaith(long seed) {
        this(seed, EASY_RUNGS, MEDIUM_RUNGS);
    }

    /**
     * A run with a chosen difficulty mix: {@code easy} easy rungs, {@code medium} medium rungs and the
     * rest hard, always {@link #RUNGS} in total. Levels use this to get harder as the player goes on.
     */
    public LadderOfFaith(long seed, int easy, int medium) {
        validateMix(easy, medium);
        easyCount = easy;
        mediumCount = medium;
        Random random = new Random(seed);
        List<Card> draw = new ArrayList<>();
        draw.addAll(pick(LadderBank.EASY, easy, random));
        draw.addAll(pick(LadderBank.MEDIUM, medium, random));
        draw.addAll(pick(LadderBank.HARD, RUNGS - easy - medium, random));
        rungs = Collections.unmodifiableList(draw);
    }

    /**
     * Same tiers as {@link #LadderOfFaith(long)}, but each rung is chosen by {@link ReviewScheduler} from the
     * player's own history on this phone: unseen questions, recently missed ones, and known ones for review.
     * {@code history} is keyed by question prompt; a missing entry counts as unseen. Null history means none.
     */
    public LadderOfFaith(long seed, Map<String, ReviewScheduler.Record> history, int unseenPercent, int missedPercent) {
        this(seed, history, unseenPercent, missedPercent, EASY_RUNGS, MEDIUM_RUNGS);
    }

    public LadderOfFaith(long seed, Map<String, ReviewScheduler.Record> history, int unseenPercent,
                         int missedPercent, int easy, int medium) {
        validateMix(easy, medium);
        easyCount = easy;
        mediumCount = medium;
        Random random = new Random(seed);
        Map<String, ReviewScheduler.Record> record = history == null ? Collections.emptyMap() : history;
        List<Card> draw = new ArrayList<>();
        draw.addAll(pickScheduled(LadderBank.EASY, easy, random, record, unseenPercent, missedPercent));
        draw.addAll(pickScheduled(LadderBank.MEDIUM, medium, random, record, unseenPercent, missedPercent));
        draw.addAll(pickScheduled(LadderBank.HARD, RUNGS - easy - medium, random, record,
                unseenPercent, missedPercent));
        rungs = Collections.unmodifiableList(draw);
    }

    private static void validateMix(int easy, int medium) {
        if (easy < 0 || medium < 0 || easy + medium > RUNGS) {
            throw new IllegalArgumentException("difficulty mix does not fit the ladder");
        }
    }

    public int currentTier() {
        return position() <= easyCount ? 1 : position() <= easyCount + mediumCount ? 2 : 3;
    }

    public boolean reachedTop() { return topAnswered || index >= RUNGS; }

    /** How many rungs of this run come from the given tier (1 easy, 2 medium, 3 hard). */
    public int rungsOfTier(int tier) {
        List<Card> pool = tier == 1 ? LadderBank.EASY : tier == 2 ? LadderBank.MEDIUM : LadderBank.HARD;
        int count = 0;
        for (Card card : rungs) if (pool.contains(card)) count++;
        return count;
    }

    private static List<Card> pick(List<Card> tier, int count, Random random) {
        List<Card> shuffled = new ArrayList<>(tier);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }

    private static List<Card> pickScheduled(List<Card> tier, int count, Random random,
                                            Map<String, ReviewScheduler.Record> history,
                                            int unseenPercent, int missedPercent) {
        Map<String, Card> left = new LinkedHashMap<>();
        for (Card card : tier) left.putIfAbsent(card.prompt, card);
        List<Card> chosen = new ArrayList<>();
        while (chosen.size() < count && !left.isEmpty()) {
            String id = ReviewScheduler.pick(new ArrayList<>(left.keySet()), history, random,
                    unseenPercent, missedPercent);
            chosen.add(left.remove(id));
        }
        return chosen;
    }

    /** Difficulty tier of a 1-based rung: 1 easy, 2 medium, 3 hard. */
    public static int tierOf(int position) {
        if (position <= EASY_RUNGS) return 1;
        return position <= EASY_RUNGS + MEDIUM_RUNGS ? 2 : 3;
    }

    public Card current() {
        if (finished()) throw new IllegalStateException("Run finished");
        return rungs.get(index);
    }

    /** 1-based rung number of the current question. */
    public int position() { return index + 1; }
    public int score() { return score; }
    public int lives() { return lives; }
    public boolean answered() { return answered; }
    public boolean correct() { return correct; }
    /** True only for the answer just given, if it was a milestone rung answered correctly. */
    public boolean milestone() { return milestone; }
    /** Options removed by 50/50 for the current question; empty when unused. */
    public List<String> hiddenOptions() { return Collections.unmodifiableList(hidden); }
    /** True once Verse Peek has been used on the current question. */
    public boolean peeked() { return peeked; }
    public boolean fiftyAvailable() { return !fiftyUsed && !answered && !finished(); }
    public boolean peekAvailable() { return !peekUsed && !answered && !finished(); }
    public boolean passAvailable() { return !passUsed && !answered && !finished(); }

    public boolean finished() { return lives <= 0 || topAnswered || index >= RUNGS; }

    public static boolean isMilestone(int position) {
        for (int m : MILESTONES) if (m == position) return true;
        return false;
    }

    /** Hides two wrong options. Only before answering, and only once per run. */
    public boolean useFiftyFifty() {
        if (!fiftyAvailable()) return false;
        List<String> wrong = new ArrayList<>();
        for (String option : current().options) if (!option.equals(current().answer)) wrong.add(option);
        hidden = new ArrayList<>(wrong.subList(0, Math.min(2, wrong.size())));
        fiftyUsed = true;
        events.add(Event.LIFELINE_USED);
        return true;
    }

    /** Reveals the Bible reference for the current question. Only before answering, once per run. */
    public boolean usePeek() {
        if (!peekAvailable()) return false;
        peeked = true;
        peekUsed = true;
        events.add(Event.LIFELINE_USED);
        return true;
    }

    /** Skips the current rung without losing a life or scoring. Once per run. */
    public boolean usePass() {
        if (!passAvailable()) return false;
        passUsed = true;
        events.add(Event.LIFELINE_USED);
        index++;
        resetQuestion();
        if (finished()) events.add(Event.RUN_COMPLETE);
        return true;
    }

    public boolean answer(String value) {
        if (finished() || answered) return false;
        correct = current().accepts(value);
        answered = true;
        milestone = false;
        if (correct) {
            score++;
            events.add(Event.CORRECT);
            if (isMilestone(position())) {
                milestone = true;
                events.add(Event.MILESTONE);
            }
        } else {
            lives--;
            events.add(Event.WRONG);
            events.add(Event.LIFE_LOST);
        }
        if (position() == RUNGS) topAnswered = true;
        if (finished()) events.add(Event.RUN_COMPLETE);
        return correct;
    }

    /** Moves past the answered rung. Only valid after an answer. */
    public boolean next() {
        if (!answered || finished()) return false;
        index++;
        resetQuestion();
        return true;
    }

    /** Returns events since the last call, then clears them. */
    public List<Event> drainEvents() {
        List<Event> copy = new ArrayList<>(events);
        events.clear();
        return copy;
    }

    private void resetQuestion() {
        answered = false;
        correct = false;
        milestone = false;
        hidden = Collections.emptyList();
        peeked = false;
    }
}
