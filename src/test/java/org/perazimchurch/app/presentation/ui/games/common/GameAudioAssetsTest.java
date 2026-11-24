package org.perazimchurch.app.presentation.ui.games.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/** Checks that every sound the engine can ask for is bundled, and that each game maps to its own track. */
public class GameAudioAssetsTest {

    private static final File AUDIO_DIR = new File("src/main/assets/games/audio");

    @Test
    public void everyEffectAssetIsBundled() {
        for (GameAudio.Sfx sfx : GameAudio.Sfx.values()) {
            assertTrue("missing " + sfx.asset, new File(AUDIO_DIR, sfx.asset + ".ogg").isFile());
        }
    }

    @Test
    public void everyGameTrackIsBundled() {
        Set<String> tracks = new HashSet<>();
        String[] games = {"bible_quiz", "who_am_i", "verse_builder", "ladder_of_faith", "word_search",
                "memory_match", "story_timeline", "road_to_jerusalem", "old_or_new", "who_said_it",
                "fill_the_gap", "vine_words", "lamp_slide", "story_mosaic"};
        for (String game : games) {
            String track = GameAudio.trackFor(game);
            assertTrue("missing track for " + game, new File(AUDIO_DIR, "music_" + track + ".ogg").isFile());
            tracks.add(track);
        }
        assertEquals("each game has its own track", games.length, tracks.size());
        assertTrue(new File(AUDIO_DIR, "music_" + GameAudio.HUB_TRACK + ".ogg").isFile());
    }

    @Test
    public void unknownGameFallsBackToHubTrack() {
        assertEquals(GameAudio.HUB_TRACK, GameAudio.trackFor("not_a_game"));
        assertEquals(GameAudio.HUB_TRACK, GameAudio.trackFor(null));
    }

    @Test
    public void audioBudgetStaysUnderEightMegabytes() {
        long total = 0;
        File[] files = AUDIO_DIR.listFiles();
        assertTrue(files != null && files.length > 0);
        for (File f : files) total += f.length();
        assertTrue("audio is " + total + " bytes", total < 8L * 1024 * 1024);
    }
}
