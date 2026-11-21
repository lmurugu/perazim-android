package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * One run of Story Timeline. The events are shown shuffled; the player taps them in story order.
 * A correct tap scores with a combo bonus for consecutive correct taps in this run only. A wrong
 * tap resets the combo and gives a gentle nudge. Hints name the book of the next event.
 */
final class StoryTimelineSession implements GameSession {

    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final List<StoryChapterBank.Event> ordered;
    private final List<StoryChapterBank.Event> shuffled = new ArrayList<>();
    private final TimelineDefinition.Tier tier;
    private int placed;
    private int score;
    private int combo;
    private boolean hintShown;
    private String lastFeedback;

    StoryTimelineSession(List<StoryChapterBank.Event> events, TimelineDefinition.Tier tier, long seed) {
        this.ordered = new ArrayList<>(events);
        this.tier = tier;
        shuffled.addAll(ordered);
        Collections.shuffle(shuffled, new Random(seed));
        if (shuffled.equals(ordered) && shuffled.size() > 1) {
            Collections.reverse(shuffled);
        }
    }

    @Override public int size() { return ordered.size(); }
    @Override public int position() { return Math.min(placed + 1, size()); }
    @Override public boolean finished() { return placed >= ordered.size(); }
    @Override public int score() { return score; }
    @Override public int lives() { return NO_LIVES; }

    @Override
    public boolean hintAvailable() {
        return !finished() && tier != TimelineDefinition.Tier.HARD && !hintShown;
    }

    @Override
    public String hint() {
        if (!hintAvailable()) return null;
        hintShown = true;
        return "Look in " + bookOf(next().reference) + ".";
    }

    @Override
    public String prompt() {
        return finished() ? null : "Tap the events in the order they happen in the story.";
    }

    /** Events shown to the player, shuffled. */
    public List<StoryChapterBank.Event> shuffled() { return Collections.unmodifiableList(shuffled); }

    /** Explanation with the reference after the last answer, or null before the first answer. */
    public String lastFeedback() { return lastFeedback; }

    /** Current combo streak within this run. */
    int combo() { return combo; }

    /** Taps the shuffled event at this index. The answer is the index as text. */
    @Override
    public Outcome answer(String value) {
        if (finished()) return Outcome.IGNORED;
        int index;
        try {
            index = Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return Outcome.IGNORED;
        }
        if (index < 0 || index >= shuffled.size()) return Outcome.IGNORED;
        StoryChapterBank.Event tapped = shuffled.get(index);
        if (tapped == next()) {
            combo++;
            int bonus = Math.min(combo - 1, COMBO_CAP) * COMBO_STEP;
            int points = BASE_POINTS + bonus - (hintShown ? HINT_COST : 0);
            score += Math.max(1, points);
            lastFeedback = tapped.title + " (" + tapped.reference + ")";
            placed++;
            hintShown = false;
            return Outcome.CORRECT;
        }
        combo = 0;
        lastFeedback = "Not quite yet. Try the event that comes next in the story.";
        return Outcome.WRONG;
    }

    private StoryChapterBank.Event next() { return ordered.get(placed); }

    /** The book name of a reference, for example "1 Samuel" from "1 Samuel 17:50". */
    static String bookOf(String reference) {
        return reference.replaceAll("\\s+\\d+(?::[\\d\\-]+)?$", "");
    }
}
