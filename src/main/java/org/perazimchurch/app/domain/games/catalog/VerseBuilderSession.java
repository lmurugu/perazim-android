package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.BibleGames;

/**
 * Session for Verse Builder. Wraps a {@link BibleGames.Round} of verses. Each verse has one try:
 * the screen builds the sentence from tiles and submits it, and the answer and reference are
 * shown afterwards. The tile puzzle for the current verse is created once and reused on redraw.
 */
public final class VerseBuilderSession extends AdvancingSession {
    private final BibleGames.Round round;
    private final long seed;
    private BibleGames.Card last;
    private BibleGames.VersePuzzle puzzle;

    public VerseBuilderSession(BibleGames.Round round, long seed) {
        this.round = round;
        this.seed = seed;
    }

    /** The verse answered most recently, or null before the first answer. */
    public BibleGames.Card lastCard() {
        return last;
    }

    /** The tile puzzle for the current verse, or null when the run is over. */
    public BibleGames.VersePuzzle puzzle() {
        if (round.finished()) return null;
        if (puzzle == null) puzzle = new BibleGames.VersePuzzle(round.current(), seed + round.position());
        return puzzle;
    }

    @Override protected boolean hasQuestion() { return !round.finished(); }

    @Override
    protected boolean submit(String value) {
        last = round.current();
        boolean right = round.answer(value);
        round.next();
        puzzle = null;
        return right;
    }

    @Override public int size() { return round.size(); }
    @Override public int position() { return Math.min(round.position(), round.size()); }
    @Override public boolean finished() { return round.finished(); }
    @Override public int score() { return round.score() + streak().bonusPoints(); }

    @Override
    public String prompt() {
        return round.finished() ? null : "Put the words of the verse in order";
    }

    @Override
    protected String hintText() {
        if (round.finished()) return null;
        return "Hint: the verse begins with \u201c" + round.current().words().get(0) + "\u201d.";
    }
}
