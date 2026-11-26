package org.perazimchurch.app.domain.games.vine;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CrosswordGridTest {

    private static CrosswordGrid.Slot across(String word, int row, int col) {
        return new CrosswordGrid.Slot(word, row, col, true);
    }

    private static CrosswordGrid.Slot down(String word, int row, int col) {
        return new CrosswordGrid.Slot(word, row, col, false);
    }

    @Test public void twoWordsThatCrossAreValid() {
        // VINE across on row 0; BEAN down from the E at column 3? Use NEAT down from N at (0,1).
        List<CrosswordGrid.Slot> slots = Arrays.asList(across("VINE", 0, 0), down("NET", 0, 2));
        CrosswordGrid grid = CrosswordGrid.of(3, 4, slots);
        assertTrue(grid.isValid());
        assertEquals('N', grid.letterAt(0, 2));
        assertEquals(0, grid.letterAt(2, 0));
    }

    @Test public void wordsTouchingWithoutCrossingAreInvalid() {
        // NOT and BE sit side by side on row 0 and read as one run "NOTBE".
        List<CrosswordGrid.Slot> slots = Arrays.asList(across("NOT", 0, 0), across("BE", 0, 3));
        CrosswordGrid grid = CrosswordGrid.of(1, 5, slots);
        assertFalse(grid.isValid());
    }

    @Test public void adjacentParallelWordsAreInvalid() {
        // Two across words one row apart create two-letter down runs that are not placed words.
        List<CrosswordGrid.Slot> slots = Arrays.asList(across("CAT", 0, 0), across("DOG", 1, 0));
        CrosswordGrid grid = CrosswordGrid.of(2, 3, slots);
        assertFalse(grid.isValid());
    }

    @Test public void clashingLettersAreRejected() {
        List<CrosswordGrid.Slot> slots = Arrays.asList(across("VINE", 0, 0), down("TREE", 0, 0));
        try {
            CrosswordGrid.of(4, 4, slots);
            fail("expected a clash");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("clashes"));
        }
    }

    @Test public void wordOutsideTheGridIsRejected() {
        try {
            CrosswordGrid.of(2, 2, Arrays.asList(across("VINE", 0, 0)));
            fail("expected the word to leave the grid");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("leaves the grid"));
        }
    }

    @Test public void letterCellCountCountsSharedCellsOnce() {
        CrosswordGrid grid = CrosswordGrid.of(3, 3, Arrays.asList(across("ABC", 0, 0), down("AXX", 0, 0)));
        assertEquals(5, grid.letterCellCount());
        assertTrue(grid.isLetterCell(0, 0));
        assertFalse(grid.isLetterCell(2, 2));
    }
}
