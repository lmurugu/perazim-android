package org.perazimchurch.app.domain.games.lampslide;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LampSlidePuzzleTest {

    private static final LampSlidePuzzle.Direction UP = LampSlidePuzzle.Direction.UP;
    private static final LampSlidePuzzle.Direction DOWN = LampSlidePuzzle.Direction.DOWN;
    private static final LampSlidePuzzle.Direction LEFT = LampSlidePuzzle.Direction.LEFT;

    @Test
    public void everyScrambledBoardIsSolvable() {
        for (int size = LampSlidePuzzle.MIN_SIZE; size <= LampSlidePuzzle.MAX_SIZE; size++) {
            for (long seed = 0; seed < 200; seed++) {
                LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(size, seed);
                assertTrue("size " + size + " seed " + seed,
                        LampSlidePuzzle.isSolvable(size, puzzle.tiles()));
            }
        }
    }

    @Test
    public void scrambledBoardIsNeverSolvedAndParIsWalkLength() {
        for (int size = 3; size <= 5; size++) {
            for (long seed = 0; seed < 50; seed++) {
                LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(size, seed);
                assertFalse(puzzle.solved());
                assertEquals(LampSlidePuzzle.walkLength(size), puzzle.par());
                assertEquals(0, puzzle.moves());
            }
        }
    }

    @Test
    public void sameSeedGivesSameBoard() {
        assertArrayEquals(LampSlidePuzzle.scrambled(4, 42).tiles(),
                LampSlidePuzzle.scrambled(4, 42).tiles());
    }

    @Test
    public void solvedBoardIsDetected() {
        assertTrue(LampSlidePuzzle.isSolvedArray(LampSlidePuzzle.solvedTiles(3)));
        assertFalse(LampSlidePuzzle.isSolvedArray(new int[] {2, 1, 3, 4, 5, 6, 7, 8, 0}));
    }

    @Test
    public void solvabilityRulesMatchKnownBoards() {
        assertTrue(LampSlidePuzzle.isSolvable(3, LampSlidePuzzle.solvedTiles(3)));
        assertTrue(LampSlidePuzzle.isSolvable(4, LampSlidePuzzle.solvedTiles(4)));
        // Swapping two tiles on a 3 by 3 board with the gap in place makes it unsolvable.
        assertFalse(LampSlidePuzzle.isSolvable(3, new int[] {2, 1, 3, 4, 5, 6, 7, 8, 0}));
        // Swapping the top two tiles of a 4 by 4 solved board is unsolvable.
        int[] swapped = LampSlidePuzzle.solvedTiles(4);
        swapped[0] = 2;
        swapped[1] = 1;
        assertFalse(LampSlidePuzzle.isSolvable(4, swapped));
    }

    @Test
    public void tileTwoSquaresAwayCannotMove() {
        LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(3, 7);
        int[] before = puzzle.tiles();
        int distant = -1;
        for (int i = 0; i < 9; i++) {
            if (i != puzzle.gapIndex() && !adjacent(i, puzzle.gapIndex(), 3)) {
                distant = i;
                break;
            }
        }
        assertFalse(puzzle.tap(distant));
        assertArrayEquals(before, puzzle.tiles());
        assertEquals(0, puzzle.moves());
    }

    @Test
    public void legalTapMovesTileIntoGapAndCountsMove() {
        LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(3, 9);
        int gap = puzzle.gapIndex();
        int neighbour = firstAdjacent(puzzle);
        int tile = puzzle.tileAt(neighbour);
        assertTrue(puzzle.tap(neighbour));
        assertEquals(neighbour, puzzle.gapIndex());
        assertEquals(tile, puzzle.tileAt(gap));
        assertEquals(1, puzzle.moves());
    }

    @Test
    public void directionMovesTileFromBehindTheGap() {
        // Gap in the centre. UP moves the tile below the gap (value 8) up into it.
        LampSlidePuzzle puzzle = LampSlidePuzzle.ofBoard(3, new int[] {1, 2, 3, 4, 0, 6, 7, 8, 5}, 5);
        assertTrue(puzzle.slide(UP));
        assertEquals(8, puzzle.tileAt(4));
        assertEquals(0, puzzle.tileAt(7));
    }

    @Test
    public void slideTowardMissingEdgeIsIllegal() {
        // Gap on the bottom row: nothing sits below it, so UP has no tile to move.
        LampSlidePuzzle puzzle = LampSlidePuzzle.ofBoard(3, new int[] {1, 2, 3, 4, 5, 6, 7, 0, 8}, 5);
        int[] before = puzzle.tiles();
        assertFalse(puzzle.slide(UP));
        assertArrayEquals(before, puzzle.tiles());
        // LEFT works: the tile on the right (8) slides left into the gap.
        assertTrue(puzzle.slide(LEFT));
        assertEquals(8, puzzle.tileAt(7));
    }

    @Test
    public void unknownDirectionTextIsIgnored() {
        assertEquals(null, LampSlidePuzzle.Direction.parse("sideways"));
        assertEquals(DOWN, LampSlidePuzzle.Direction.parse(" Down "));
    }

    @Test
    public void undoRestoresBoardAndMoveCount() {
        LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(4, 3);
        int[] start = puzzle.tiles();
        assertTrue(puzzle.tap(firstAdjacent(puzzle)));
        assertTrue(puzzle.tap(firstAdjacent(puzzle)));
        assertEquals(2, puzzle.moves());
        assertTrue(puzzle.undo());
        assertTrue(puzzle.undo());
        assertArrayEquals(start, puzzle.tiles());
        assertEquals(0, puzzle.moves());
        assertFalse(puzzle.undo());
    }

    @Test
    public void oneSlideFinishesTheBoard() {
        LampSlidePuzzle puzzle = LampSlidePuzzle.ofBoard(3, new int[] {1, 2, 3, 4, 5, 6, 7, 0, 8}, 1);
        assertFalse(puzzle.solved());
        assertTrue(puzzle.tap(8));
        assertTrue(puzzle.solved());
        assertEquals(1, puzzle.moves());
    }

    @Test
    public void helpfulTileIsLegalToMove() {
        LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(3, 11);
        int hint = puzzle.helpfulTile();
        assertTrue(puzzle.canTap(hint));
    }

    @Test
    public void helpfulTileIsMinusOneWhenSolved() {
        assertEquals(-1, LampSlidePuzzle.ofBoard(3, LampSlidePuzzle.solvedTiles(3), 1).helpfulTile());
    }

    @Test
    public void starsFollowParAndHintCap() {
        assertEquals(3, LampSlidePuzzle.stars(20, 20, 0));
        assertEquals(2, LampSlidePuzzle.stars(21, 20, 0));
        assertEquals(2, LampSlidePuzzle.stars(30, 20, 0));
        assertEquals(1, LampSlidePuzzle.stars(31, 20, 0));
        assertEquals(2, LampSlidePuzzle.stars(5, 20, 1));
        assertEquals(1, LampSlidePuzzle.stars(5, 20, 2));
        assertEquals(1, LampSlidePuzzle.stars(5, 20, 5));
    }

    @Test
    public void inPlaceCountsTilesOnTheirSquareOnly() {
        LampSlidePuzzle solved = LampSlidePuzzle.ofBoard(3, LampSlidePuzzle.solvedTiles(3), 1);
        assertEquals(8, solved.inPlace());
        LampSlidePuzzle puzzle = LampSlidePuzzle.ofBoard(3, new int[] {1, 2, 3, 4, 5, 6, 7, 0, 8}, 1);
        assertEquals(7, puzzle.inPlace());
        assertTrue(puzzle.tap(8));
        assertEquals(8, puzzle.inPlace());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sizeOutsideRangeIsRejected() {
        LampSlidePuzzle.scrambled(6, 1);
    }

    private static boolean adjacent(int a, int b, int size) {
        return Math.abs(a / size - b / size) + Math.abs(a % size - b % size) == 1;
    }

    private static int firstAdjacent(LampSlidePuzzle puzzle) {
        for (int i = 0; i < puzzle.size() * puzzle.size(); i++) {
            if (puzzle.canTap(i)) return i;
        }
        throw new AssertionError("no tile next to the gap");
    }
}
