package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.BibleGames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Session for Bible Quiz. Wraps a {@link BibleGames.Round} of quiz cards and keeps the last card,
 * so the play screen can show the explanation and reference after each choice. The choices are
 * shuffled per question with a seed, so they stay in the same order when the screen redraws.
 */
public final class QuizSession extends AdvancingSession {
    private final BibleGames.Round round;
    private final long seed;
    private BibleGames.Card last;

    public QuizSession(BibleGames.Round round, long seed) {
        this.round = round;
        this.seed = seed;
    }

    /** The card answered most recently, or null before the first answer. */
    public BibleGames.Card lastCard() {
        return last;
    }

    /** The options for the current question in a stable shuffled order, or an empty list when done. */
    public List<String> choices() {
        if (round.finished()) return Collections.emptyList();
        List<String> options = new ArrayList<>(round.current().options);
        Collections.shuffle(options, new Random(seed + round.position()));
        return options;
    }

    @Override protected boolean hasQuestion() { return !round.finished(); }

    @Override
    protected boolean submit(String value) {
        last = round.current();
        boolean right = round.answer(value);
        round.next();
        return right;
    }

    @Override public int size() { return round.size(); }
    @Override public int position() { return Math.min(round.position(), round.size()); }
    @Override public boolean finished() { return round.finished(); }
    @Override public int score() { return round.score() + streak().bonusPoints(); }

    @Override
    public String prompt() {
        return round.finished() ? null : round.current().prompt;
    }

    @Override
    protected String hintText() {
        if (round.finished() || round.current().hint.isEmpty()) return null;
        return "Hint: " + round.current().hint;
    }
}
