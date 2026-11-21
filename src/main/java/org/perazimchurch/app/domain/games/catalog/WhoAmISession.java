package org.perazimchurch.app.domain.games.catalog;

import java.util.Collections;
import java.util.List;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * One run of Who am I?. Each person is asked once. A correct answer scores a base amount plus a
 * combo bonus for consecutive correct answers within this run only. A hint costs a few points on
 * that question. Wrong answers reset the combo and show the explanation and reference gently.
 */
final class WhoAmISession implements GameSession {

    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final List<WhoAmIBank.Person> people;
    private final WhoAmIDefinition.Tier tier;
    private int index;
    private int score;
    private int combo;
    private boolean hintShown;
    private String lastFeedback;

    WhoAmISession(List<WhoAmIBank.Person> people, WhoAmIDefinition.Tier tier) {
        this.people = people;
        this.tier = tier;
    }

    @Override public int size() { return people.size(); }
    @Override public int position() { return Math.min(index + 1, size()); }
    @Override public boolean finished() { return index >= people.size(); }
    @Override public int score() { return score; }
    @Override public int lives() { return NO_LIVES; }

    @Override
    public boolean hintAvailable() {
        return !finished() && tier != WhoAmIDefinition.Tier.HARD && !hintShown;
    }

    @Override
    public String hint() {
        if (!hintAvailable()) return null;
        hintShown = true;
        return current().hint;
    }

    /** Clues for the current person, most general first. Empty when the run is finished. */
    public List<String> clues() {
        return finished() ? Collections.emptyList() : current().clues;
    }

    /** Explanation and reference for the last answered person, or null before the first answer. */
    public String lastFeedback() { return lastFeedback; }

    @Override
    public Outcome answer(String value) {
        if (finished()) return Outcome.IGNORED;
        WhoAmIBank.Person person = current();
        boolean right = matches(person, value);
        if (right) {
            combo++;
            int bonus = (Math.min(combo - 1, COMBO_CAP)) * COMBO_STEP;
            int points = BASE_POINTS + bonus - (hintShown ? HINT_COST : 0);
            score += Math.max(1, points);
        } else {
            combo = 0;
        }
        lastFeedback = person.answer + " (" + person.reference + "): " + person.explanation;
        index++;
        hintShown = false;
        return right ? Outcome.CORRECT : Outcome.WRONG;
    }

    /** Current combo streak within this run. */
    int combo() { return combo; }

    /** The expected name for the current person, for tests in this package only. */
    String currentAnswerForTest() { return current().answer; }

    private WhoAmIBank.Person current() { return people.get(index); }

    private static boolean matches(WhoAmIBank.Person person, String value) {
        String given = BibleGames.normalize(value);
        if (given.isEmpty()) return false;
        if (given.equals(BibleGames.normalize(person.answer))) return true;
        for (String alias : person.aliases) {
            if (given.equals(BibleGames.normalize(alias))) return true;
        }
        return false;
    }
}
