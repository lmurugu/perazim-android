package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Story Timeline: tap the events in the order they happen in the Bible narrative.
 * This is the order of the stories as told in the text, not a historical dating claim.
 */
public final class TimelineSort {
    public static final class Event {
        public final String title;
        public final String reference;
        Event(String title, String reference) { this.title = title; this.reference = reference; }
    }

    // Listed in the order the stories appear in Genesis to 1 Samuel.
    private static final Event[] STORY = {
        new Event("God creates the world", "Genesis 1"),
        new Event("The flood in Noah's day", "Genesis 6-9"),
        new Event("The tower of Babel", "Genesis 11:1-9"),
        new Event("God calls Abram", "Genesis 12:1-3"),
        new Event("Joseph is sold into Egypt", "Genesis 37:28"),
        new Event("God speaks from the burning bush", "Exodus 3:2-4"),
        new Event("The Red Sea opens", "Exodus 14:21-22"),
        new Event("The walls of Jericho fall", "Joshua 6:20"),
        new Event("David defeats Goliath", "1 Samuel 17:50"),
    };

    private final List<Event> chosen = new ArrayList<>();
    private final List<Event> shuffled = new ArrayList<>();
    private int next;
    private int mistakes;

    /** Picks {@code count} events (at most 9) in story order, then shuffles them by seed. */
    public TimelineSort(long seed, int count) {
        Random random = new Random(seed);
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < STORY.length; i++) indexes.add(i);
        Collections.shuffle(indexes, random);
        List<Integer> picked = new ArrayList<>(indexes.subList(0, Math.min(count, STORY.length)));
        Collections.sort(picked);
        for (int i : picked) chosen.add(STORY[i]);
        shuffled.addAll(chosen);
        Collections.shuffle(shuffled, random);
    }

    public List<Event> shuffled() { return Collections.unmodifiableList(shuffled); }
    /** The events in correct order, for the review at the end. */
    public List<Event> ordered() { return Collections.unmodifiableList(chosen); }
    public int mistakes() { return mistakes; }
    public int placed() { return next; }
    public boolean complete() { return next == chosen.size(); }

    /** Taps the event at {@code shuffledIndex}. Returns true when it is the next event in order. */
    public boolean tap(int shuffledIndex) {
        if (complete() || shuffledIndex < 0 || shuffledIndex >= shuffled.size()) return false;
        Event event = shuffled.get(shuffledIndex);
        if (event == chosen.get(next)) {
            next++;
            return true;
        }
        mistakes++;
        return false;
    }

    public boolean isPlaced(int shuffledIndex) {
        Event event = shuffled.get(shuffledIndex);
        return chosen.indexOf(event) < next;
    }
}
