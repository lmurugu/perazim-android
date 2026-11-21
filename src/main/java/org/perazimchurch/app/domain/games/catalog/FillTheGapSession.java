package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.FillTheGap;

import java.util.Collections;
import java.util.List;

/**
 * Session for Fill the Gap. Wraps {@link FillTheGap} and keeps the last verse, so the play screen
 * can show the missing word and reference after each choice.
 */
public final class FillTheGapSession extends AdvancingSession {
    private final FillTheGap game;
    private FillTheGap.Question last;

    public FillTheGapSession(FillTheGap game) {
        this.game = game;
    }

    /** The verse answered most recently, or null before the first answer. */
    public FillTheGap.Question lastQuestion() {
        return last;
    }

    /** The word choices for the current verse, or an empty list when the run is over. */
    public List<String> choices() {
        return game.finished() ? Collections.<String>emptyList() : game.current().choices;
    }

    @Override protected boolean hasQuestion() { return !game.finished(); }

    @Override
    protected boolean submit(String value) {
        last = game.current();
        return game.answer(value);
    }

    @Override public int size() { return game.size(); }
    @Override public int position() { return game.position(); }
    @Override public boolean finished() { return game.finished(); }
    @Override public int score() { return game.score() + streak().bonusPoints(); }

    @Override
    public String prompt() {
        return game.finished() ? null : game.current().before + "_____" + game.current().after;
    }
}
