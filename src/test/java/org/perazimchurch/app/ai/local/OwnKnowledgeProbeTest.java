package org.perazimchurch.app.ai.local;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.ai.library.KjvVerseText;
import org.perazimchurch.app.ai.library.ScriptureRangeTable;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Probe for own-knowledge replies (round 49). Runs the model on each question in PERAZIM_PROBE_CASES (jsonl with id, q),
 * writes the raw reply and whether it is accepted to PERAZIM_PROBE_OUT. Skipped unless the env vars are set.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OwnKnowledgeProbeTest {

    @Test
    public void probeOwnKnowledgeReplies() throws Exception {
        String casesPath = System.getenv("PERAZIM_PROBE_CASES");
        String modelPath = System.getenv("PERAZIM_EVAL_MODEL");
        String llama = System.getenv("PERAZIM_EVAL_LLAMA");
        String outPath = System.getenv("PERAZIM_PROBE_OUT");
        assumeTrue(casesPath != null && modelPath != null && llama != null && outPath != null);
        Context c = ApplicationProvider.getApplicationContext();
        KjvVerseText kv = KjvVerseText.load(c);
        ScriptureRangeTable table = OwnKnowledge.loadTable(c);
        final String llamaFinal = llama;
        final String modelFinal = modelPath;
        OnDeviceAi.testRunner = (p, g, n) -> {
            try {
                return EvalPipelineTest.runLlama(llamaFinal, modelFinal, p, n);
            } catch (Exception e) {
                throw new java.io.IOException(e);
            }
        };
        try (BufferedReader in = new BufferedReader(new FileReader(casesPath));
             PrintWriter out = new PrintWriter(new FileWriter(outPath))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                JSONObject o = new JSONObject(line);
                String id = o.optString("id");
                String q = o.optString("q");
                String raw = OnDeviceAi.testRunner.generate(OwnKnowledge.prompt(q), null, 160);
                String accepted = OwnKnowledge.accept(raw, kv, table);
                JSONObject r = new JSONObject();
                r.put("id", id);
                r.put("q", q);
                r.put("raw", raw == null ? "" : raw);
                r.put("accepted", accepted != null);
                out.println(r.toString());
                out.flush();
            }
        } finally {
            OnDeviceAi.testRunner = null;
        }
    }
}
