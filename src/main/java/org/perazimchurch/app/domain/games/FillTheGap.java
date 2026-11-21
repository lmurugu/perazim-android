package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "Fill the gap" quiz: a well-known King James verse with one word missing. The player picks
 * the missing word from four choices. Pure logic, no Android types, offline like the other games.
 */
public final class FillTheGap {

    public static final class Question {
        public final String before;
        public final String after;
        public final String reference;
        public final String answer;
        public final List<String> choices;

        Question(String before, String after, String reference, String answer, List<String> choices) {
            this.before = before;
            this.after = after;
            this.reference = reference;
            this.answer = answer;
            this.choices = Collections.unmodifiableList(choices);
        }
    }

    /** One verse with the missing word; {@code wrong} holds the other choices. */
    public static final class Verse {
        public final String before;
        public final String answer;
        public final String after;
        public final String reference;
        public final List<String> wrong;

        public Verse(String before, String answer, String after, String reference, String... wrong) {
            this.before = before;
            this.answer = answer;
            this.after = after;
            this.reference = reference;
            this.wrong = Arrays.asList(wrong);
        }
    }

    /** Verbatim King James wording with the answer word removed from the text. */
    private static final List<Verse> VERSES = Collections.unmodifiableList(Arrays.asList(
            new Verse("The LORD is my ", "shepherd", "; I shall not want.", "Psalm 23:1", "king", "friend", "father"),
            new Verse("In the beginning God ", "created", " the heaven and the earth.", "Genesis 1:1", "made", "formed", "built"),
            new Verse("For God so ", "loved", " the world, that he gave his only begotten Son.", "John 3:16", "saw", "blessed", "made"),
            new Verse("Trust in the LORD with all thine heart; and lean not unto thine own ", "understanding", ".", "Proverbs 3:5", "wisdom", "strength", "knowledge"),
            new Verse("I can do all things through Christ which ", "strengtheneth", " me.", "Philippians 4:13", "helpeth", "guideth", "comforteth"),
            new Verse("The Lord is my light and my salvation; whom shall I ", "fear", "?", "Psalm 27:1", "trust", "seek", "follow"),
            new Verse("Be strong and of a good ", "courage", ".", "Joshua 1:9", "faith", "hope", "joy"),
            new Verse("Thy word is a ", "lamp", " unto my feet, and a light unto my path.", "Psalm 119:105", "candle", "torch", "star")));

    /** Five verses per run. */
    public static final int ROUND_SIZE = 5;

    private final List<Question> questions = new ArrayList<>();
    private int index;
    private int score;
    private Question last;

    public FillTheGap(long seed) {
        this(VERSES, seed);
    }

    /** A run drawn from the given verses. */
    public FillTheGap(List<Verse> source, long seed) {
        List<Verse> pool = new ArrayList<>(source);
        Collections.shuffle(pool, new Random(seed));
        Random choiceRandom = new Random(seed ^ 0x9E3779B97F4A7C15L);
        for (Verse verse : pool.subList(0, Math.min(ROUND_SIZE, pool.size()))) {
            List<String> choices = new ArrayList<>(verse.wrong);
            choices.add(verse.answer);
            Collections.shuffle(choices, choiceRandom);
            questions.add(new Question(verse.before, verse.after, verse.reference, verse.answer, choices));
        }
    }

    public Question current() { return finished() ? null : questions.get(index); }
    public int position() { return Math.min(index + 1, questions.size()); }
    public int size() { return questions.size(); }
    public int score() { return score; }
    public boolean finished() { return index >= questions.size(); }

    /** Records the player's word for the current verse. Returns true when it is right, then advances. */
    public boolean answer(String choice) {
        if (finished()) return false;
        last = questions.get(index);
        boolean right = last.answer.equals(choice);
        if (right) score++;
        index++;
        return right;
    }

    /** The verse answered most recently, for the feedback line. Null before the first answer. */
    public Question lastQuestion() { return last; }
}
