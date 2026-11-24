package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.engine.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One climb of Ladder of Faith as a generic game session. Scores 10 points per correct rung with a
 * combo bonus, costs a life per wrong answer, and offers one Verse Peek per run as its hint.
 */
public final class LadderSession implements GameSession {

    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final LadderOfFaith ladder;
    private final long seed;
    private int points;
    private int combo;
    private boolean hintShown;
    private String lastFeedback;

    LadderSession(LadderOfFaith ladder, long seed) {
        this.ladder = ladder;
        this.seed = seed;
    }

    @Override public int size() { return LadderOfFaith.RUNGS; }
    @Override public int position() { return Math.min(ladder.position(), LadderOfFaith.RUNGS); }
    @Override public boolean finished() { return ladder.finished(); }
    @Override public int score() { return points; }
    @Override public int lives() { return ladder.lives(); }

    @Override public boolean hintAvailable() {
        return !ladder.finished() && !ladder.answered() && ladder.peekAvailable();
    }

    /** The Bible reference for the current rung. Costs points, and is offered once per run. */
    @Override public String hint() {
        if (ladder.finished() || ladder.answered()) return null;
        if (!ladder.usePeek() && !ladder.peeked()) return null;
        hintShown = true;
        return ladder.current().reference;
    }

    @Override public int hintsUsed() { return hintShown ? 1 : 0; }

    @Override public Outcome answer(String value) {
        if (ladder.finished() || ladder.answered()) return Outcome.IGNORED;
        BibleGames.Card card = ladder.current();
        boolean right = ladder.answer(value);
        if (right) {
            int bonus = Math.min(combo, COMBO_CAP) * COMBO_STEP;
            points += BASE_POINTS + bonus - (hintShown ? HINT_COST : 0);
            combo++;
        } else {
            combo = 0;
        }
        lastFeedback = card.answer + " (" + card.reference + "): " + card.explanation;
        hintShown = false;
        ladder.next();
        return right ? Outcome.CORRECT : Outcome.WRONG;
    }

    /** The question for the current rung. */
    @Override public String prompt() { return finished() ? null : ladder.current().prompt; }

    /** Answer options for the current rung, in a fixed shuffled order for this run. */
    public List<String> options() {
        if (finished()) return Collections.emptyList();
        List<String> shuffled = new ArrayList<>(ladder.current().options);
        Collections.shuffle(shuffled, new Random(seed * 31 + ladder.position()));
        return shuffled;
    }

    /** The answer, reference and explanation for the rung just answered, or null before the first answer. */
    public String lastFeedback() { return lastFeedback; }
}
