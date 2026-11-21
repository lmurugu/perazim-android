package org.perazimchurch.app.domain.games.mosaic;

import java.util.Random;

/**
 * A square picture puzzle for Story Mosaic. Piece {@code p} belongs in slot {@code p}. The tray
 * order is shuffled from a seed, so the same seed always gives the same tray. Pure Java, no
 * Android types, so the rules can be tested on the JVM.
 *
 * <p>Slots are numbered row by row from 0 (top left) to {@code side * side - 1} (bottom right).
 */
public final class MosaicBoard {

    /** Outcome of dropping one piece on one slot. */
    public enum Drop { PLACED, WRONG, IGNORED }

    /** Largest grid side the board supports (8 by 8 = 64 pieces). */
    public static final int MAX_SIDE = 8;

    private final int side;
    private final int[] tray;
    private final boolean[] placed;
    private int placedCount;
    private int wrongDrops;

    /**
     * @param side grid side, from 1 to {@link #MAX_SIDE}
     * @param seed seed for the tray order
     */
    public MosaicBoard(int side, long seed) {
        if (side < 1 || side > MAX_SIDE) throw new IllegalArgumentException("side out of range: " + side);
        this.side = side;
        int n = side * side;
        this.tray = new int[n];
        for (int i = 0; i < n; i++) tray[i] = i;
        Random random = new Random(seed);
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = tray[i];
            tray[i] = tray[j];
            tray[j] = swap;
        }
        this.placed = new boolean[n];
    }

    /** Grid side for a level: 3 for levels 1 to 5, 4 for 6 to 15, 5 for 16 to 30, 6 from 31. */
    public static int sideForLevel(int level) {
        if (level <= 5) return 3;
        if (level <= 15) return 4;
        if (level <= 30) return 5;
        return 6;
    }

    public int side() { return side; }

    public int pieceCount() { return side * side; }

    public int placedCount() { return placedCount; }

    public int wrongDrops() { return wrongDrops; }

    public boolean isPlaced(int piece) {
        return piece >= 0 && piece < placed.length && placed[piece];
    }

    public boolean complete() { return placedCount == placed.length; }

    /** Piece ids in tray order. A copy, so callers cannot change the board. */
    public int[] trayOrder() { return tray.clone(); }

    public int row(int slot) { return slot / side; }

    public int column(int slot) { return slot % side; }

    /**
     * Drops a piece on a slot. A piece is placed only on its own slot. Anything else is a wrong
     * drop and the piece goes back to the tray. A piece that is already placed is ignored.
     */
    public Drop drop(int piece, int slot) {
        if (piece < 0 || piece >= placed.length || slot < 0 || slot >= placed.length) return Drop.IGNORED;
        if (placed[piece]) return Drop.IGNORED;
        if (slot != piece) {
            wrongDrops++;
            return Drop.WRONG;
        }
        placed[piece] = true;
        placedCount++;
        return Drop.PLACED;
    }

    /**
     * Finds the slot under a point, for a drop on the board. Coordinates are in pixels from the
     * top left of the board, and {@code cell} is the size of one slot in pixels.
     *
     * @return the slot, or -1 when the point is outside the board
     */
    public int slotAt(float x, float y, float cell) {
        if (cell <= 0f || x < 0f || y < 0f) return -1;
        int column = (int) (x / cell);
        int row = (int) (y / cell);
        if (column >= side || row >= side) return -1;
        return row * side + column;
    }

    /** The first unplaced piece in tray order, used for a guided hint. -1 when complete. */
    public int hintPiece() {
        for (int piece : tray) {
            if (!placed[piece]) return piece;
        }
        return -1;
    }
}
