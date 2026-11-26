package org.perazimchurch.app.ai;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Host-side harness for the offline assistant's scope gate. Runs on the laptop with no emulator or phone.
 * Corpora are synthetic, one prompt per line, in src/test/resources/offline_scope/:
 * - must_refuse.txt: off-topic prompts (English, Swahili, Sheng, role-play, mixed) the deterministic gate must refuse.
 * - must_pass.txt: church, Bible and app prompts the gate must never refuse.
 * Anything the gate misses is still refused at runtime, because the on-device path requires verified
 * approved-source evidence before generation.
 */
public class OfflineScopeHarnessTest {

    @Test
    public void offTopicCorpusIsRefusedByDeterministicGate() throws IOException {
        List<String> prompts = load("offline_scope/must_refuse.txt");
        List<String> misses = new ArrayList<>();
        for (String prompt : prompts) {
            if (!PerazimAiEngine.isOutOfScopeForModel(prompt)) misses.add(prompt);
        }
        System.out.println("OFFSCOPE refuse corpus=" + prompts.size() + " missed=" + misses.size());
        if (!misses.isEmpty()) {
            fail("Off-topic prompts not refused by the gate (" + misses.size() + "/" + prompts.size() + "): " + misses);
        }
    }

    @Test
    public void inScopeCorpusIsNeverRefusedByDeterministicGate() throws IOException {
        List<String> prompts = load("offline_scope/must_pass.txt");
        List<String> wrongRefusals = new ArrayList<>();
        for (String prompt : prompts) {
            if (PerazimAiEngine.isOutOfScopeForModel(prompt)) wrongRefusals.add(prompt);
        }
        System.out.println("OFFSCOPE pass corpus=" + prompts.size() + " wrongly_refused=" + wrongRefusals.size());
        if (!wrongRefusals.isEmpty()) {
            fail("In-scope prompts wrongly refused (" + wrongRefusals.size() + "/" + prompts.size() + "): " + wrongRefusals);
        }
    }

    @Test
    public void corporaAreLargeEnough() throws IOException {
        assertTrue(load("offline_scope/must_refuse.txt").size() >= 150);
        assertTrue(load("offline_scope/must_pass.txt").size() >= 100);
    }

    private static List<String> load(String resource) throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream in = OfflineScopeHarnessTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Missing test resource: " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) lines.add(trimmed);
                }
            }
        }
        return lines;
    }
}
