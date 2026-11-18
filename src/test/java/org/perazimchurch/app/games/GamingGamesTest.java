package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.MemoryMatch;
import org.perazimchurch.app.domain.games.TimelineSort;
import org.perazimchurch.app.domain.games.WordSearch;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GamingGamesTest {
    @Test public void wordSearchPlacesEveryWordAndSelectionFindsIt() {
        WordSearch ws = new WordSearch(42);
        assertEquals(WordSearch.WORDS_PER_PUZZLE, ws.words().size());
        Set<String> unique = new HashSet<>(ws.words());
        assertEquals(ws.words().size(), unique.size());
        assertFalse(ws.complete());
        for (String word : ws.words()) {
            boolean found = false;
            for (int r = 0; r < WordSearch.SIZE && !found; r++)
                for (int c = 0; c < WordSearch.SIZE && !found; c++)
                    for (int r2 = 0; r2 < WordSearch.SIZE && !found; r2++)
                        for (int c2 = 0; c2 < WordSearch.SIZE && !found; c2++)
                            if (ws.select(r, c, r2, c2) != null) found = true;
            assertTrue("word not reachable: " + word, found);
        }
        assertTrue(ws.complete());
    }

    @Test public void wordSearchRejectsBentSelection() {
        WordSearch ws = new WordSearch(1);
        assertNull(ws.select(0, 0, 1, 2));
    }

    @Test public void memoryMatchNeedsTwoTilesAndCountsMoves() {
        MemoryMatch mm = new MemoryMatch(5, 6);
        assertEquals(12, mm.tiles().size());
        assertFalse(mm.flip(-1));
        assertTrue(mm.flip(0));
        assertFalse(mm.flip(0));
        assertEquals(0, mm.moves());
        int partner = -1;
        for (int i = 1; i < mm.tiles().size(); i++) {
            if (mm.tiles().get(i).symbol == mm.tiles().get(0).symbol) { partner = i; break; }
        }
        assertTrue(mm.flip(partner));
        assertEquals(1, mm.moves());
        assertTrue(mm.tiles().get(0).matched);
    }

    @Test public void memoryMatchMismatchIsHiddenOnlyWhenCallerResolves() {
        MemoryMatch mm = new MemoryMatch(9, 6);
        int a = 0, b = 1;
        while (mm.tiles().get(a).symbol == mm.tiles().get(b).symbol) b++;
        mm.flip(a);
        mm.flip(b);
        assertTrue(mm.mismatchShowing());
        assertFalse(mm.flip(2));
        mm.hideMismatch();
        assertFalse(mm.mismatchShowing());
        assertFalse(mm.tiles().get(a).faceUp);
    }

    @Test public void timelineAcceptsOnlyStoryOrder() {
        TimelineSort t = new TimelineSort(11, 6);
        assertEquals(6, t.ordered().size());
        int wrong = -1;
        for (int i = 0; i < t.shuffled().size(); i++) {
            if (t.shuffled().get(i) != t.ordered().get(0)) { wrong = i; break; }
        }
        assertFalse(t.tap(wrong));
        assertEquals(1, t.mistakes());
        for (int step = 0; step < 6; step++) {
            int idx = -1;
            for (int i = 0; i < t.shuffled().size(); i++) {
                if (t.shuffled().get(i) == t.ordered().get(step)) { idx = i; break; }
            }
            assertTrue(t.tap(idx));
        }
        assertTrue(t.complete());
    }
}
