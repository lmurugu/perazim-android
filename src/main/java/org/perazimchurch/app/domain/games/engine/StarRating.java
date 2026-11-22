package org.perazimchurch.app.domain.games.engine;

/**
 * Turns a finished run into 1 to 3 stars. Pure logic, offline, never uploaded.
 *
 * <ul>
 *   <li>3 stars: every answer right.</li>
 *   <li>2 stars: at least 60 percent right.</li>
 *   <li>1 star: finished the run with less.</li>
 * </ul>
 * A run that used lives and ended with none left is capped at 2 stars, so a lost run never shows 3.
 */
public final class StarRating {

    /** Share of correct answers needed for two stars. */
    static final double TWO_STAR_SHARE = 0.6;

    private StarRating() {}

    /**
     * @param score    correct answers in the run
     * @param size     questions in the run, at least 1
     * @param lives    lives left, or {@link GameSession#NO_LIVES}
     */
    public static int stars(int score, int size, int lives) {
        if (size < 1) throw new IllegalArgumentException("size must be at least 1");
        int clamped = Math.max(0, Math.min(score, size));
        int stars;
        if (clamped == size) {
            stars = 3;
        } else if (clamped >= size * TWO_STAR_SHARE) {
            stars = 2;
        } else {
            stars = 1;
        }
        if (lives == 0 && stars > 2) stars = 2;
        return stars;
    }
}
