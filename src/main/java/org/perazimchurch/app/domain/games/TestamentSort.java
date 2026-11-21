package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "Old or New?" sorting game: each round shows one Bible book and the player places it
 * in the Old or New Testament. Pure logic, no Android types, so it can be unit tested.
 */
public final class TestamentSort {
    public enum Testament { OLD, NEW }

    public static final class Book {
        public final String name;
        public final Testament testament;
        public final String fact;

        Book(String name, Testament testament, String fact) {
            this.name = name;
            this.testament = testament;
            this.fact = fact;
        }
    }

    /** Eight books per run, four from each testament, shuffled per seed. */
    private static final List<Book> BOOKS = Collections.unmodifiableList(Arrays.asList(
            new Book("Genesis", Testament.OLD, "The first book: creation, Abraham and Joseph."),
            new Book("Exodus", Testament.OLD, "Moses leads Israel out of Egypt."),
            new Book("Psalms", Testament.OLD, "A book of 150 songs and prayers."),
            new Book("Isaiah", Testament.OLD, "Many prophecies about the coming Messiah."),
            new Book("Daniel", Testament.OLD, "Daniel in the lions' den and Shadrach, Meshach and Abed-nego."),
            new Book("Jonah", Testament.OLD, "A prophet swallowed by a great fish."),
            new Book("Ruth", Testament.OLD, "A loyal daughter-in-law in Bethlehem."),
            new Book("Malachi", Testament.OLD, "The last book of the Old Testament."),
            new Book("Matthew", Testament.NEW, "The first Gospel, with the Sermon on the Mount."),
            new Book("Mark", Testament.NEW, "The shortest Gospel, full of action."),
            new Book("Luke", Testament.NEW, "The Gospel written by a physician."),
            new Book("John", Testament.NEW, "The Gospel that says 'In the beginning was the Word'."),
            new Book("Acts", Testament.NEW, "The early church, from Pentecost onward."),
            new Book("Romans", Testament.NEW, "Paul's letter on grace and faith."),
            new Book("Philippians", Testament.NEW, "Paul's letter of joy from prison."),
            new Book("Revelation", Testament.NEW, "The last book of the Bible.")));

    private final List<Book> order;
    private int index;
    private int score;
    private Testament lastGuess;

    public TestamentSort(long seed) {
        List<Book> pool = new ArrayList<>(BOOKS);
        Collections.shuffle(pool, new Random(seed));
        // Keep the run to eight books, balanced between the two testaments.
        List<Book> old = new ArrayList<>();
        List<Book> neu = new ArrayList<>();
        for (Book b : pool) {
            if (b.testament == Testament.OLD && old.size() < 4) old.add(b);
            if (b.testament == Testament.NEW && neu.size() < 4) neu.add(b);
        }
        order = new ArrayList<>();
        order.addAll(old);
        order.addAll(neu);
        Collections.shuffle(order, new Random(seed ^ 0x5DEECE66DL));
    }

    public Book current() { return finished() ? null : order.get(index); }
    public int position() { return Math.min(index + 1, order.size()); }
    public int size() { return order.size(); }
    public int score() { return score; }
    public boolean finished() { return index >= order.size(); }
    /** The testament the player chose for the most recent book, or null before any answer. */
    public Testament lastGuess() { return lastGuess; }
    public boolean lastCorrect() { return lastGuessMatched; }

    private boolean lastGuessMatched;

    /** Places the current book. Returns true when the placement is right, then advances. */
    public boolean place(Testament choice) {
        if (finished()) return false;
        Book book = order.get(index);
        boolean right = book.testament == choice;
        lastGuess = choice;
        lastGuessMatched = right;
        if (right) score++;
        index++;
        return right;
    }

    /** Book the player just placed, for the feedback line. Null before the first placement. */
    public Book lastBook() { return index == 0 ? null : order.get(index - 1); }
}
