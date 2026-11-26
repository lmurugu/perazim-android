package org.perazimchurch.app.domain.games;

import java.util.Random;

/**
 * Road to Jerusalem: a race of 20 squares played against the phone. Each roll is a die from 1 to 6.
 * Some squares carry an encouraging or a setback note that moves the traveller forward or back.
 */
public final class RoadToJerusalem {
    public static final int FINISH = 20;

    public static final class Move {
        public final boolean player;
        public final int roll;
        public final int from;
        public final int to;
        public final String note;
        Move(boolean player, int roll, int from, int to, String note) {
            this.player = player;
            this.roll = roll;
            this.from = from;
            this.to = to;
            this.note = note;
        }
    }

    // Square -> step to take after landing there (positive is encouragement, negative a setback).
    private static final int[] EFFECT = new int[FINISH + 1];
    private static final String[] NOTE = new String[FINISH + 1];
    static {
        put(3, 2, "A friend walks with you. Move forward 2.");
        put(6, 0, "A quiet moment of prayer. Take a breath.");
        put(8, -2, "Worry slows the journey. Back 2.");
        put(11, 3, "Someone shares a meal with you. Forward 3.");
        put(14, 2, "Forgiveness lightens the load. Forward 2.");
        put(17, -3, "Doubt makes the road feel long. Back 3.");
        put(19, 1, "Giving thanks along the way. Forward 1.");
    }

    private static void put(int square, int effect, String note) {
        EFFECT[square] = effect;
        NOTE[square] = note;
    }

    private final Random random;
    private final int finish;
    private int playerAt;
    private int computerAt;

    public RoadToJerusalem(long seed) { this(seed, 0); }

    public RoadToJerusalem(long seed, int band) {
        random = new Random(seed);
        finish = FINISH + 5 * Math.max(0, Math.min(2, band));
        computerAt = band >= 2 ? 2 : 0;
    }

    public int finish() { return finish; }

    public int effect(int square) {
        if (square < 0 || square >= finish) return 0;
        if (square <= FINISH) return EFFECT[square];
        if (square == 22 || square == 27) return -3;
        return square == 24 || square == 28 ? 2 : 0;
    }

    private String note(int square) {
        if (square >= finish || square < 0) return null;
        if (square <= FINISH) return NOTE[square];
        if (effect(square) < 0) return "A steep stretch slows you down. Back 3.";
        if (effect(square) > 0) return "Travellers share a shortcut. Forward 2.";
        return null;
    }

    public int playerAt() { return playerAt; }
    public int computerAt() { return computerAt; }
    public boolean finished() { return playerAt >= finish || computerAt >= finish; }
    /** "You" or "Computer" once someone reaches Jerusalem, otherwise null. */
    public String winner() {
        if (playerAt >= finish) return "You";
        if (computerAt >= finish) return "Computer";
        return null;
    }

    /** Rolls for the player or the computer, moves that token, and applies any square note. */
    public Move turn(boolean player) {
        if (finished()) return null;
        int roll = 1 + random.nextInt(6);
        int from = player ? playerAt : computerAt;
        int landing = Math.min(from + roll, finish);
        String note = note(landing);
        int to = Math.max(0, Math.min(finish, landing + effect(landing)));
        if (player) playerAt = to; else computerAt = to;
        return new Move(player, roll, from, to, note);
    }

    /** True when a square carries a note, so the screen can highlight it. */
    public boolean hasNote(int square) { return note(square) != null; }
}
