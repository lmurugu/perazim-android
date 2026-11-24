package org.perazimchurch.app.domain.games.lampslide;

import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * One Lamp Slide run as the Games engine sees it.
 *
 * <p>size() is par, position() is moves made (capped at par, at least 1), finished() is the solved
 * board, and score() is the stars earned once solved (0 before that). Lives do not apply.
 * answer("up" | "down" | "left" | "right") slides a tile in that direction into the gap.
 */
public final class LampSlideSession implements GameSession {

    private final LampSlidePuzzle puzzle;
    private final LampSlideScene scene;
    private int hintsUsed;

    public LampSlideSession(LampSlidePuzzle puzzle, LampSlideScene scene) {
        if (puzzle == null || scene == null) throw new IllegalArgumentException("puzzle and scene are required");
        this.puzzle = puzzle;
        this.scene = scene;
    }

    public LampSlidePuzzle puzzle() { return puzzle; }
    public LampSlideScene scene() { return scene; }
    public int hintsUsed() { return hintsUsed; }

    @Override public int size() { return puzzle.par(); }

    @Override public int position() { return Math.max(1, Math.min(puzzle.moves(), size())); }

    @Override public boolean finished() { return puzzle.solved(); }

    @Override public int score() {
        if (!finished()) return 0;
        return LampSlidePuzzle.stars(puzzle.moves(), puzzle.par(), hintsUsed);
    }

    @Override public int lives() { return NO_LIVES; }

    @Override public boolean hintAvailable() { return !finished(); }

    /** The "One move" hint text. Each call counts as a hint use. Null once solved. */
    @Override public String hint() {
        int tile = useOneMoveHint();
        if (tile < 0) return null;
        int size = puzzle.size();
        return "Slide the tile at row " + (tile / size + 1) + ", column " + (tile % size + 1) + ".";
    }

    /** Slides a tile in a direction. Returns IGNORED for an illegal or unknown move. */
    @Override public Outcome answer(String value) {
        LampSlidePuzzle.Direction direction = LampSlidePuzzle.Direction.parse(value);
        if (finished() || direction == null || !puzzle.slide(direction)) return Outcome.IGNORED;
        return finished() ? Outcome.CORRECT : Outcome.MOVED;
    }

    /** Taps the tile at an index. Returns IGNORED when that tile cannot slide into the gap. */
    public Outcome tapTile(int index) {
        if (finished() || !puzzle.tap(index)) return Outcome.IGNORED;
        return finished() ? Outcome.CORRECT : Outcome.MOVED;
    }

    /** Takes back the last slide. Returns false when there is nothing to undo. */
    public boolean undoMove() {
        return !finished() && puzzle.undo();
    }

    /** Records a use of the free "Show the picture" peek, which lowers the star cap by one. */
    public void recordPeek() {
        hintsUsed++;
    }

    /** Records a use of "One move" and returns the helpful tile index, or -1 when solved. */
    public int useOneMoveHint() {
        if (finished()) return -1;
        hintsUsed++;
        return puzzle.helpfulTile();
    }
}
