package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.mosaic.MosaicBoard;

import java.util.Arrays;

import static org.junit.Assert.*;

public class StoryMosaicBoardTest {

    @Test public void trayIsAPermutationOfAllPieces() {
        MosaicBoard board = new MosaicBoard(4, 7L);
        int[] tray = board.trayOrder();
        int[] sorted = tray.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length; i++) assertEquals(i, sorted[i]);
        assertEquals(16, board.pieceCount());
    }

    @Test public void sameSeedGivesSameTrayAndDifferentSeedsUsuallyDiffer() {
        assertArrayEquals(new MosaicBoard(5, 99L).trayOrder(), new MosaicBoard(5, 99L).trayOrder());
        assertFalse(Arrays.equals(new MosaicBoard(5, 1L).trayOrder(), new MosaicBoard(5, 2L).trayOrder()));
    }

    @Test public void trayOrderCopyCannotChangeTheBoard() {
        MosaicBoard board = new MosaicBoard(3, 5L);
        int[] copy = board.trayOrder();
        copy[0] = 99;
        assertNotEquals(99, board.trayOrder()[0]);
    }

    @Test public void correctDropPlacesPieceAndWrongDropReturnsIt() {
        MosaicBoard board = new MosaicBoard(3, 3L);
        assertEquals(MosaicBoard.Drop.WRONG, board.drop(4, 5));
        assertFalse(board.isPlaced(4));
        assertEquals(1, board.wrongDrops());
        assertEquals(MosaicBoard.Drop.PLACED, board.drop(4, 4));
        assertTrue(board.isPlaced(4));
        assertEquals(1, board.placedCount());
    }

    @Test public void placedPieceAndOutOfRangeAreIgnored() {
        MosaicBoard board = new MosaicBoard(2, 3L);
        board.drop(1, 1);
        assertEquals(MosaicBoard.Drop.IGNORED, board.drop(1, 1));
        assertEquals(MosaicBoard.Drop.IGNORED, board.drop(1, 2));
        assertEquals(MosaicBoard.Drop.IGNORED, board.drop(-1, 0));
        assertEquals(MosaicBoard.Drop.IGNORED, board.drop(0, 9));
        assertEquals(0, board.wrongDrops());
    }

    @Test public void completesWhenEveryPieceIsPlaced() {
        MosaicBoard board = new MosaicBoard(3, 11L);
        for (int p = 0; p < 9; p++) {
            assertFalse(board.complete());
            board.drop(p, p);
        }
        assertTrue(board.complete());
        assertEquals(-1, board.hintPiece());
    }

    @Test public void hintPointsAtFirstUnplacedPieceInTrayOrder() {
        MosaicBoard board = new MosaicBoard(3, 21L);
        int first = board.trayOrder()[0];
        assertEquals(first, board.hintPiece());
        board.drop(first, first);
        assertNotEquals(first, board.hintPiece());
        assertFalse(board.isPlaced(board.hintPiece()));
    }

    @Test public void slotAtFindsCellsAndRejectsOutsidePoints() {
        MosaicBoard board = new MosaicBoard(3, 1L);
        assertEquals(0, board.slotAt(5f, 5f, 100f));
        assertEquals(5, board.slotAt(250f, 150f, 100f));
        assertEquals(8, board.slotAt(299f, 299f, 100f));
        assertEquals(-1, board.slotAt(300f, 10f, 100f));
        assertEquals(-1, board.slotAt(-1f, 10f, 100f));
        assertEquals(-1, board.slotAt(10f, 10f, 0f));
    }

    @Test public void sideForLevelFollowsTheBands() {
        assertEquals(3, MosaicBoard.sideForLevel(1));
        assertEquals(3, MosaicBoard.sideForLevel(5));
        assertEquals(4, MosaicBoard.sideForLevel(6));
        assertEquals(5, MosaicBoard.sideForLevel(16));
        assertEquals(6, MosaicBoard.sideForLevel(31));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSideZero() {
        new MosaicBoard(0, 1L);
    }
}
