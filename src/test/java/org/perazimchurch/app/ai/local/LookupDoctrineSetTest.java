package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The dedicated set for biblical doctrines, verse finding, word searches and follow-ups (Levi, 2026-11-25). Each case is routed
 * through GuideTurnRouter.route, the same path the guide dialog uses before any model call. Writes a per-category report to
 * build/reports/lookup-doctrine.json and fails when any case misses its expected route or its required wording.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LookupDoctrineSetTest {

    @Test
    public void lookupAndDoctrineSet() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        List<String> lines = Files.readAllLines(new File("tools/ai-eval/lookup-doctrine/cases.jsonl").toPath(), StandardCharsets.UTF_8);
        TreeMap<String, int[]> perCategory = new TreeMap<>();
        StringBuilder failures = new StringBuilder();
        JSONArray report = new JSONArray();
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            JSONObject kase = new JSONObject(line);
            SoftSafety.reset();
            GuideTurnRouter.Turn turn = GuideTurnRouter.route(c, new PerazimAiEngine(c), kase.getString("question"));
            String text = turn.response == null ? "" : turn.response.getMessageText();
            String status = turn.status == null ? "" : turn.status;
            boolean ok = status.startsWith(kase.getString("expect"));
            org.json.JSONArray must = kase.getJSONArray("must");
            for (int i = 0; i < must.length(); i++) {
                if (!text.toLowerCase(Locale.ROOT).contains(must.getString(i).toLowerCase(Locale.ROOT))) ok = false;
            }
            int[] tally = perCategory.computeIfAbsent(kase.getString("category"), k -> new int[2]);
            tally[1]++;
            if (ok) tally[0]++;
            else failures.append(kase.getString("id")).append(' ').append(kase.getString("question")).append(" -> status=")
                    .append(status).append(" expected=").append(kase.getString("expect")).append('\n');
            JSONObject row = new JSONObject();
            row.put("id", kase.getString("id"));
            row.put("category", kase.getString("category"));
            row.put("question", kase.getString("question"));
            row.put("status", status);
            row.put("pass", ok);
            row.put("answer", text.length() > 240 ? text.substring(0, 240) : text);
            report.put(row);
        }
        StringBuilder summary = new StringBuilder();
        int total = 0;
        int passed = 0;
        for (java.util.Map.Entry<String, int[]> e : perCategory.entrySet()) {
            summary.append(String.format(Locale.ROOT, "%-14s %d/%d%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
            total += e.getValue()[1];
            passed += e.getValue()[0];
        }
        summary.append(String.format(Locale.ROOT, "%-14s %d/%d%n", "TOTAL", passed, total));
        File out = new File("build/reports/lookup-doctrine.json");
        out.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(out, StandardCharsets.UTF_8)) {
            w.write(report.toString(2));
        }
        Files.write(new File("build/reports/lookup-doctrine-summary.txt").toPath(), summary.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(summary + "\nFailures:\n" + failures, failures.length() == 0);
    }
}
