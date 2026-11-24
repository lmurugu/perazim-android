package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "Who said it?" quiz: the player reads a short KJV quotation and picks the speaker from four
 * names. Pure logic, no Android types, so it can be unit tested. Offline, like the other games.
 */
public final class WhoSaidIt {

    public static final class Question {
        public final String quote;
        public final String reference;
        public final String answer;
        public final List<String> choices;

        Question(String quote, String reference, String answer, List<String> choices) {
            this.quote = quote;
            this.reference = reference;
            this.answer = answer;
            this.choices = Collections.unmodifiableList(choices);
        }
    }

    /** One quotation with its speaker and reference, from the built-in list or a bank file. */
    public static final class Quote {
        public final String text;
        public final String reference;
        public final String speaker;

        public Quote(String text, String reference, String speaker) {
            this.text = text;
            this.reference = reference;
            this.speaker = speaker;
        }
    }

    /** Verbatim King James wording. Each speaker is only used once in a run. */
    private static final List<Quote> QUOTES = Collections.unmodifiableList(Arrays.asList(
            new Quote("Let there be light.", "Genesis 1:3", "God"),
            new Quote("Am I my brother's keeper?", "Genesis 4:9", "Cain"),
            new Quote("Let my people go.", "Exodus 5:1", "Moses"),
            new Quote("Here am I; for thou calledst me.", "1 Samuel 3:5", "Samuel"),
            new Quote("Lord, save me.", "Matthew 14:30", "Peter"),
            new Quote("Give me this water, that I thirst not.", "John 4:15", "The Samaritan woman"),
            new Quote("Thy people shall be my people, and thy God my God.", "Ruth 1:16", "Ruth"),
            new Quote("Not my will, but thine, be done.", "Luke 22:42", "Jesus"),
            new Quote("Lord, that I might receive my sight.", "Mark 10:51", "Bartimaeus")));

    /** Names used only as wrong choices. */
    private static final List<String> DISTRACTORS = Collections.unmodifiableList(Arrays.asList(
            "Abraham", "Noah", "Joseph", "Eli", "Naomi", "Thomas", "Zacchaeus", "Mary", "Elijah",
            "Lazarus", "Nicodemus", "Jacob"));

    /** Five quotations per run. */
    public static final int ROUND_SIZE = 5;

    private final List<Question> questions = new ArrayList<>();
    private int index;
    private int score;
    private Question last;

    public WhoSaidIt(long seed) {
        this(QUOTES, DISTRACTORS, seed);
    }

    /** A run drawn from the given quotations, with wrong speakers taken from {@code distractors}. */
    public WhoSaidIt(List<Quote> source, List<String> distractors, long seed) {
        List<Quote> pool = new ArrayList<>(source);
        Collections.shuffle(pool, new Random(seed));
        Random choiceRandom = new Random(seed ^ 0x2545F4914F6CDD1DL);
        for (Quote quote : pool.subList(0, Math.min(ROUND_SIZE, pool.size()))) {
            List<String> wrong = new ArrayList<>(distractors);
            Collections.shuffle(wrong, choiceRandom);
            List<String> choices = new ArrayList<>();
            choices.add(quote.speaker);
            for (String name : wrong) {
                if (choices.size() == 4) break;
                if (!name.equals(quote.speaker)) choices.add(name);
            }
            Collections.shuffle(choices, choiceRandom);
            questions.add(new Question(quote.text, quote.reference, quote.speaker, choices));
        }
    }

    public Question current() { return finished() ? null : questions.get(index); }
    public int position() { return Math.min(index + 1, questions.size()); }
    public int size() { return questions.size(); }
    public int score() { return score; }
    public boolean finished() { return index >= questions.size(); }

    /** Records the player's choice for the current quotation. Returns true when it is right, then advances. */
    public boolean answer(String choice) {
        if (finished()) return false;
        last = questions.get(index);
        boolean right = last.answer.equals(choice);
        if (right) score++;
        index++;
        return right;
    }

    /** The quotation answered most recently, for the feedback line. Null before the first answer. */
    public Question lastQuestion() { return last; }
}
