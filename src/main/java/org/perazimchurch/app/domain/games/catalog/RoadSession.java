package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One journey along the road to Jerusalem. The track has {@link #FINISH} squares. Each turn rolls a
 * die; the traveller moves that many squares and lands on a square that asks a Bible stage. A correct
 * answer keeps the move and scores with a combo bonus. A wrong answer leaves the traveller where
 * they stood and resets the combo. The run ends at Jerusalem or after {@link #TURNS} turns.
 * The hint gives the first letter of the answer, costs 3 points and is offered once per stage.
 */
public final class RoadSession implements GameSession {

    public static final int FINISH = 20;
    static final int TURNS = 12;
    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final List<String[]> stages = new ArrayList<>();
    private final int[] rolls = new int[TURNS];
    private int turn;
    private int square;
    private int points;
    private int combo;
    private boolean hintShown;
    private String lastFeedback;

    /** Builds a run from the tier's bank rows, drawn in a seeded order, with seeded dice. */
    RoadSession(String[] tierRows, long seed) {
        List<String> pool = new ArrayList<>(Arrays.asList(tierRows));
        Collections.shuffle(pool, new Random(seed));
        for (int i = 0; i < TURNS && i < pool.size(); i++) stages.add(pool.get(i).split("~", -1));
        Random dice = new Random(seed * 7919L + 13);
        for (int i = 0; i < TURNS; i++) rolls[i] = 1 + dice.nextInt(6);
    }

    @Override public int size() { return Math.min(TURNS, stages.size()); }
    @Override public int position() { return Math.min(turn + 1, size()); }
    @Override public boolean finished() { return square >= FINISH || turn >= size(); }
    @Override public int score() { return points; }
    @Override public int lives() { return NO_LIVES; }

    /** The traveller's square on the track, from 0 (the start) to {@link #FINISH} (Jerusalem). */
    public int square() { return square; }

    /** The die roll for the current turn. */
    public int roll() { return finished() ? 0 : rolls[turn]; }

    /** The square the traveller would land on with this turn's roll, capped at Jerusalem. */
    public int landingSquare() { return Math.min(square + roll(), FINISH); }

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
            square = landingSquare();
        } else {
            combo = 0;
        }
        lastFeedback = answer() + " (" + reference() + "): " + explanation();
        hintShown = false;
        turn++;
        return right ? Outcome.CORRECT : Outcome.WRONG;
    }

    @Override public String prompt() { return finished() ? null : question(); }

    /** The four options for the current stage, in a fixed shuffled order for this turn. */
    public List<String> options() {
        if (finished()) return Collections.emptyList();
        String[] row = stages.get(turn);
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= 4; i++) options.add(row[i]);
        Collections.shuffle(options, new Random(turn * 37L + row[0].length()));
        return options;
    }

    /** The answer, reference and explanation for the last stage answered, or null before the first. */
    public String lastFeedback() { return lastFeedback; }

    /** The correct answer for the current stage; used by tests. */
    String answerForTest() { return finished() ? "" : answer(); }

    private String question() { return stages.get(turn)[0]; }
    private String answer() { return stages.get(turn)[1]; }
    private String reference() { return stages.get(turn)[5]; }
    private String explanation() { return stages.get(turn)[6]; }
}
