package org.perazimchurch.app.ai;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Runs the synthetic off-topic and in-scope corpora through the model-path gate on the emulator. No model is loaded. */
public class OfflineScopeDeviceTest {

    private static List<String> load(String name) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("offline_scope/" + name),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (!t.isEmpty()) lines.add(t);
            }
        }
        return lines;
    }

    @Test
    public void offTopicCorpusIsRefusedOnDevice() throws Exception {
        List<String> prompts = load("must_refuse.txt");
        List<String> misses = new ArrayList<>();
        for (String p : prompts) if (!PerazimAiEngine.isOutOfScopeForModel(p)) misses.add(p);
        assertTrue(prompts.size() >= 150);
        if (!misses.isEmpty()) fail("Not refused on device (" + misses.size() + "/" + prompts.size() + "): " + misses);
    }

    @Test
    public void inScopeCorpusIsNeverRefusedOnDevice() throws Exception {
        List<String> prompts = load("must_pass.txt");
        List<String> wrong = new ArrayList<>();
        for (String p : prompts) if (PerazimAiEngine.isOutOfScopeForModel(p)) wrong.add(p);
        assertTrue(prompts.size() >= 100);
        if (!wrong.isEmpty()) fail("Wrongly refused on device (" + wrong.size() + "/" + prompts.size() + "): " + wrong);
    }
}
