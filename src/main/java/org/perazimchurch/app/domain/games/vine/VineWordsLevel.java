package org.perazimchurch.app.domain.games.vine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One Vine Words level, as stored in {@code assets/games/vine_words.json}. Built by
 * {@code scripts/games/content/build_vine_words.py}; this class only reads it.
 */
public final class VineWordsLevel {

    /** A KJV verse shown after the level is solved. */
    public static final class Verse {
        public final String reference;
        public final String text;

        public Verse(String reference, String text) {
            this.reference = reference;
            this.text = text;
        }
    }

    private final int number;
    private final int band;
    private final String passage;
    private final long seed;
    private final List<Verse> verses;
    private final CrosswordGrid grid;
    private final String wheel;
    private final String bonus;

    public VineWordsLevel(int number, int band, String passage, long seed, List<Verse> verses,
                          CrosswordGrid grid, String wheel, String bonus) {
        this.number = number;
        this.band = band;
        this.passage = passage;
        this.seed = seed;
        this.verses = Collections.unmodifiableList(new ArrayList<>(verses));
        this.grid = grid;
        this.wheel = wheel;
        this.bonus = bonus;
    }

    /** 1-based level number. */
    public int number() { return number; }

    /** Difficulty band: 1 (short words), 2 or 3. */
    public int band() { return band; }

    /** Passage reference, for example "John 15:1-3". */
    public String passage() { return passage; }

    /** Build seed recorded by the content script. */
    public long seed() { return seed; }

    public List<Verse> verses() { return verses; }

    public CrosswordGrid grid() { return grid; }

    /** Wheel letters in alphabetical order. Use {@link #wheelOrder(long)} for what the player sees. */
    public String wheel() { return wheel; }

    /** Optional bonus word that can be spelled from the wheel, or null. */
    public String bonus() { return bonus; }

    /** The target words, in the order they are listed in the level file. */
    public List<String> targetWords() {
        List<String> words = new ArrayList<>();
        for (CrosswordGrid.Slot slot : grid.slots()) words.add(slot.word);
        return words;
    }

    /**
     * The wheel letters in a shuffled order. The same seed always gives the same order. The
     * order is never alphabetical, so a seed that happens to sort the letters is shuffled again.
     */
    public List<Character> wheelOrder(long shuffleSeed) {
        List<Character> letters = new ArrayList<>();
        for (int i = 0; i < wheel.length(); i++) letters.add(wheel.charAt(i));
        Random random = new Random(shuffleSeed);
        for (int attempt = 0; attempt < 8; attempt++) {
            Collections.shuffle(letters, random);
            if (!isSorted(letters) || letters.size() < 2) break;
        }
        return letters;
    }

    private static boolean isSorted(List<Character> letters) {
        for (int i = 1; i < letters.size(); i++) {
            if (letters.get(i - 1) > letters.get(i)) return false;
        }
        return true;
    }
}
