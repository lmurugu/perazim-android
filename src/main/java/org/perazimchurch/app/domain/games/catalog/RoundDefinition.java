package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for the Bible Quiz and Verse Builder rounds in {@link BibleGames}. */
public final class RoundDefinition implements GameDefinition {
    private final String id;
    private final String title;
    private final String description;
    private final int iconResource;
    private final BibleGames.Mode mode;

    public RoundDefinition(String id, String title, String description, int iconResource, BibleGames.Mode mode) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResource = iconResource;
        this.mode = mode;
    }

    @Override public String id() { return id; }
    @Override public String title() { return title; }
    @Override public String description() { return description; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final BibleGames.Round round = new BibleGames.Round(mode, seed);
        if (mode == BibleGames.Mode.QUIZ) return new QuizSession(round, seed);
        if (mode == BibleGames.Mode.VERSE_ORDER) return new VerseBuilderSession(round, seed);
        return new AdvancingSession() {
            @Override protected boolean hasQuestion() { return !round.finished(); }
            @Override protected boolean submit(String value) {
                boolean right = round.answer(value);
                round.next();
                return right;
            }
            @Override public int size() { return round.size(); }
            @Override public String prompt() {
                return round.finished() ? null : round.current().prompt;
            }
            @Override public int position() { return Math.min(round.position(), round.size()); }
            @Override public boolean finished() { return round.finished(); }
            @Override public int score() { return round.score(); }
        };
    }
}
