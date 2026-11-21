package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One journey along the road to Jerusalem: six stages, each a Bible question about a place or a
 * person on the way. Scores 10 points per correct stage with a combo bonus, and a wrong stage resets
 * the combo. The hint gives the first letter of the answer, costs 3 points, and is offered once per stage.
 */
public final class RoadSession implements GameSession {

    static final int STAGES = 6;
    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final List<String[]> stages = new ArrayList<>();
    private final long seed;
    private int index;
    private int points;
    private int combo;
    private boolean hintShown;
    private String lastFeedback;

    /** Builds a run from rows of the tier's bank, drawn in a seeded order. */
    RoadSession(String[] tierRows, long seed) {
        this.seed = seed;
        List<String> pool = new ArrayList<>(java.util.Arrays.asList(tierRows));
        Collections.shuffle(pool, new Random(seed));
        for (int i = 0; i < STAGES && i < pool.size(); i++) {
            stages.add(pool.get(i).split("~", -1));
        }
    }

    @Override public int size() { return stages.size(); }
    @Override public int position() { return Math.min(index + 1, stages.size()); }
    @Override public boolean finished() { return index >= stages.size(); }
    @Override public int score() { return points; }
    @Override public int lives() { return NO_LIVES; }

    @Override public boolean hintAvailable() { return !finished() && !hintShown; }

    @Override public String hint() {
        if (finished()) return null;
        hintShown = true;
        return "The answer starts with " + answer().charAt(0) + ".";
    }

    @Override public int hintsUsed() { return hintShown ? 1 : 0; }

    @Override public Outcome answer(String value) {
        if (finished()) return Outcome.IGNORED;
        boolean right = answer().equalsIgnoreCase(value == null ? "" : value.trim());
        if (right) {
            int bonus = Math.min(combo, COMBO_CAP) * COMBO_STEP;
            points += BASE_POINTS + bonus - (hintShown ? HINT_COST : 0);
            combo++;
        } else {
            combo = 0;
        }
        lastFeedback = answer() + " (" + reference() + "): " + explanation();
        hintShown = false;
        index++;
        return right ? Outcome.CORRECT : Outcome.WRONG;
    }

    @Override public String prompt() { return finished() ? null : question(); }

    /** The four options for the current stage, in a fixed shuffled order for this run. */
    public List<String> options() {
        if (finished()) return Collections.emptyList();
        String[] row = stages.get(index);
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= 4; i++) options.add(row[i]);
        Collections.shuffle(options, new Random(seed * 37 + index));
        return options;
    }

    /** The answer, reference and explanation for the stage just answered, or null before the first. */
    public String lastFeedback() { return lastFeedback; }

    /** The correct answer for the current stage; used by tests. */
    String answerForTest() { return finished() ? "" : answer(); }

    private String question() { return stages.get(index)[0]; }
    private String answer() { return stages.get(index)[1]; }
    private String reference() { return stages.get(index)[5]; }
    private String explanation() { return stages.get(index)[6]; }
}
