package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.TestamentSort;

/**
 * Session for Old or New?. Wraps {@link TestamentSort} and keeps the book and fact of the
 * last answer, so the play screen can show the fact after each placement.
 */
public final class TestamentSession extends AdvancingSession {
    private final TestamentSort game;
    private TestamentSort.Book lastBook;

    public TestamentSession(TestamentSort game) {
        this.game = game;
    }

    /** The book placed by the most recent answer, or null before the first answer. */
    public TestamentSort.Book lastBook() {
        return lastBook;
    }

    @Override protected boolean hasQuestion() { return !game.finished(); }

    @Override
    protected boolean submit(String value) {
        lastBook = game.current();
        TestamentSort.Testament choice = "OLD".equals(value) ? TestamentSort.Testament.OLD
                : TestamentSort.Testament.NEW;
        return game.place(choice);
    }

    @Override public int size() { return game.size(); }
    @Override public int position() { return game.position(); }
    @Override public boolean finished() { return game.finished(); }
    @Override public int score() { return game.score() + streak().bonusPoints(); }

    @Override
    public String prompt() {
        return game.finished() ? null : "Which testament is " + game.current().name + " in?";
    }

    @Override
    protected String hintText() {
        return null;
    }
}
