package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.mosaic.MosaicBoard;
import org.perazimchurch.app.domain.games.mosaic.MosaicPuzzles;

/**
 * One run of Story Mosaic: one scene, cut into a square grid. Answers are "piece:slot" strings,
 * so the hub's shared results screen works. The drag view calls {@link #answer} directly.
 *
 * <p>Score is pieces placed without a wrong drop, so a run with no wrong drops is 3 stars and one
 * with few mistakes is still 2 stars under {@code StarRating}'s 60 percent rule.
 */
public final class StoryMosaicSession implements GameSession {

    private final MosaicPuzzles.Puzzle puzzle;
    private final MosaicBoard board;
    private final boolean guided;

    public StoryMosaicSession(MosaicPuzzles.Puzzle puzzle, int side, boolean guided, long seed) {
        this.puzzle = puzzle;
        this.board = new MosaicBoard(side, seed);
        this.guided = guided;
    }

    public MosaicPuzzles.Puzzle puzzle() { return puzzle; }

    public MosaicBoard board() { return board; }

    /** Guided puzzles show a hint button. Quiet puzzles do not. */
    public boolean guided() { return guided; }

    @Override public int size() { return board.pieceCount(); }

    @Override public int position() { return Math.min(board.placedCount() + 1, size()); }

    @Override public boolean finished() { return board.complete(); }

    @Override public int score() { return Math.max(0, size() - board.wrongDrops()); }

    @Override public int lives() { return NO_LIVES; }

    @Override public boolean hintAvailable() { return guided && !board.complete(); }

    @Override public String hint() {
        if (!hintAvailable()) return null;
        int piece = board.hintPiece();
        if (piece < 0) return null;
        return "Look for the piece for row " + (board.row(piece) + 1)
                + ", column " + (board.column(piece) + 1) + ".";
    }

    @Override public String prompt() { return puzzle.chapterTitle; }

    @Override
    public Outcome answer(String value) {
        if (value == null) return Outcome.IGNORED;
        int colon = value.indexOf(':');
        if (colon < 0) return Outcome.IGNORED;
        int piece;
        int slot;
        try {
            piece = Integer.parseInt(value.substring(0, colon).trim());
            slot = Integer.parseInt(value.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return Outcome.IGNORED;
        }
        switch (board.drop(piece, slot)) {
            case PLACED: return Outcome.CORRECT;
            case WRONG: return Outcome.WRONG;
            default: return Outcome.IGNORED;
        }
    }
}
