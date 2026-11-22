package org.perazimchurch.app.presentation.ui.games.lampslide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class LampSlideScenesTest {

    @Test
    public void bundledSceneFileParsesWithVersesAndReferences() throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/games/lamp_slide.json")),
                StandardCharsets.UTF_8);
        List<LampSlideScene> scenes = LampSlideScenes.parse(json);
        assertTrue(scenes.size() >= 12);
        for (LampSlideScene scene : scenes) {
            assertFalse(scene.verse().isEmpty());
            assertFalse(scene.reference().isEmpty());
            assertFalse(scene.image().isEmpty());
        }
    }

    @Test
    public void missingPrayerParsesAsEmpty() throws Exception {
        String json = "{\"scenes\":[{\"id\":\"p\",\"image\":\"game_art_quiz\",\"reference\":\"Psalm 23:1\","
                + "\"verse\":\"The LORD is my shepherd; I shall not want.\"}]}";
        List<LampSlideScene> scenes = LampSlideScenes.parse(json);
        assertEquals(1, scenes.size());
        assertEquals("", scenes.get(0).prayer());
    }
}
