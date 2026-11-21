package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pairs game: flip two tiles, find matching Bible symbols. Scripture references are for the reveal. */
public final class MemoryMatch {
    public static final class Symbol {
        public final String label;
        public final String reference;
        Symbol(String label, String reference) { this.label = label; this.reference = reference; }
    }

    public static final class Tile {
        public final Symbol symbol;
        public boolean faceUp;
        public boolean matched;
        Tile(Symbol symbol) { this.symbol = symbol; }
    }

    private static final Symbol[] SYMBOLS = {
        new Symbol("ARK", "Noah builds the ark: Genesis 6:14"),
        new Symbol("DOVE", "Noah sends out a dove: Genesis 8:8-12"),
        new Symbol("RAINBOW", "God sets a rainbow: Genesis 9:13"),
        new Symbol("RAM", "A ram caught in a thicket: Genesis 22:13"),
        new Symbol("MANNA", "Bread from heaven in the wilderness: Exodus 16:15"),
        new Symbol("STAFF", "Moses' staff becomes a serpent: Exodus 4:2-4"),
        new Symbol("FISH", "Jonah and the great fish: Jonah 1:17"),
        new Symbol("SLING", "David's sling and stone: 1 Samuel 17:50"),
        new Symbol("LAMP", "A lamp unto my feet: Psalm 119:105"),
    };

    private final List<Tile> tiles = new ArrayList<>();
    private int firstIndex = -1;
    private int secondIndex = -1;
    private int moves;

    /** Builds a deck of {@code pairs} matching pairs (at most 9), shuffled by seed. */
    public MemoryMatch(long seed, int pairs) {
        Random random = new Random(seed);
        List<Symbol> chosen = new ArrayList<>(List.of(SYMBOLS));
        Collections.shuffle(chosen, random);
        for (int i = 0; i < Math.min(pairs, SYMBOLS.length); i++) {
            tiles.add(new Tile(chosen.get(i)));
            tiles.add(new Tile(chosen.get(i)));
        }
        Collections.shuffle(tiles, random);
    }

    public List<Tile> tiles() { return Collections.unmodifiableList(tiles); }
    public int moves() { return moves; }
    public boolean complete() { for (Tile t : tiles) if (!t.matched) return false; return true; }
    /** True while two mismatched tiles are face up and the UI has not hidden them yet. */
    public boolean mismatchShowing() { return secondIndex >= 0; }

    /**
     * Turns a tile face up. Returns true when the flip was accepted. A pair counts as one move;
     * when it does not match, the caller must call {@link #hideMismatch()} after a short pause.
     */
    public boolean flip(int index) {
        if (index < 0 || index >= tiles.size() || mismatchShowing()) return false;
        Tile tile = tiles.get(index);
        if (tile.faceUp || tile.matched) return false;
        tile.faceUp = true;
        if (firstIndex < 0) { firstIndex = index; return true; }
        secondIndex = index;
        moves++;
        Tile first = tiles.get(firstIndex);
        if (first.symbol == tile.symbol) {
            first.matched = true;
            tile.matched = true;
            firstIndex = -1;
            secondIndex = -1;
        }
        return true;
    }

    /** Turns the two unmatched tiles back over. */
    public void hideMismatch() {
        if (!mismatchShowing()) return;
        tiles.get(firstIndex).faceUp = false;
        tiles.get(secondIndex).faceUp = false;
        firstIndex = -1;
        secondIndex = -1;
    }
}
