package org.perazimchurch.app.domain.games.lampslide;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Sliding-tile board for Lamp Slide. Pure Java, no Android types, so every rule is unit tested.
 *
 * <p>Tiles are numbered 1 to n*n-1 in reading order and 0 is the gap. The solved board is
 * 1, 2, ..., n*n-1, 0. Every starting board is built by sliding tiles from the solved board, so
 * every board can be solved. A slide is legal only when the tile is next to the gap.
 */
public final class LampSlidePuzzle {

    /** Direction a tile travels into the gap. UP means the tile below the gap moves up. */
    public enum Direction {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);

        final int dRow;
        final int dCol;

        Direction(int dRow, int dCol) {
            this.dRow = dRow;
            this.dCol = dCol;
        }

        /** Parses "up", "down", "left" or "right" (any case). Returns null for anything else. */
        public static Direction parse(String value) {
            if (value == null) return null;
            switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "up": return UP;
                case "down": return DOWN;
                case "left": return LEFT;
                case "right": return RIGHT;
                default: return null;
            }
        }
    }

    /** Smallest and largest board side. */
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 5;

    private final int size;
    private final int[] tiles;
    private final int par;
    private int gap;
    /** Each entry is {from, to}: the tile moved from "from" into the gap at "to". */
    private final Deque<int[]> history = new ArrayDeque<>();

    /** Builds a board from explicit tiles. Used by tests and by level data with a known layout. */
    static LampSlidePuzzle ofBoard(int size, int[] tiles, int par) {
        checkSize(size);
        int gap = -1;
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] == 0) gap = i;
        }
        if (gap < 0 || tiles.length != size * size) throw new IllegalArgumentException("invalid board");
        return new LampSlidePuzzle(size, tiles.clone(), gap, par);
    }

    private LampSlidePuzzle(int size, int[] tiles, int gap, int par) {
        this.size = size;
        this.tiles = tiles;
        this.gap = gap;
        this.par = par;
    }

    /**
     * Builds a scrambled board by walking the gap through random legal slides from the solved
     * board. The walk never undoes its previous slide, so the board is never already solved.
     * Par is the number of slides in that walk, which is an achievable solution length.
     *
     * @param size board side, 3 to 5
     * @param seed the same seed always gives the same board
     */
    public static LampSlidePuzzle scrambled(int size, long seed) {
        checkSize(size);
        int n = size * size;
        int[] t = solvedTiles(size);
        int gap = n - 1;
        int steps = walkLength(size);
        Random random = new Random(seed);
        int previousGap = -1;
        int made = 0;
        while (made < steps || isSolvedArray(t)) {
            int[] options = neighbours(gap, size);
            int from;
            do {
                from = options[random.nextInt(options.length)];
            } while (from == previousGap && options.length > 1);
            t[gap] = t[from];
            t[from] = 0;
            previousGap = gap;
            gap = from;
            made++;
        }
        return new LampSlidePuzzle(size, t, gap, made);
    }

    /** Number of random slides used to scramble a board of this size. */
    public static int walkLength(int size) {
        checkSize(size);
        switch (size) {
            case 3: return 30;
            case 4: return 60;
            default: return 100;
        }
    }

    /** The solved board, 1 to n*n-1 then 0. */
    public static int[] solvedTiles(int size) {
        checkSize(size);
        int n = size * size;
        int[] t = new int[n];
        for (int i = 0; i < n - 1; i++) t[i] = i + 1;
        t[n - 1] = 0;
        return t;
    }

    /** True when the board holds 1 to n*n-1 in order with the gap last. */
    public static boolean isSolvedArray(int[] t) {
        int n = t.length;
        for (int i = 0; i < n - 1; i++) {
            if (t[i] != i + 1) return false;
        }
        return t[n - 1] == 0;
    }

    /**
     * Standard solvability test for a sliding board. Odd sides: solvable when the inversion count
     * is even. Even sides: solvable when inversions plus the gap row from the top is odd.
     */
    public static boolean isSolvable(int size, int[] t) {
        checkSize(size);
        int inversions = 0;
        int gapRow = 0;
        for (int i = 0; i < t.length; i++) {
            if (t[i] == 0) {
                gapRow = i / size;
                continue;
            }
            for (int j = i + 1; j < t.length; j++) {
                if (t[j] != 0 && t[j] < t[i]) inversions++;
            }
        }
        if (size % 2 == 1) return inversions % 2 == 0;
        return (inversions + gapRow) % 2 == 1;
    }

    /**
     * Stars for a solved board. 3 when moves are at most par, 2 when at most 1.5 times par, else 1.
     * Each hint used lowers the cap by one star, never below one.
     */
    public static int stars(int moves, int par, int hintsUsed) {
        if (par < 1) throw new IllegalArgumentException("par must be at least 1");
        int base;
        if (moves <= par) base = 3;
        else if (moves * 2 <= par * 3) base = 2;
        else base = 1;
        int cap = Math.max(1, 3 - Math.max(0, hintsUsed));
        return Math.min(base, cap);
    }

    public int size() { return size; }

    /** Par: the slide count the board was scrambled with. Moves at or below par earn 3 stars. */
    public int par() { return par; }

    /** Slides made so far. Undo takes a slide back, so this is the length of the current path. */
    public int moves() { return history.size(); }

    public int gapIndex() { return gap; }

    /** Tile value at an index, 0 for the gap. */
    public int tileAt(int index) { return tiles[index]; }

    /** Copy of the board. */
    public int[] tiles() { return tiles.clone(); }

    public boolean solved() { return isSolvedArray(tiles); }

    /** Number of tiles that sit on their own square. The gap is not counted. */
    public int inPlace() {
        int count = 0;
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] != 0 && tiles[i] == i + 1) count++;
        }
        return count;
    }

    /** True when the tile at this index is next to the gap, so it can slide in. */
    public boolean canTap(int index) {
        if (index < 0 || index >= tiles.length || index == gap) return false;
        return adjacent(index, gap, size);
    }

    /** Slides the tile at index into the gap. Returns false and changes nothing when illegal. */
    public boolean tap(int index) {
        if (!canTap(index)) return false;
        slideInto(index);
        return true;
    }

    /**
     * Slides a tile in a direction into the gap. "UP" moves the tile below the gap up.
     * Returns false and changes nothing when no tile can move that way.
     */
    public boolean slide(Direction direction) {
        if (direction == null) return false;
        // The tile travels from the square behind the direction into the gap.
        int row = gap / size - direction.dRow;
        int col = gap % size - direction.dCol;
        if (row < 0 || row >= size || col < 0 || col >= size) return false;
        return tap(row * size + col);
    }

    /** Takes back the last slide. Returns false when there is nothing to undo. */
    public boolean undo() {
        if (history.isEmpty()) return false;
        int[] record = history.pop();
        int from = record[0];
        int to = record[1];
        // The tile now sits at "to" and the gap at "from". Slide it back.
        tiles[from] = tiles[to];
        tiles[to] = 0;
        gap = to;
        return true;
    }

    /**
     * A helpful tile for the "One move" hint: a tile next to the gap whose slide brings it closer
     * to its place. Returns -1 when the board is solved. Falls back to any legal tile.
     */
    public int helpfulTile() {
        if (solved()) return -1;
        int best = -1;
        int bestGain = 0;
        int fallback = -1;
        for (int n : neighbours(gap, size)) {
            if (fallback < 0) fallback = n;
            int value = tiles[n];
            int before = distance(value, n);
            int after = distance(value, gap);
            int gain = before - after;
            if (gain > bestGain) {
                bestGain = gain;
                best = n;
            }
        }
        return best >= 0 ? best : fallback;
    }

    private int distance(int value, int index) {
        int target = value - 1;
        return Math.abs(index / size - target / size) + Math.abs(index % size - target % size);
    }

    private void slideInto(int from) {
        int to = gap;
        tiles[to] = tiles[from];
        tiles[from] = 0;
        history.push(new int[] {from, to});
        gap = from;
    }

    private static void checkSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 3 and 5: " + size);
        }
    }

    private static int[] neighbours(int index, int size) {
        int row = index / size;
        int col = index % size;
        int[] out = new int[4];
        int count = 0;
        if (row > 0) out[count++] = index - size;
        if (row < size - 1) out[count++] = index + size;
        if (col > 0) out[count++] = index - 1;
        if (col < size - 1) out[count++] = index + 1;
        return java.util.Arrays.copyOf(out, count);
    }

    private static boolean adjacent(int a, int b, int size) {
        int dr = Math.abs(a / size - b / size);
        int dc = Math.abs(a % size - b % size);
        return dr + dc == 1;
    }
}
