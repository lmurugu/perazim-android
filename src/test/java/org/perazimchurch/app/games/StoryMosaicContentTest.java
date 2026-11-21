package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.catalog.StoryChapterBank;
import org.perazimchurch.app.domain.games.catalog.StoryMosaicDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.mosaic.MosaicPuzzles;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StoryMosaicContentTest {

    private static final Pattern REFERENCE = Pattern.compile("^([1-3] )?[A-Za-z]+( [A-Za-z]+)* \\d+:\\d+$");
    private static final Pattern ASSET = Pattern.compile("^story_c\\d{2}\\.webp$");

    @Test public void atLeastEightChaptersAreUsed() {
        Set<String> chapters = new HashSet<>();
        for (MosaicPuzzles.Puzzle p : MosaicPuzzles.all()) chapters.add(p.chapterId);
        assertTrue("chapters: " + chapters.size(), chapters.size() >= 8);
    }

    @Test public void everyChapterHasFourToSixPuzzles() {
        for (StoryChapterBank.Chapter chapter : StoryChapterBank.CHAPTERS) {
            int count = chapter.events.size();
            assertTrue(chapter.id + " has " + count + " puzzles", count >= 4 && count <= 6);
        }
    }

    @Test public void puzzlesCountMatchTheBankAndAreInStoryOrder() {
        int expected = 0;
        for (StoryChapterBank.Chapter c : StoryChapterBank.CHAPTERS) expected += c.events.size();
        assertEquals(expected, MosaicPuzzles.all().size());
        assertEquals(expected, new StoryMosaicDefinition(0).levelCount());
    }

    @Test public void everyReferenceIsWellFormed() {
        for (MosaicPuzzles.Puzzle p : MosaicPuzzles.all()) {
            assertTrue("bad reference: " + p.reference, REFERENCE.matcher(p.reference).matches());
            assertFalse(p.title.trim().isEmpty());
        }
    }

    @Test public void oneSceneFilePerChapterFollowsTheArtConvention() {
        Set<String> names = new HashSet<>();
        for (StoryChapterBank.Chapter c : StoryChapterBank.CHAPTERS) {
            String name = MosaicPuzzles.assetName(c.id);
            assertTrue(name, ASSET.matcher(name).matches());
            assertTrue("duplicate scene " + name, names.add(name));
        }
        assertEquals(StoryChapterBank.CHAPTERS.size(), names.size());
    }

    @Test public void allPuzzlesOfAChapterShareItsScene() {
        for (MosaicPuzzles.Puzzle p : MosaicPuzzles.all()) {
            assertEquals(MosaicPuzzles.assetName(p.chapterId), p.assetName());
        }
    }

    @Test public void chapterStartsPointAtFirstPuzzleOfEachChapter() {
        int chapters = StoryChapterBank.CHAPTERS.size();
        assertEquals(chapters, MosaicPuzzles.chapterStarts().size());
        for (int start : MosaicPuzzles.chapterStarts()) {
            String id = MosaicPuzzles.at(start).chapterId;
            assertEquals(1, MosaicPuzzles.at(start).number);
            if (start > 0) assertNotEquals(id, MosaicPuzzles.at(start - 1).chapterId);
        }
    }

    @Test public void everyLevelBuildsAPlayableSession() {
        StoryMosaicDefinition game = new StoryMosaicDefinition(0);
        for (int level = 1; level <= game.levelCount(); level++) {
            GameSession session = game.createSession(level, level);
            assertTrue(session.size() >= 9);
            assertFalse(session.finished());
            assertEquals(GameSession.NO_LIVES, session.lives());
        }
    }

    @Test public void guidedLevelsOfferHintsAndQuietLevelsDoNot() {
        StoryMosaicDefinition game = new StoryMosaicDefinition(0);
        assertTrue(game.createSession(1, 5L).hintAvailable());
        assertNotNull(game.createSession(1, 5L).hint());
        assertFalse(game.createSession(StoryMosaicDefinition.GUIDED_LEVELS + 1, 5L).hintAvailable());
    }

    @Test public void completingEveryPieceFinishesTheSession() {
        StoryMosaicDefinition game = new StoryMosaicDefinition(0);
        GameSession session = game.createSession(1, 9L);
        for (int p = 0; p < session.size(); p++) {
            assertEquals(GameSession.Outcome.CORRECT, session.answer(p + ":" + p));
        }
        assertTrue(session.finished());
        assertEquals(session.size(), session.score());
    }
}
