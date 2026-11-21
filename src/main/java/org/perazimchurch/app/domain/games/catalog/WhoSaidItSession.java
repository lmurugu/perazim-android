package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.WhoSaidIt;

/**
 * Session for Who Said It?. Wraps {@link WhoSaidIt} and keeps the last quotation, so the play
 * screen can show the answer and reference after each choice.
 */
public final class WhoSaidItSession extends AdvancingSession {
    private final WhoSaidIt game;
    private WhoSaidIt.Question last;

    public WhoSaidItSession(WhoSaidIt game) {
        this.game = game;
    }

    /** The quotation answered most recently, or null before the first answer. */
    public WhoSaidIt.Question lastQuestion() {
        return last;
    }

    /** The speakers offered for the current quotation, or an empty list when the run is over. */
    public java.util.List<String> choices() {
        return game.finished() ? java.util.Collections.<String>emptyList() : game.current().choices;
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
        return game.finished() ? null : "“" + game.current().quote + "”";
    }
}
