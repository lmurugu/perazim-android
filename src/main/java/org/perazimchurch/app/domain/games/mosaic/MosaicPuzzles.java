package org.perazimchurch.app.domain.games.mosaic;

import org.perazimchurch.app.domain.games.catalog.StoryChapterBank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every Story Mosaic puzzle, in story order. Each event of each shared chapter is one puzzle. All
 * puzzles of a chapter use one scene, {@code story_<chapter>.webp}; the grid size changes with the
 * level, so each event is a different puzzle on the same picture. Built from
 * {@link StoryChapterBank} so Story Timeline and Story Mosaic never disagree about the story.
 */
public final class MosaicPuzzles {

    /** One puzzle: a scene and the story line it reveals. */
    public static final class Puzzle {
        public final String chapterId;
        public final String chapterTitle;
        public final int number;
        public final String title;
        public final String reference;

        Puzzle(String chapterId, String chapterTitle, int number, String title, String reference) {
            this.chapterId = chapterId;
            this.chapterTitle = chapterTitle;
            this.number = number;
            this.title = title;
            this.reference = reference;
        }

        /** Art file name for this chapter's scene, for example story_c01.webp. */
        public String assetName() {
            return MosaicPuzzles.assetName(chapterId);
        }
    }

    private static final List<Puzzle> ALL = build();

    private MosaicPuzzles() {}

    public static String assetName(String chapterId) {
        return "story_" + chapterId + ".webp";
    }

    /** Every puzzle in story order. Unmodifiable. */
    public static List<Puzzle> all() {
        return ALL;
    }

    /** Puzzle at a 0-based index into {@link #all()}. */
    public static Puzzle at(int index) {
        return ALL.get(index);
    }

    /** Index of the first puzzle of each chapter, in chapter order. */
    public static List<Integer> chapterStarts() {
        List<Integer> starts = new ArrayList<>();
        String last = null;
        for (int i = 0; i < ALL.size(); i++) {
            if (!ALL.get(i).chapterId.equals(last)) {
                starts.add(i);
                last = ALL.get(i).chapterId;
            }
        }
        return Collections.unmodifiableList(starts);
    }

    private static List<Puzzle> build() {
        List<Puzzle> puzzles = new ArrayList<>();
        for (StoryChapterBank.Chapter chapter : StoryChapterBank.CHAPTERS) {
            for (int i = 0; i < chapter.events.size(); i++) {
                StoryChapterBank.Event event = chapter.events.get(i);
                puzzles.add(new Puzzle(chapter.id, chapter.title, i + 1, event.title, event.reference));
            }
        }
        return Collections.unmodifiableList(puzzles);
    }
}
